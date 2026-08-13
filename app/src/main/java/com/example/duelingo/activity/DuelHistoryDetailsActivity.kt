package com.example.duelingo.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.duelingo.R
import com.example.duelingo.databinding.ActivityDuelHistoryDetailsBinding
import com.example.duelingo.dto.response.DuelAnswerReviewResponse
import com.example.duelingo.dto.response.DuelInHistoryResponse
import com.google.gson.Gson

class DuelHistoryDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDuelHistoryDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDuelHistoryDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val duel = intent.getStringExtra(EXTRA_DUEL)
            ?.let { runCatching { Gson().fromJson(it, DuelInHistoryResponse::class.java) }.getOrNull() }
        if (duel == null) {
            finish()
            return
        }

        binding.backButton.setOnClickListener { finish() }
        binding.matchPlayers.text = getString(
            R.string.duel_players_format,
            duel.player1.username,
            duel.player2.username
        )
        binding.matchScore.text = getString(
            R.string.duel_score_format,
            duel.player1Score,
            duel.player2Score
        )
        binding.matchMode.setText(
            if (duel.mode == "OFFLINE") R.string.duel_mode_offline_history
            else R.string.duel_mode_online_history
        )

        renderReview(binding.yourErrorsContainer, duel.yourAnswers)
        renderReview(binding.opponentErrorsContainer, duel.opponentAnswers)
    }

    private fun renderReview(
        container: LinearLayout,
        answers: List<DuelAnswerReviewResponse>
    ) {
        container.removeAllViews()
        val errors = answers.filterNot { it.correct }
        when {
            answers.isEmpty() -> container.addView(statusText(R.string.duel_details_unavailable))
            errors.isEmpty() -> container.addView(statusText(R.string.duel_no_mistakes))
            else -> errors.forEach { error ->
                val row = LayoutInflater.from(this).inflate(R.layout.item_duel_error, container, false)
                row.findViewById<TextView>(R.id.errorQuestion).text = getString(
                    R.string.duel_question_format,
                    error.questionNumber,
                    error.questionText
                )
                row.findViewById<TextView>(R.id.submittedAnswer).text = getString(
                    R.string.duel_submitted_answer_format,
                    error.submittedAnswer.ifBlank { getString(R.string.duel_no_answer) }
                )
                row.findViewById<TextView>(R.id.correctAnswer).text = getString(
                    R.string.duel_correct_answer_format,
                    error.correctAnswer
                )
                container.addView(row)
            }
        }
    }

    private fun statusText(textResource: Int) = TextView(this).apply {
        setText(textResource)
        setTextColor(getColor(R.color.gray))
        textSize = 14f
        setPadding(4, 20, 4, 20)
    }

    companion object {
        const val EXTRA_DUEL = "extra_duel"
    }
}
