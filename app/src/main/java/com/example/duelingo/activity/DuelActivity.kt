package com.example.duelingo.activity


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
import com.example.duelingo.network.websocket.DuelWebSocketClient
import com.example.duelingo.storage.TokenManager
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

class DuelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDuelBinding
    private lateinit var timer: CountDownTimer
    private var timeLeftMillis: Long = 5 * 60 * 1000
    private var currentQuestion = 0
    private val totalQuestions: Int get() = duelQuestions.size
    private lateinit var duelQuestions: List<QuestionDetailedResponse>
    private lateinit var duelId: String
    private lateinit var opponentName: String
    private val webSocketClient = DuelWebSocketClient(TokenManager(this))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDuelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!TokenManager(this).isLoggedIn()) {
            finish()
            return
        }

        // Проверяем наличие данных дуэли
        val duelInfoJson = intent.getStringExtra("DUEL_INFO") ?: run {
            Toast.makeText(this, "Duel data corrupted", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val duelInfo = Gson().fromJson(duelInfoJson, DuelFoundEvent::class.java)
        duelQuestions = duelInfo.duel.questions
        duelId = duelInfo.duel.id

        binding.opponentName.text = "Opponent: $opponentName"

        setupTimer()
        loadNextQuestion()


    }

    override fun onStart() {
        super.onStart()
        webSocketClient.connect(
            onConnected = { joinMatchmaking() },
            onError = { showError(it) },
            onDuelFound = { startDuel(it) },
            onMatchmakingFailed = { showMatchmakingError(it) }
        )
    }

    override fun onStop() {
        super.onStop()
        webSocketClient.disconnect()
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
            duelId = duelId.toString(),
            correctAnswers = currentQuestion,
            timeSpent = 5 * 60 * 1000 - timeLeftMillis
        )

        lifecycleScope.launch {
            try {
                ApiClient.duelService.finishDuel(
                    "Bearer ${TokenManager(this@DuelActivity).getAccessToken()}",
                    request
                )
            } catch (e: Exception) {
                Log.e("Duel", "Results submission failed", e)
            }
        }
    }

    private fun joinMatchmaking() = webSocketClient.joinMatchmaking()
    private fun cancelMatchmaking() = webSocketClient.cancelMatchmaking()
    private fun startDuel(event: DuelFoundEvent) = runOnUiThread {
        duelQuestions = event.duel.questions
        duelId = event.duel.id
        loadNextQuestion()
    }

    private fun showError(error: Throwable) {
        Log.e("Duel", "WebSocket Error", error)
    }

    private fun showMatchmakingError(event: MatchmakingFailedEvent) {
        Log.e("Duel", "Matchmaking failed: ${event.reason}")
    }
}