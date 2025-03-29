package com.example.duelingo.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.duelingo.R
import com.example.duelingo.databinding.ActivityListeningBinding
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ListeningActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityListeningBinding
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private var currentSpeed = 100
    private var currentQuestionId: String? = null
    private var currentQuestionText: String? = null
    private lateinit var tokenManager: TokenManager
    private var isRecording = false

    private val speechListener = object : android.speech.RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            binding.recognizedText.text = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Error: $error"
            }
        }

        override fun onResults(results: Bundle) {
            results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let {
                if (it.isNotEmpty()) {
                    binding.recognizedText.text = it[0]
                    submitAnswer(it[0])
                }
            }
        }

        override fun onPartialResults(partialResults: Bundle) {
            partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let {
                if (it.isNotEmpty()) {
                    binding.recognizedText.text = it[0]
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceRecognition()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListeningBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tokenManager = TokenManager(this)

        initSpeechComponents()
        setupSpeedControl()
        setupClickListeners()
        loadAudioQuestion()
    }

    private fun initSpeechComponents() {
        textToSpeech = TextToSpeech(this, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(speechListener)
        }
    }

    private fun setupSpeedControl() {
        binding.speedSeekBar.apply {
            max = 200
            progress = currentSpeed
            binding.speedLabel.text = "Speed: $currentSpeed%"

            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    currentSpeed = progress
                    binding.speedLabel.text = "Speed: $progress%"
                    textToSpeech.setSpeechRate(progress / 100f)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
        }
    }

    private fun setupClickListeners() {
        binding.recordAudioIcon.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED -> toggleRecording()
                else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        binding.playAudioIcon.setOnClickListener {
            currentQuestionText?.let { text ->
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            } ?: Toast.makeText(this, "No question loaded", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleRecording() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            if (isRecording) {
                stopVoiceRecognition()
                binding.recordAudioIcon.setImageResource(R.drawable.microphone)
            } else {
                startVoiceRecognition()
                binding.recordAudioIcon.setImageResource(R.drawable.record_square)
            }
            isRecording = !isRecording
        } else {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startVoiceRecognition() {
        try {
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                speechRecognizer.startListening(this)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error starting recognition", Toast.LENGTH_SHORT).show()
            resetRecordingState()
        }
    }
    private fun stopVoiceRecognition() {
        try {
            speechRecognizer.stopListening()
        } catch (e: Exception) {
            Toast.makeText(this, "Error stopping recognition", Toast.LENGTH_SHORT).show()
        }
    }
    private fun resetRecordingState() {
        isRecording = false
        binding.recordAudioIcon.setImageResource(R.drawable.microphone)
    }
    private fun loadAudioQuestion() {
        val accessToken = tokenManager.getAccessToken()
        if (accessToken != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val questions = ApiClient.questionService.getRandomAudioQuestions(
                        token = "Bearer $accessToken",
                        size = 1
                    )
                    withContext(Dispatchers.Main) {
                        if (questions.isNotEmpty()) {
                            val question = questions[0]
                            currentQuestionId = question.id
                            currentQuestionText = question.questionText

                            if (currentQuestionText != null) {
                                binding.playAudioIcon.isEnabled = true
                            } else {
                                showToast("Текст вопроса отсутствует.")
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showToast("Ошибка при загрузке вопроса.")
                    }
                }
            }
        } else {
            showToast("Ошибка авторизации.")
        }
    }

    private fun submitAnswer(userAnswer: String) {
        if (currentQuestionText == null) {
            showToast("Question not loaded")
            return
        }

        val cleanedQuestion = currentQuestionText!!.replace("[^a-zA-Z ]".toRegex(), "").lowercase()
        val cleanedAnswer = userAnswer.replace("[^a-zA-Z ]".toRegex(), "").lowercase()

        val similarity = calculateEnhancedSimilarity(cleanedQuestion, cleanedAnswer)
        val isCorrect = similarity >= 0.7
        val feedback = generateFeedback(similarity)

        showToast(
            if (isCorrect) "✅ Correct! $feedback"
            else "❌ Incorrect. $feedback"
        )
    }

    private fun calculateEnhancedSimilarity(text1: String, text2: String): Double {
        val words1 = text1.split(" ").filter { it.isNotBlank() }
        val words2 = text2.split(" ").filter { it.isNotBlank() }

        if (words1.isEmpty() || words2.isEmpty()) return 0.0

        val similarityScore = words2.sumOf { userWord ->
            words1.maxOfOrNull { questionWord ->
                calculateWordSimilarity(questionWord, userWord)
            } ?: 0.0
        } / words2.size

        return similarityScore.coerceIn(0.0, 1.0)
    }

    private fun calculateWordSimilarity(word1: String, word2: String): Double {
        if (word1 == word2) return 1.0
        if (word1.contains(word2)) return 0.8
        if (word2.contains(word1)) return 0.8

        // Простая проверка на опечатки (можно улучшить)
        val minLength = minOf(word1.length, word2.length)
        var matchingChars = 0
        for (i in 0 until minLength) {
            if (word1[i] == word2[i]) matchingChars++
        }

        return matchingChars.toDouble() / maxOf(word1.length, word2.length)
    }

    private fun generateFeedback(similarity: Double): String {
        return when {
            similarity >= 0.9 -> "Perfect match!"
            similarity >= 0.7 -> "Close enough! Minor mistakes"
            similarity >= 0.5 -> "Some correct parts"
            else -> "Needs more practice"
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.US
            textToSpeech.setSpeechRate(currentSpeed / 100f)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}