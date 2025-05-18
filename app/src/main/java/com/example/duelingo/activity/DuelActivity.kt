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

        // Инициализация StompManager
        stompManager = StompManager(tokenManager)

        // Проверка данных дуэли
        val duelInfoJson = intent.getStringExtra("DUEL_INFO") ?: run {
            Toast.makeText(this, "Duel data corrupted", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val duelInfo = Gson().fromJson(duelInfoJson, DuelFoundEvent::class.java)
        duelQuestions = duelInfo.duel.questions
        duelId = duelInfo.duel.id

        val opponentName = if (duelInfo.opponentId.equals(duelInfo.duel.player1.userId)) {
            duelInfo.duel.player1.username
        } else {
            duelInfo.duel.player2.username
        }

        binding.opponentName.text = "Opponent: $opponentName"

        setupTimer()
        loadNextQuestion()
    }

    override fun onStart() {
        super.onStart()
        connectToWebSocket()
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
        joinMatchmaking()
    }

    private fun handleWebSocketError(error: Throwable) {
        Log.e("DuelActivity", "WebSocket error", error)
        Toast.makeText(this, "Connection error: ${error.message}", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun handleDuelFound(event: DuelFoundEvent) {
        runOnUiThread {
            duelQuestions = event.duel.questions
            duelId = event.duel.id
            loadNextQuestion()
        }
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
        binding.tvQuestionCounter.text = "Question: ${currentQuestion + 1}/$totalQuestions"
    }

    fun loadNextQuestion() {
        if (currentQuestion >= totalQuestions) {
            finishDuel()
            return
        }

        val question = duelQuestions[currentQuestion]
        currentQuestion++
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