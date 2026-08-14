package com.example.duelingo.activity

import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.duelingo.databinding.ActivityDuelResultsBinding
import java.util.concurrent.TimeUnit

class DuelResultsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDuelResultsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDuelResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val opponentName = intent.getStringExtra("opponent_name")
            ?.takeIf { it.isNotBlank() }
            ?: getString(com.example.duelingo.R.string.unknown_opponent)
        val correctAnswers = intent.getIntExtra("correct_answers", 0)
        val totalQuestions = intent.getIntExtra("total_questions", 0)
        val timeSpentMillis = intent.getLongExtra("time_spent", 0)
        val opponentScore = intent.getIntExtra("opponent_score", 0)
        val isDraw = intent.getBooleanExtra("is_draw", false)
        val isWinner = intent.getBooleanExtra("is_winner", false)
        val opponentForfeited = intent.getBooleanExtra("opponent_forfeited", false)

        val title = when {
            isDraw -> getString(com.example.duelingo.R.string.duel_draw)
            isWinner -> getString(com.example.duelingo.R.string.duel_victory)
            else -> getString(com.example.duelingo.R.string.duel_defeat)
        }
        val message = when {
            isDraw -> getString(com.example.duelingo.R.string.duel_draw_message)
            opponentForfeited -> getString(com.example.duelingo.R.string.duel_opponent_forfeited_message)
            isWinner -> getString(com.example.duelingo.R.string.duel_victory_message)
            else -> getString(com.example.duelingo.R.string.duel_defeat_message)
        }
        binding.tvResultsTitle.text = title
        binding.tvResultMessage.text = message
        binding.tvResultIcon.text = when {
            isDraw -> "🤝"
            isWinner -> "🏆"
            else -> "🌱"
        }
        binding.tvOpponentName.text = opponentName
        binding.tvYourScore.text = "$correctAnswers/$totalQuestions"
        binding.tvOpponentScore.text = "$opponentScore/$totalQuestions"
        
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeSpentMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeSpentMillis) % 60
        binding.tvTimeSpent.text = String.format("%02d:%02d", minutes, seconds)

        binding.btnFinish.setOnClickListener { returnToMenu() }
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = returnToMenu()
        })

        binding.tvResultIcon.apply {
            scaleX = 0.25f
            scaleY = 0.25f
            alpha = 0f
            animate().scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(650)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    animate().rotationBy(if (isWinner) 8f else -4f).setDuration(180)
                        .withEndAction { animate().rotation(0f).setDuration(180).start() }
                        .start()
                }.start()
        }
    }

    private fun returnToMenu() {
        startActivity(Intent(this, MenuActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        finish()
    }
}
