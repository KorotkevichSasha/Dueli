package com.example.duelingo.activity

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
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
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var outputFile: String? = null
    private var currentQuestionId: String? = null
    private var currentQuestionText: String? = null
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speedSeekBar: SeekBar
    private lateinit var speedLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListeningBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        textToSpeech = TextToSpeech(this, this)

        speedSeekBar = binding.speedSeekBar
        speedLabel = binding.speedLabel

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

        binding.playAudioIcon.setOnClickListener {
            playAudio()
        }

        binding.recordAudioIcon.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        binding.submitButton.setOnClickListener {
            submitRecording()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                showToast("Язык не поддерживается")
            } else {
                textToSpeech.setSpeechRate(1.0f)
            }
        } else {
            showToast("Ошибка инициализации TTS")
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

                            println("Question Text: $currentQuestionText")

                            if (currentQuestionText != null) {
                                binding.playAudioIcon.isEnabled = true
                            } else {
                                showToast("Текст вопроса отсутствует")
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        showToast("Ошибка при загрузке вопроса")
                    }
                }
            }
        } else {
            showToast("Ошибка авторизации")
        }
    }

    private fun playAudio() {
        val questionText = currentQuestionText
        if (questionText != null) {
            textToSpeech.speak(questionText, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            showToast("Текст вопроса отсутствует")
        }
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
            return
        }

        isRecording = true
        binding.recordAudioIcon.setImageResource(R.drawable.record_square)

        outputFile = "${externalCacheDir?.absolutePath}/recording.3gp"
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outputFile)
            prepare()
            start()
        }
    }

    private fun stopRecording() {
        isRecording = false
        binding.recordAudioIcon.setImageResource(R.drawable.microphone)

        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
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
                            showToast("Правильно! ${response.recognizedText}")
                        } else {
                            showToast("Неправильно. ${response.feedback}")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        showToast("О: ${e.message}")
                        println("Ошибка: ${e.stackTraceToString()}")
                    }
                }
            }
        } else {
            showToast("Ошибка авторизации или записи")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaRecorder?.release()

        // Остановка TextToSpeech
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO = 100
    }
}