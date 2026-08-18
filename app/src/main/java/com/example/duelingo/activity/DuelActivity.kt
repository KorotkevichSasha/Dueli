package com.example.duelingo.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.example.duelingo.R
import com.example.duelingo.dto.response.QuestionDetailedResponse
import com.example.duelingo.dto.response.DuelAnswerReviewResponse
import com.example.duelingo.dto.response.DuelInHistoryResponse
import com.example.duelingo.databinding.ActivityDuelBinding
import com.example.duelingo.dto.event.DuelFoundEvent
import com.example.duelingo.dto.event.DuelResultEvent
import com.example.duelingo.dto.event.MatchmakingFailedEvent
import com.example.duelingo.dto.request.DuelFinishRequest
import com.example.duelingo.dto.request.DuelAnswerRequest
import com.example.duelingo.fragment.QuestionFragment
import com.example.duelingo.network.ApiClient
import com.example.duelingo.network.websocket.StompManager
import com.example.duelingo.storage.TokenManager
import com.example.duelingo.storage.OfflineDuelHistoryStore
import com.example.duelingo.storage.LearningHabitTracker
import com.example.duelingo.utils.RefreshEvents
import com.example.duelingo.utils.UserMessage
import com.google.gson.Gson
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class DuelActivity : AppCompatActivity() {

    companion object {
        private const val DUEL_DURATION_MILLIS = 2 * 60 * 1000L
        private const val STATE_TIME_LEFT = "duel_time_left"
        private const val STATE_CURRENT_QUESTION = "duel_current_question"
        private const val STATE_CORRECT_ANSWERS = "duel_correct_answers"
        private const val STATE_SUBMITTED_ANSWERS = "duel_submitted_answers"
        private const val STATE_QUESTION_LOADED = "duel_question_loaded"
        private const val STATE_ANSWER_CHECKED = "duel_answer_checked"
        const val EXTRA_OFFLINE_DUEL = "OFFLINE_DUEL"
    }

    private lateinit var binding: ActivityDuelBinding
    private lateinit var timer: CountDownTimer
    private var duelDurationMillis: Long = DUEL_DURATION_MILLIS
    private var timeLeftMillis: Long = DUEL_DURATION_MILLIS
    private var currentQuestion = 0
    private val totalQuestions: Int get() = duelQuestions.size
    private lateinit var duelQuestions: List<QuestionDetailedResponse>
    private lateinit var duelId: String
    private lateinit var stompManager: StompManager
    private lateinit var tokenManager: TokenManager
    private var isQuestionLoaded = false
    private var correctAnswers = 0
    private val submittedAnswers = linkedMapOf<String, String>()
    private var isFinishingDuel = false
    private var opponentName: String = ""
    private var opponentIsPlayerOne = false
    private var offlineDuel = false
    private var duelDifficulty = "MEDIUM"
    private var isLeavingDuel = false
    private lateinit var duelInfo: DuelFoundEvent

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDuelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)
        offlineDuel = intent.getBooleanExtra(EXTRA_OFFLINE_DUEL, false)
        if (!offlineDuel && !tokenManager.isLoggedIn()) {
            finish()
            return
        }

        // Initialize StompManager
        stompManager = StompManager(tokenManager)

        // Check duel data
        val duelInfoJson = intent.getStringExtra("DUEL_INFO") ?: run {
            Toast.makeText(this, R.string.duel_data_unavailable, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        duelInfo = Gson().fromJson(duelInfoJson, DuelFoundEvent::class.java)
        duelDifficulty = duelInfo.difficulty
        duelDurationMillis = duelInfo.durationMillis.coerceIn(60_000L, 180_000L)
        timeLeftMillis = duelDurationMillis
        initializeDuel(duelInfo)
        restoreDuelState(savedInstanceState)
        setupTimer()
        setupNextButton()
        setupExitAction()
        if (savedInstanceState == null) {
            loadFirstQuestion()
        } else {
            updateTimerUI()
            updateQuestionCounter()
            if (supportFragmentManager.findFragmentById(R.id.questionContainer) == null) {
                loadNextQuestion()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun restoreDuelState(state: Bundle?) {
        if (state == null) return
        timeLeftMillis = state.getLong(STATE_TIME_LEFT, duelDurationMillis)
        currentQuestion = state.getInt(STATE_CURRENT_QUESTION, 0)
            .coerceIn(0, (totalQuestions - 1).coerceAtLeast(0))
        correctAnswers = state.getInt(STATE_CORRECT_ANSWERS, 0)
        isQuestionLoaded = state.getBoolean(STATE_QUESTION_LOADED, duelQuestions.isNotEmpty())
        submittedAnswers.clear()
        state.getSerializable(STATE_SUBMITTED_ANSWERS)?.let { restored ->
            @Suppress("UNCHECKED_CAST")
            submittedAnswers.putAll(restored as? Map<String, String> ?: emptyMap())
        }
        binding.btnNext.setText(
            if (state.getBoolean(STATE_ANSWER_CHECKED, false)) {
                R.string.next_question
            } else {
                R.string.check_answer
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_TIME_LEFT, timeLeftMillis)
        outState.putInt(STATE_CURRENT_QUESTION, currentQuestion)
        outState.putInt(STATE_CORRECT_ANSWERS, correctAnswers)
        outState.putSerializable(STATE_SUBMITTED_ANSWERS, LinkedHashMap(submittedAnswers))
        outState.putBoolean(STATE_QUESTION_LOADED, isQuestionLoaded)
        outState.putBoolean(
            STATE_ANSWER_CHECKED,
            binding.btnNext.text == getString(R.string.next_question)
        )
    }

    private fun initializeDuel(duelInfo: DuelFoundEvent) {
        if (isQuestionLoaded) {
            return // Prevent re-initialization if questions are already loaded
        }

        duelQuestions = duelInfo.duel.questions
        duelId = duelInfo.duel.id

        opponentIsPlayerOne = duelInfo.opponentId == duelInfo.duel.player1.userId.toString()
        val opponent = if (opponentIsPlayerOne) {
            duelInfo.duel.player1
        } else {
            duelInfo.duel.player2
        }

        opponentName = opponent.username
        binding.opponentName.text = getString(R.string.opponent_name_format, opponentName)
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
        if (offlineDuel) return
        // The result is delivered through this socket as well. Keeping it
        // connected during an active duel is required to receive the real
        // opponent score and winner instead of showing a placeholder 0:0.
        connectToWebSocket()
    }

    override fun onStop() {
        super.onStop()
        if (!offlineDuel) stompManager.disconnect()
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
        // This activity already owns a concrete duel received from MenuActivity.
        // Rejoining matchmaking here created a second queue entry whenever the
        // socket reconnected (for example after rotation or returning from a
        // result screen), which could immediately launch another duel.
    }

    private fun handleWebSocketError(error: Throwable) {
        Log.e("DuelActivity", "WebSocket error", error)
        Toast.makeText(this, UserMessage.from(this, error), Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun handleDuelFound(event: DuelFoundEvent) {
        // We don't need to handle questions here since they're already initialized in onCreate
        Log.d("DuelActivity", "Additional duel found event received - ignoring")
    }

    private fun handleMatchmakingFailed(event: MatchmakingFailedEvent) {
        Log.e("DuelActivity", "Matchmaking failed: ${event.reason}")
        Toast.makeText(this, R.string.error_matchmaking_failed, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun handleDuelResult(event: DuelResultEvent) {
        Log.d("DuelActivity", "Received duel result: $event")
        if (isLeavingDuel && event.forfeitedBy != opponentName) {
            return
        }
        // Only handle the result if we haven't finished yet
        if (!isFinishing) {
            val intent = Intent(this, DuelResultsActivity::class.java).apply {
                putExtra("duel_id", duelId)
                putExtra("opponent_name", opponentName)
                putExtra("correct_answers", correctAnswers)
                putExtra("total_questions", totalQuestions)
                putExtra("time_spent", duelDurationMillis - timeLeftMillis)
                putExtra("opponent_score", if (opponentIsPlayerOne) event.player1Score else event.player2Score)
                putExtra("is_winner", event.winner != opponentName && event.winner != "Draw")
                putExtra("is_draw", event.winner == "Draw")
                putExtra("opponent_forfeited", event.forfeitedBy == opponentName)
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
        binding.tvTimer.text = getString(R.string.timer_format, minutes, seconds)
    }

    private fun updateQuestionCounter() {
        binding.tvQuestionCounter.text = "${currentQuestion + 1}/$totalQuestions"
    }

    private fun setupNextButton() {
        binding.btnNext.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.questionContainer) as? QuestionFragment
            if (currentFragment != null) {
                if (binding.btnNext.text == getString(R.string.check_answer)) {
                    val answer = currentFragment.getAnswer()
                    if (answer.isNotEmpty()) {
                        val isCorrect = when (currentFragment.getQuestion().type) {
                            "FILL_IN_CHOICE", "FILL_IN_INPUT" ->
                                currentFragment.getQuestion().correctAnswers.any {
                                    normalizeAnswer(it) == normalizeAnswer(answer)
                                }
                            "SENTENCE_CONSTRUCTION" -> {
                                val correctAnswer = currentFragment.getQuestion().correctAnswers.joinToString(" ")
                                normalizeAnswer(answer) == normalizeAnswer(correctAnswer)
                            }
                            else -> false
                        }
                        if (isCorrect) correctAnswers++
                        submittedAnswers[currentFragment.getQuestion().id] = answer.trim()
                        currentFragment.showFeedback(isCorrect)
                        binding.btnNext.setText(R.string.next_question)
                    } else {
                        Toast.makeText(this, R.string.answer_required, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    binding.btnNext.setText(R.string.check_answer)
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
            setCustomAnimations(R.anim.slide_in_right, R.anim.fade_out)
            replace(R.id.questionContainer, QuestionFragment.newInstance(question))
        }
    }

    private fun setupExitAction() {
        binding.btnExitDuel.setOnClickListener { confirmExitDuel() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = confirmExitDuel()
        })
    }

    private fun confirmExitDuel() {
        if (isFinishingDuel || isLeavingDuel) return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.leave_duel_title)
            .setMessage(if (offlineDuel) R.string.leave_offline_duel_message else R.string.leave_duel_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.leave_duel) { _, _ -> leaveDuel() }
            .show()
    }

    private fun leaveDuel() {
        if (isLeavingDuel) return
        isLeavingDuel = true
        timer.cancel()
        if (offlineDuel) {
            returnToMenu()
            return
        }
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    ApiClient.duelService.forfeitDuel(
                        duelId,
                        "Bearer ${tokenManager.getAccessToken()}"
                    )
                }
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    RefreshEvents.notifyChanged(RefreshEvents.DataSet.DUEL_HISTORY)
                    returnToMenu()
                } else {
                    isLeavingDuel = false
                    setupTimer()
                    Toast.makeText(this@DuelActivity, R.string.leave_duel_failed, Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                isLeavingDuel = false
                setupTimer()
                Toast.makeText(this@DuelActivity, R.string.leave_duel_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun returnToMenu() {
        startActivity(Intent(this, MenuActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        finish()
    }

    private fun normalizeAnswer(value: String): String = value
        .lowercase()
        .replace(Regex("[.,!?;:]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun finishDuel() {
        if (isFinishingDuel) return
        isFinishingDuel = true
        LearningHabitTracker(this).recordPractice(10)
        timer.cancel()
        val timeSpent = duelDurationMillis - timeLeftMillis

        if (offlineDuel) {
            val targetAccuracy = when (duelDifficulty) { "EASY" -> 0.48; "HARD" -> 0.82; else -> 0.68 }
            val opponentScore = ((totalQuestions * targetAccuracy).toInt() + (-1..1).random())
                .coerceIn(0, totalQuestions)
            val opponentTime = when (duelDifficulty) {
                "EASY" -> (95_000L..155_000L).random()
                "HARD" -> (48_000L..72_000L).random()
                else -> (65_000L..105_000L).random()
            }.coerceAtMost(duelDurationMillis)
            saveOfflineHistory(opponentScore, timeSpent, opponentTime)
            val intent = Intent(this, DuelResultsActivity::class.java).apply {
                putExtra("duel_id", duelId)
                putExtra("opponent_name", opponentName)
                putExtra("correct_answers", correctAnswers)
                putExtra("total_questions", totalQuestions)
                putExtra("time_spent", timeSpent)
                putExtra("opponent_score", opponentScore)
                putExtra("is_winner", correctAnswers > opponentScore)
                putExtra("is_draw", correctAnswers == opponentScore)
            }
            startActivity(intent)
            finish()
            return
        }
        
        Log.d("DuelActivity", "Finishing duel - sending results via REST API")
        
        // Send results via REST API
        lifecycleScope.launch {
            try {
                val answers = duelQuestions.map { question ->
                    DuelAnswerRequest(question.id, submittedAnswers[question.id].orEmpty())
                }
                val request = DuelFinishRequest(duelId, correctAnswers, timeSpent, answers)
                Log.d("DuelActivity", "Sending finish request: $request")
                
                val response = withContext(Dispatchers.IO) {
                    ApiClient.duelService.finishDuel(
                        request,
                        "Bearer ${tokenManager.getAccessToken()}"
                    )
                }
                if (!response.isSuccessful) {
                    throw IllegalStateException("Server rejected duel result (${response.code()})")
                }
                
                Log.d("DuelActivity", "Finish request sent successfully")
                RefreshEvents.notifyChanged(RefreshEvents.DataSet.DUEL_HISTORY)
                RefreshEvents.notifyChanged(RefreshEvents.DataSet.LEADERBOARD)
                RefreshEvents.notifyChanged(RefreshEvents.DataSet.PROFILE)
                Toast.makeText(this@DuelActivity, R.string.duel_waiting_for_result, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("DuelActivity", "Failed to send results", e)
                showResults(false)
            }
        }
    }

    private fun showResults(resultsSent: Boolean) {
        if (!isFinishing) {
            val intent = Intent(this, DuelResultsActivity::class.java).apply {
                putExtra("duel_id", duelId)
                putExtra("opponent_name", opponentName)
                putExtra("correct_answers", correctAnswers)
                putExtra("total_questions", totalQuestions)
                putExtra("time_spent", duelDurationMillis - timeLeftMillis)
                putExtra("opponent_score", 0) // We don't know the opponent's score yet
                putExtra("is_winner", false) // We'll update this when opponent finishes
            }
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        if (::timer.isInitialized) timer.cancel()
        super.onDestroy()
    }

    private fun saveOfflineHistory(opponentScore: Int, userTime: Long, opponentTime: Long) {
        val userReview = duelQuestions.mapIndexed { index, question ->
            reviewAnswer(index, question, submittedAnswers[question.id].orEmpty())
        }
        val correctBotIndexes = duelQuestions.indices.shuffled().take(opponentScore).toSet()
        val botReview = duelQuestions.mapIndexed { index, question ->
            val answer = if (index in correctBotIndexes) {
                answerForDisplay(question)
            } else {
                question.options.firstOrNull { option -> !answerIsCorrect(question, option) }.orEmpty()
            }
            reviewAnswer(index, question, answer)
        }

        val player1Score = if (opponentIsPlayerOne) opponentScore else correctAnswers
        val player2Score = if (opponentIsPlayerOne) correctAnswers else opponentScore
        val player1Time = if (opponentIsPlayerOne) opponentTime else userTime
        val player2Time = if (opponentIsPlayerOne) userTime else opponentTime
        OfflineDuelHistoryStore(this, tokenManager.getAccessToken()).add(
            DuelInHistoryResponse(
                id = UUID.fromString(duelId),
                player1 = duelInfo.duel.player1,
                player1Score = player1Score,
                player1Time = player1Time,
                player2 = duelInfo.duel.player2,
                player2Score = player2Score,
                player2Time = player2Time,
                mode = "OFFLINE",
                yourAnswers = userReview,
                opponentAnswers = botReview
            )
        )
    }

    private fun reviewAnswer(
        index: Int,
        question: QuestionDetailedResponse,
        submitted: String
    ) = DuelAnswerReviewResponse(
        questionNumber = index + 1,
        questionText = question.questionText,
        type = question.type,
        submittedAnswer = submitted,
        correctAnswer = answerForDisplay(question),
        correct = answerIsCorrect(question, submitted)
    )

    private fun answerForDisplay(question: QuestionDetailedResponse): String =
        if (question.type == "SENTENCE_CONSTRUCTION") {
            question.correctAnswers.joinToString(" ")
        } else {
            question.correctAnswers.joinToString(" / ")
        }

    private fun answerIsCorrect(question: QuestionDetailedResponse, answer: String): Boolean =
        if (question.type == "SENTENCE_CONSTRUCTION") {
            normalizeAnswer(answer) == normalizeAnswer(question.correctAnswers.joinToString(" "))
        } else {
            question.correctAnswers.any { normalizeAnswer(it) == normalizeAnswer(answer) }
        }
}
