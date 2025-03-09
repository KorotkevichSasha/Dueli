package com.example.duelingo.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.duelingo.R
import com.example.duelingo.databinding.ActivityListeningBinding
import com.example.duelingo.dto.response.AudioAnswerResponse
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.Locale

class ListeningActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityListeningBinding
    private lateinit var tokenManager: TokenManager
    private var mediaPlayer: MediaPlayer? = null
    private var outputFile: String? = null
    private var currentQuestionId: String? = null
    private var currentQuestionText: String? = null
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speedSeekBar: SeekBar
    private lateinit var speedLabel: TextView
    private lateinit var recognizedTextView: TextView
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private var isRecording = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListeningBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recognizedTextView = binding.recognizedText
        speedSeekBar = binding.speedSeekBar
        speedLabel = binding.speedLabel

        tokenManager = TokenManager(this)
        textToSpeech = TextToSpeech(this, this)
        speedLabel.text = "Скорость: 100%"
        speedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val speed = progress / 100f
                textToSpeech.setSpeechRate(speed)
                speedLabel.text = "Скорость: ${progress}%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        loadAudioQuestion()

        binding.playAudioIcon.setOnClickListener { playAudio() }
        binding.recordAudioIcon.setOnClickListener {
            if (isRecording) stopRecording() else startRecording()
        }
        binding.submitButton.setOnClickListener { submitRecording() }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                when (error) {
                    SpeechRecognizer.ERROR_NETWORK -> showToast("Ошибка сети. Проверьте интернет.")
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> showToast("Распознаватель занят. Попробуйте снова.")
                    else -> showToast("Ошибка распознавания: $error")
                }
                if (isRecording) speechRecognizer.startListening(recognizerIntent)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0]
                    if (isEnglish(recognizedText)) {
                        binding.recognizedText.text = "Распознано: $recognizedText"
                    } else {
                        showToast("Говорите только на английском.")
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0]
                    if (isEnglish(recognizedText)) {
                        binding.recognizedText.text = recognizedText
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun isEnglish(text: String): Boolean {
        return text.matches(Regex("^[a-zA-Z0-9\\s.,!?'-]+\$"))
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                showToast("Английский язык не поддерживается.")
            } else {
                textToSpeech.setSpeechRate(1.0f)
            }
        }
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

    private fun playAudio() {
        val questionText = currentQuestionText
        if (questionText != null) {
            textToSpeech.speak(questionText, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            showToast("Текст вопроса отсутствует.")
        }
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
            return
        }

        isRecording = true
        binding.recordAudioIcon.setImageResource(R.drawable.record_square)
        speechRecognizer.startListening(recognizerIntent)
    }

    private fun stopRecording() {
        isRecording = false
        binding.recordAudioIcon.setImageResource(R.drawable.microphone)
        speechRecognizer.stopListening()
    }

    private fun submitRecording() {
        val accessToken = tokenManager.getAccessToken()
        val audioFile = outputFile
        val questionId = currentQuestionId

        if (accessToken != null && audioFile != null && questionId != null) {
            val file = File(audioFile)
            val requestFile = file.asRequestBody("audio/3gpp".toMediaTypeOrNull())
            val audioPart = MultipartBody.Part.createFormData("audioFile", file.name, requestFile)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = ApiClient.questionService.verifyAnswer(
                        token = "Bearer $accessToken",
                        questionId = questionId,
                        audioFile = audioPart
                    )
                    withContext(Dispatchers.Main) {
                        if (response.isCorrect) {
                            showToast("Правильно! Распознанный текст: ${response.recognizedText}")
                            recognizedTextView.text = "Распознанный текст: ${response.recognizedText}"
                        } else {
                            showToast("Неправильно. Ошибка: ${response.feedback}")
                            recognizedTextView.text = "Ошибка: ${response.feedback}"
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showToast("Ошибка: ${e.message}")
                        recognizedTextView.text = "Ошибка: ${e.message}"
                    }
                }
            }
        } else {
            showToast("Ошибка авторизации или записи.")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO = 100
    }
}