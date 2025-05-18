package com.example.duelingo.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.example.duelingo.R
import com.example.duelingo.dto.response.QuestionDetailedResponse
import com.example.duelingo.databinding.ActivityDuelBinding
import com.example.duelingo.dto.event.DuelFoundEvent
import com.example.duelingo.dto.event.MatchmakingFailedEvent
import com.example.duelingo.dto.request.DuelFinishRequest
import com.example.duelingo.fragment.QuestionFragment
import com.example.duelingo.network.ApiClient
import com.example.duelingo.network.websocket.StompManager
import com.example.duelingo.storage.TokenManager
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class DuelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDuelBinding
    private lateinit var timer: CountDownTimer
    private var timeLeftMillis: Long = 2 * 60 * 1000
    private var currentQuestion = 0
    private val totalQuestions: Int get() = duelQuestions.size
    private lateinit var duelQuestions: List<QuestionDetailedResponse>
    private lateinit var duelId: String
    private lateinit var stompManager: StompManager
    private lateinit var tokenManager: TokenManager
    private var isQuestionLoaded = false
    private var userId: String? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDuelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)
        if (!tokenManager.isLoggedIn()) {
            finish()
            return
        }

        // Initialize StompManager
        stompManager = StompManager(tokenManager)

        // Check duel data
        val duelInfoJson = intent.getStringExtra("DUEL_INFO") ?: run {
            Toast.makeText(this, "Duel data corrupted", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val duelInfo = Gson().fromJson(duelInfoJson, DuelFoundEvent::class.java)
        initializeDuel(duelInfo)
        setupTimer()
        setupNextButton()
        loadFirstQuestion()
    }

    private fun initializeDuel(duelInfo: DuelFoundEvent) {
        if (isQuestionLoaded) {
            return // Prevent re-initialization if questions are already loaded
        }

        duelQuestions = duelInfo.duel.questions
        duelId = duelInfo.duel.id

        val opponent = if (duelInfo.opponentId.equals(duelInfo.duel.player1.userId)) {
            duelInfo.duel.player1
        } else {
            duelInfo.duel.player2
        }

        binding.opponentName.text = "Opponent: ${opponent.username}"
    }

    private fun loadFirstQuestion() {
        if (!isQuestionLoaded && duelQuestions.isNotEmpty()) {
            isQuestionLoaded = true
            currentQuestion = 0
            loadNextQuestion()
        }
    }

    override fun onStart() {
        super.onStart()
        if (!isQuestionLoaded) {
            connectToWebSocket()
        }
    }

    override fun onStop() {
        super.onStop()
        stompManager.disconnect()
    }

    private fun connectToWebSocket() {
        stompManager.connect(
            onConnected = ::handleWebSocketConnected,
            onError = ::handleWebSocketError,
            onDuelFound = ::handleDuelFound,
            onMatchmakingFailed = ::handleMatchmakingFailed
        )
    }

    private fun handleWebSocketConnected() {
        Log.d("DuelActivity", "WebSocket connected")
        if (!isQuestionLoaded) {
            joinMatchmaking()
        }
    }

    private fun handleWebSocketError(error: Throwable) {
        Log.e("DuelActivity", "WebSocket error", error)
        Toast.makeText(this, "Connection error: ${error.message}", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun handleDuelFound(event: DuelFoundEvent) {
        // We don't need to handle questions here since they're already initialized in onCreate
        Log.d("DuelActivity", "Additional duel found event received - ignoring")
    }

    private fun handleMatchmakingFailed(event: MatchmakingFailedEvent) {
        Log.e("DuelActivity", "Matchmaking failed: ${event.reason}")
        Toast.makeText(this, "Matchmaking failed: ${event.reason}", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun setupTimer() {
        timer = object : CountDownTimer(timeLeftMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftMillis = millisUntilFinished
                updateTimerUI()
            }

            override fun onFinish() = finishDuel()
        }.start()
    }

    private fun updateTimerUI() {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeftMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeftMillis) % 60
        binding.tvTimer.text = "Time: %02d:%02d".format(minutes, seconds)
    }

    private fun updateQuestionCounter() {
        binding.tvQuestionCounter.text = "${currentQuestion + 1}/$totalQuestions"
    }

    private fun setupNextButton() {
        binding.btnNext.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.questionContainer) as? QuestionFragment
            if (currentFragment != null) {
                if (binding.btnNext.text == "Check Answer") {
                    val answer = currentFragment.getAnswer()
                    if (answer.isNotEmpty()) {
                        val isCorrect = when (currentFragment.getQuestion().type) {
                            "FILL_IN_CHOICE", "FILL_IN_INPUT" -> 
                                currentFragment.getQuestion().correctAnswers.contains(answer)
                            "SENTENCE_CONSTRUCTION" -> {
                                val correctAnswer = currentFragment.getQuestion().correctAnswers.firstOrNull()?.lowercase()?.trim() ?: ""
                                answer.lowercase().trim() == correctAnswer
                            }
                            else -> false
                        }
                        currentFragment.showFeedback(isCorrect)
                        binding.btnNext.text = "Next Question"
                    } else {
                        Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    binding.btnNext.text = "Check Answer"
                    moveToNextQuestion()
                }
            }
        }
    }

    fun moveToNextQuestion() {
        currentQuestion++
        loadNextQuestion()
    }

    fun loadNextQuestion() {
        if (currentQuestion >= totalQuestions) {
            finishDuel()
            return
        }

        val question = duelQuestions[currentQuestion]
        updateQuestionCounter()

        supportFragmentManager.commit {
            replace(R.id.questionContainer, QuestionFragment.newInstance(question))
        }
    }

    private fun finishDuel() {
        timer.cancel()
        sendResultsToServer()
        finish()
    }

    private fun sendResultsToServer() {
        val request = DuelFinishRequest(
            duelId = duelId,
            correctAnswers = currentQuestion,
            timeSpent = 2 * 60 * 1000 - timeLeftMillis
        )

        lifecycleScope.launch {
            try {
                ApiClient.duelService.finishDuel(
                    "Bearer ${tokenManager.getAccessToken()}",
                    request
                )
            } catch (e: Exception) {
                Log.e("Duel", "Results submission failed", e)
            }
        }
    }

    private fun joinMatchmaking() {
        if (!stompManager.joinMatchmaking()) {
            Toast.makeText(this, "Failed to join matchmaking", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}