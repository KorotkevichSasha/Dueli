package com.example.duelingo.activity

import android.annotation.SuppressLint
import android.content.Intent
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
import com.example.duelingo.dto.event.DuelResultEvent
import com.example.duelingo.dto.event.MatchmakingFailedEvent
import com.example.duelingo.dto.request.DuelFinishRequest
import com.example.duelingo.fragment.QuestionFragment
import com.example.duelingo.network.ApiClient
import com.example.duelingo.network.websocket.StompManager
import com.example.duelingo.storage.TokenManager
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    private var correctAnswers = 0
    private var opponentName: String = "Unknown"

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

        opponentName = opponent.username
        binding.opponentName.text = "Opponent: $opponentName"
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
            onMatchmakingFailed = ::handleMatchmakingFailed,
            onDuelResult = ::handleDuelResult
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

    private fun handleDuelResult(event: DuelResultEvent) {
        Log.d("DuelActivity", "Received duel result: $event")
        // Only handle the result if we haven't finished yet
        if (!isFinishing) {
            val intent = Intent(this, DuelResultsActivity::class.java).apply {
                putExtra("opponent_name", opponentName)
                putExtra("correct_answers", correctAnswers)
                putExtra("total_questions", totalQuestions)
                putExtra("time_spent", 2 * 60 * 1000 - timeLeftMillis)
                putExtra("opponent_score", if (event.player1Points == correctAnswers) event.player2Points else event.player1Points)
                putExtra("is_winner", event.winner != opponentName)
            }
            startActivity(intent)
            finish()
        }
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
                        if (isCorrect) correctAnswers++
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
        val timeSpent = 2 * 60 * 1000 - timeLeftMillis
        
        Log.d("DuelActivity", "Finishing duel - sending results via REST API")
        
        // Send results via REST API
        lifecycleScope.launch {
            try {
                val request = DuelFinishRequest(duelId, correctAnswers, timeSpent)
                Log.d("DuelActivity", "Sending finish request: $request")
                
                val response = withContext(Dispatchers.IO) {
                    ApiClient.duelService.finishDuel(
                        request,
                        "Bearer ${tokenManager.getAccessToken()}"
                    )
                }
                
                Log.d("DuelActivity", "Finish request sent successfully")
                showResults(true) // Show results after successful submission
            } catch (e: Exception) {
                Log.e("DuelActivity", "Failed to send results", e)
                showResults(false) // Show results even if submission failed
            }
        }
    }

    private fun showResults(resultsSent: Boolean) {
        if (!isFinishing) {
            val intent = Intent(this, DuelResultsActivity::class.java).apply {
                putExtra("opponent_name", opponentName)
                putExtra("correct_answers", correctAnswers)
                putExtra("total_questions", totalQuestions)
                putExtra("time_spent", 2 * 60 * 1000 - timeLeftMillis)
                putExtra("opponent_score", 0) // We don't know the opponent's score yet
                putExtra("is_winner", false) // We'll update this when opponent finishes
            }
            startActivity(intent)
            finish()
        }
    }

    private fun joinMatchmaking() {
        if (!stompManager.joinMatchmaking()) {
            Toast.makeText(this, "Failed to join matchmaking", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}