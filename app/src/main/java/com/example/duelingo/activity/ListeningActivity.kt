package com.example.duelingo.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.duelingo.R
import com.example.duelingo.databinding.ActivityListeningBinding
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
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: String? = null
    private var currentQuestionId: String? = null
    private var currentQuestionText: String? = null
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListeningBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initDependencies()
        setupViews()
        setupSpeechRecognizer()
        loadAudioQuestion()
    }

    private fun initDependencies() {
        tokenManager = TokenManager(this)
        textToSpeech = TextToSpeech(this, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
    }

    private fun setupViews() {
        binding.playAudioIcon.setOnClickListener { playQuestionAudio() }
        binding.recordAudioIcon.setOnClickListener { toggleRecording() }
        binding.submitButton.setOnClickListener { submitRecording() }
    }

    private fun setupSpeechRecognizer() {
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                    binding.recognizedText.text = it
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                    binding.recognizedText.text = it
                }
            }

            override fun onError(error: Int) {
                showToast("Recognition error: ${getErrorText(error)}")
            }

            // Other empty overrides...
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error"
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.US
            textToSpeech.setSpeechRate(1.0f)
        } else {
            showToast("TextToSpeech initialization failed")
        }
    }

    private fun loadAudioQuestion() {
        val accessToken = tokenManager.getAccessToken() ?: run {
            showToast("Authorization error")
            return
        }

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
                        binding.playAudioIcon.isEnabled = true
                    } else {
                        showToast("No questions available")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Failed to load question: ${e.message}")
                }
            }
        }
    }

    private fun playQuestionAudio() {
        currentQuestionText?.let {
            textToSpeech.speak(it, TextToSpeech.QUEUE_FLUSH, null, null)
        } ?: showToast("Question text not available")
    }

    private fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO
            )
            return
        }

        outputFile = "${externalCacheDir?.absolutePath}/recording_${System.currentTimeMillis()}.mp4"
        mediaRecorder = try {
            MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile)
                prepare()
                start()
            }
        } catch (e: Exception) {
            showToast("Recording setup failed: ${e.message}")
            null
        }

        speechRecognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH)
            }
        )

        isRecording = true
        binding.recordAudioIcon.setImageResource(R.drawable.record_square)
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            speechRecognizer.stopListening()
        } catch (e: Exception) {
            showToast("Recording stop failed: ${e.message}")
        } finally {
            mediaRecorder = null
            isRecording = false
            binding.recordAudioIcon.setImageResource(R.drawable.microphone)
        }
    }

    private fun submitRecording() {
        val accessToken = tokenManager.getAccessToken() ?: run {
            showToast("Authorization error")
            return
        }

        val questionId = currentQuestionId ?: run {
            showToast("Question not loaded")
            return
        }

        val audioFile = outputFile?.let { File(it) } ?: run {
            showToast("No recording available")
            return
        }

        if (!audioFile.exists() || audioFile.length() == 0L) {
            showToast("Recording file is empty")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val requestFile = audioFile.asRequestBody("audio/mp4".toMediaTypeOrNull())
                val audioPart = MultipartBody.Part.createFormData("audioFile", audioFile.name, requestFile)

                val response = ApiClient.questionService.verifyAnswer(
                    token = "Bearer $accessToken",
                    questionId = questionId,
                    audioFile = audioPart
                )

                withContext(Dispatchers.Main) {
                    val message = if (response.isCorrect) {
                        "✅ Correct! ${response.feedback}"
                    } else {
                        "❌ Incorrect. ${response.feedback}"
                    }
                    showToast(message)
                    binding.recognizedText.text = response.recognizedText
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Submission failed: ${e.message}")
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.shutdown()
        speechRecognizer.destroy()
        mediaRecorder?.release()
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO = 100
    }
}