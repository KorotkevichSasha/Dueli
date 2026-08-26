package com.example.duelingo.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.duelingo.R
import com.example.duelingo.databinding.ActivityListeningBinding
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import com.example.duelingo.storage.LearningHabitTracker
import com.example.duelingo.utils.SpeechTextNormalizer
import kotlinx.coroutines.launch
import java.util.Locale

class ListeningActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    companion object {
        private const val MIN_SPEECH_RATE = 50
        private const val DEFAULT_SPEECH_RATE = 100
        private const val UTTERANCE_ID = "duelrush-listening-question"
        private const val STATE_QUESTION = "listening_question"
        private const val STATE_RECOGNIZED = "listening_recognized"
        private const val STATE_FEEDBACK = "listening_feedback"
        private const val STATE_SPEED = "listening_speed"
        private const val STATE_CHECKED = "listening_checked"
    }

    private lateinit var binding: ActivityListeningBinding
    private lateinit var tokenManager: TokenManager
    private lateinit var textToSpeech: TextToSpeech
    private var speechRecognizer: SpeechRecognizer? = null

    private var currentSpeed = DEFAULT_SPEECH_RATE
    private var currentQuestionText: String? = null
    private var lastRecognizedText: String? = null
    private var ttsReady = false
    private var isPlaying = false
    private var recognitionInProgress = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceRecognition()
        } else {
            showToast(getString(R.string.listening_microphone_permission))
        }
    }

    private val speechListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            binding.recordingStatus.setText(R.string.listening_speak_now)
        }

        override fun onBeginningOfSpeech() {
            binding.recordingStatus.setText(R.string.listening_recording)
        }

        override fun onRmsChanged(rmsdB: Float) {
            val scale = (1f + ((rmsdB.coerceIn(0f, 10f) / 10f) * 0.12f))
            binding.recordAudioIcon.scaleX = scale
            binding.recordAudioIcon.scaleY = scale
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            binding.recordAudioIcon.setImageResource(R.drawable.microphone)
            binding.recordAudioIcon.isEnabled = false
            binding.recordingStatus.setText(R.string.listening_processing)
        }

        override fun onError(error: Int) {
            resetRecognitionState()
            binding.recognizedText.text = lastRecognizedText
                ?: getString(R.string.listening_nothing_recorded)
            binding.recordingStatus.text = getString(speechErrorMessage(error))
        }

        override fun onResults(results: Bundle?) {
            val recognized = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()

            resetRecognitionState()
            if (recognized.isNullOrBlank()) {
                binding.recognizedText.setText(R.string.listening_nothing_recorded)
                binding.recordingStatus.setText(R.string.listening_no_speech)
                return
            }

            lastRecognizedText = recognized
            binding.recognizedText.text = recognized
            binding.recordingStatus.setText(R.string.listening_recorded)
            binding.submitButton.isEnabled = true
        }

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let { binding.recognizedText.text = it }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListeningBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tokenManager = TokenManager(this)

        initializeSpeechComponents()
        setupSpeedControl()
        setupClickListeners()
        resetQuestionUi()
        if (!restoreListeningState(savedInstanceState)) {
            loadAudioQuestion()
        }
    }

    private fun restoreListeningState(state: Bundle?): Boolean {
        val question = state?.getString(STATE_QUESTION)?.takeIf { it.isNotBlank() } ?: return false
        currentQuestionText = question
        currentSpeed = state.getInt(STATE_SPEED, DEFAULT_SPEECH_RATE)
            .coerceIn(MIN_SPEECH_RATE, MIN_SPEECH_RATE + binding.speedSeekBar.max)
        binding.speedSeekBar.progress = currentSpeed - MIN_SPEECH_RATE
        binding.speedLabel.text = getString(R.string.speed_format, currentSpeed)
        binding.playbackStatus.setText(R.string.listening_tap_to_play)
        binding.questionLoading.visibility = View.GONE
        binding.playAudioIcon.visibility = View.VISIBLE

        lastRecognizedText = state.getString(STATE_RECOGNIZED)?.takeIf { it.isNotBlank() }
        binding.recognizedText.text = lastRecognizedText
            ?: getString(R.string.listening_nothing_recorded)
        binding.recordingStatus.setText(
            if (lastRecognizedText == null) R.string.listening_tap_microphone
            else R.string.listening_recorded
        )
        binding.submitButton.isEnabled = lastRecognizedText != null
        binding.feedbackText.text = state.getString(STATE_FEEDBACK).orEmpty()
        binding.nextListeningButton.visibility =
            if (state.getBoolean(STATE_CHECKED, false)) View.VISIBLE else View.GONE
        val checked = state.getBoolean(STATE_CHECKED, false)
        binding.submittedAnswerText.text = lastRecognizedText.orEmpty()
        binding.correctAnswerText.text = question
        binding.listeningResultCard.visibility = if (checked) View.VISIBLE else View.GONE
        if (checked) binding.submitButton.isEnabled = false
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_QUESTION, currentQuestionText)
        outState.putString(STATE_RECOGNIZED, lastRecognizedText)
        outState.putString(STATE_FEEDBACK, binding.feedbackText.text.toString())
        outState.putInt(STATE_SPEED, currentSpeed)
        outState.putBoolean(
            STATE_CHECKED,
            binding.nextListeningButton.visibility == View.VISIBLE
        )
    }

    private fun initializeSpeechComponents() {
        textToSpeech = TextToSpeech(this, this)
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                runOnUiThread { updatePlaybackState(true) }
            }

            override fun onDone(utteranceId: String?) {
                runOnUiThread { updatePlaybackState(false) }
            }

            override fun onError(utteranceId: String?) {
                runOnUiThread {
                    updatePlaybackState(false)
                    showToast(getString(R.string.listening_playback_error))
                }
            }
        })

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).also {
                it.setRecognitionListener(speechListener)
            }
        } else {
            binding.recordAudioIcon.isEnabled = false
            binding.recordingStatus.setText(R.string.listening_recognition_unavailable)
        }
    }

    private fun setupSpeedControl() {
        binding.speedSeekBar.max = 100
        binding.speedSeekBar.progress = DEFAULT_SPEECH_RATE - MIN_SPEECH_RATE
        binding.speedLabel.text = getString(R.string.speed_format, currentSpeed)
        binding.speedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentSpeed = MIN_SPEECH_RATE + progress
                binding.speedLabel.text = getString(R.string.speed_format, currentSpeed)
                if (ttsReady) textToSpeech.setSpeechRate(currentSpeed / 100f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            stopAllAudio()
            finish()
        }
        binding.playAudioIcon.setOnClickListener { toggleSentencePlayback() }
        binding.recordAudioIcon.setOnClickListener { requestOrToggleRecognition() }
        binding.submitButton.setOnClickListener {
            lastRecognizedText?.let(::checkAnswer)
                ?: showToast(getString(R.string.listening_record_first))
        }
        binding.nextListeningButton.setOnClickListener {
            LearningHabitTracker(this).recordPractice(3)
            loadAudioQuestion()
        }
        binding.replayCorrectAnswerButton.setOnClickListener { playCurrentSentence() }
    }

    private fun toggleSentencePlayback() {
        if (isPlaying) {
            textToSpeech.stop()
            updatePlaybackState(false)
            return
        }
        if (recognitionInProgress) {
            showToast(getString(R.string.listening_stop_recording_first))
            return
        }

        playCurrentSentence()
    }

    private fun playCurrentSentence() {
        val sentence = currentQuestionText
        if (sentence.isNullOrBlank()) {
            showToast(getString(R.string.listening_question_not_loaded))
            return
        }
        if (!ttsReady) {
            showToast(getString(R.string.listening_voice_unavailable))
            return
        }

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f)
        }
        val result = textToSpeech.speak(sentence, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID)
        if (result == TextToSpeech.ERROR) {
            updatePlaybackState(false)
            showToast(getString(R.string.listening_playback_error))
        } else {
            updatePlaybackState(true)
        }
    }

    private fun requestOrToggleRecognition() {
        if (recognitionInProgress) {
            speechRecognizer?.stopListening()
            binding.recordAudioIcon.isEnabled = false
            binding.recordingStatus.setText(R.string.listening_processing)
            return
        }
        if (isPlaying) {
            textToSpeech.stop()
            updatePlaybackState(false)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceRecognition()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceRecognition() {
        val recognizer = speechRecognizer ?: run {
            showToast(getString(R.string.listening_recognition_unavailable))
            return
        }

        lastRecognizedText = null
        binding.recognizedText.setText(R.string.listening_waiting_for_voice)
        binding.feedbackText.text = ""
        binding.listeningResultCard.visibility = View.GONE
        binding.nextListeningButton.visibility = View.GONE
        binding.submitButton.isEnabled = false

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.listening_speak_now))
        }

        try {
            recognizer.cancel()
            recognitionInProgress = true
            binding.recordAudioIcon.setImageResource(R.drawable.record_square)
            binding.recordingStatus.setText(R.string.listening_preparing_microphone)
            updateControls()
            recognizer.startListening(intent)
        } catch (_: Exception) {
            resetRecognitionState()
            binding.recognizedText.setText(R.string.listening_nothing_recorded)
            showToast(getString(R.string.listening_recognition_start_error))
        }
    }

    private fun resetRecognitionState() {
        recognitionInProgress = false
        binding.recordAudioIcon.scaleX = 1f
        binding.recordAudioIcon.scaleY = 1f
        binding.recordAudioIcon.setImageResource(R.drawable.microphone)
        updateControls()
    }

    private fun updatePlaybackState(playing: Boolean) {
        isPlaying = playing
        binding.playAudioIcon.setImageResource(
            if (playing) R.drawable.record_square else R.drawable.ic_play_clean
        )
        binding.playbackStatus.setText(
            if (playing) R.string.listening_playing else R.string.listening_tap_to_play
        )
        updateControls()
    }

    private fun updateControls() {
        binding.playAudioIcon.isEnabled = ttsReady && currentQuestionText != null && !recognitionInProgress
        binding.playAudioIcon.alpha = if (binding.playAudioIcon.isEnabled) 1f else 0.45f
        binding.recordAudioIcon.isEnabled = speechRecognizer != null && !isPlaying && currentQuestionText != null
        binding.recordAudioIcon.alpha = if (binding.recordAudioIcon.isEnabled) 1f else 0.45f
    }

    private fun loadAudioQuestion() {
        val accessToken = tokenManager.getAccessToken() ?: run {
            showToast(getString(R.string.listening_authorization_error))
            return
        }

        stopAllAudio()
        resetQuestionUi()
        lifecycleScope.launch {
            runCatching {
                ApiClient.questionService.getRandomAudioQuestions(
                    token = "Bearer $accessToken",
                    size = 1
                ).firstOrNull()
            }.onSuccess { question ->
                if (question == null || question.questionText.isBlank()) {
                    binding.playbackStatus.setText(R.string.listening_no_questions)
                } else {
                    currentQuestionText = question.questionText
                    binding.playbackStatus.setText(R.string.listening_tap_to_play)
                }
                binding.questionLoading.visibility = View.GONE
                binding.playAudioIcon.visibility = View.VISIBLE
                updateControls()
            }.onFailure {
                binding.questionLoading.visibility = View.GONE
                binding.playAudioIcon.visibility = View.VISIBLE
                binding.playbackStatus.setText(R.string.listening_loading_error)
                showToast(getString(R.string.listening_loading_error))
                updateControls()
            }
        }
    }

    private fun resetQuestionUi() {
        currentQuestionText = null
        lastRecognizedText = null
        binding.playbackStatus.setText(R.string.listening_loading)
        binding.questionLoading.visibility = View.VISIBLE
        binding.playAudioIcon.visibility = View.INVISIBLE
        binding.recordingStatus.setText(R.string.listening_tap_microphone)
        binding.recognizedText.setText(R.string.listening_nothing_recorded)
        binding.feedbackText.text = ""
        binding.listeningResultCard.visibility = View.GONE
        binding.submitButton.isEnabled = false
        binding.nextListeningButton.visibility = View.GONE
        updateControls()
    }

    private fun checkAnswer(userAnswer: String) {
        val expected = currentQuestionText ?: return
        binding.submitButton.isEnabled = false
        val similarity = sentenceSimilarity(expected, userAnswer)
        val percent = (similarity * 100).toInt()
        // Speech recognition often drops articles or short endings. Sixty
        // percent still requires the phrase to be recognizably the same while
        // avoiding false failures caused by the recognition engine.
        binding.feedbackText.text = if (similarity >= 0.60) {
            getString(R.string.listening_answer_correct, percent)
        } else {
            getString(R.string.listening_answer_try_again, percent)
        }
        binding.submittedAnswerText.text = userAnswer
        binding.correctAnswerText.text = expected
        binding.listeningResultCard.visibility = View.VISIBLE
        binding.nextListeningButton.visibility = View.VISIBLE
        claimListeningReward(percent)
        binding.listeningContent.post {
            binding.listeningContent.smoothScrollTo(0, binding.listeningResultCard.bottom)
        }
    }

    private fun claimListeningReward(percent: Int) {
        val token = tokenManager.getAccessToken() ?: return
        lifecycleScope.launch {
            runCatching {
                ApiClient.userService.claimListeningReward("Bearer $token", percent)
            }.onSuccess { reward ->
                if (reward.goldAwarded > 0) {
                    binding.feedbackText.append(
                        getString(R.string.listening_gold_reward, reward.goldAwarded)
                    )
                }
            }
        }
    }

    private fun sentenceSimilarity(expected: String, actual: String): Double {
        val normalizedExpected = normalize(expected)
        val normalizedActual = normalize(actual)
        if (normalizedExpected.isBlank() || normalizedActual.isBlank()) return 0.0
        if (normalizedExpected == normalizedActual) return 1.0

        val expectedWords = normalizedExpected.split(' ').toSet()
        val actualWords = normalizedActual.split(' ').toSet()
        val union = expectedWords union actualWords
        val wordScore = if (union.isEmpty()) 0.0 else (expectedWords intersect actualWords).size.toDouble() / union.size

        val maxLength = maxOf(normalizedExpected.length, normalizedActual.length)
        val editScore = 1.0 - levenshtein(normalizedExpected, normalizedActual).toDouble() / maxLength
        return (wordScore * 0.45 + editScore * 0.55).coerceIn(0.0, 1.0)
    }

    private fun normalize(text: String): String = SpeechTextNormalizer.normalize(text)

    private fun levenshtein(first: String, second: String): Int {
        var previous = IntArray(second.length + 1) { it }
        for (i in first.indices) {
            val current = IntArray(second.length + 1)
            current[0] = i + 1
            for (j in second.indices) {
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + if (first[i] == second[j]) 0 else 1
                )
            }
            previous = current
        }
        return previous[second.length]
    }

    private fun speechErrorMessage(error: Int): Int = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> R.string.listening_no_speech
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> R.string.listening_speech_timeout
        SpeechRecognizer.ERROR_AUDIO -> R.string.listening_audio_error
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_SERVER -> R.string.listening_network_error
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> R.string.listening_recognizer_busy
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> R.string.listening_microphone_permission
        else -> R.string.listening_recognition_error
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            binding.playbackStatus.setText(R.string.listening_voice_unavailable)
            updateControls()
            return
        }

        textToSpeech.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )

        val languageResult = listOf(Locale.US, Locale.UK, Locale.ENGLISH)
            .firstNotNullOfOrNull { locale ->
                textToSpeech.setLanguage(locale).takeIf {
                    it != TextToSpeech.LANG_MISSING_DATA && it != TextToSpeech.LANG_NOT_SUPPORTED
                }
            }

        ttsReady = languageResult != null
        if (ttsReady) {
            textToSpeech.setSpeechRate(currentSpeed / 100f)
        } else {
            binding.playbackStatus.setText(R.string.listening_voice_unavailable)
        }
        updateControls()
    }

    private fun stopAllAudio() {
        if (::textToSpeech.isInitialized) textToSpeech.stop()
        speechRecognizer?.cancel()
        isPlaying = false
        recognitionInProgress = false
        if (::binding.isInitialized) {
            binding.playAudioIcon.setImageResource(R.drawable.ic_play_clean)
            binding.recordAudioIcon.setImageResource(R.drawable.microphone)
            binding.recordAudioIcon.scaleX = 1f
            binding.recordAudioIcon.scaleY = 1f
            binding.playbackStatus.setText(
                if (currentQuestionText == null) R.string.listening_loading
                else R.string.listening_tap_to_play
            )
            binding.recordingStatus.setText(R.string.listening_tap_microphone)
            updateControls()
        }
    }

    override fun onStop() {
        stopAllAudio()
        super.onStop()
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
