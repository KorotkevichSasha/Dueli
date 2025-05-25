package com.example.duelingo.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.duelingo.databinding.ActivityDuelResultsBinding
import java.util.concurrent.TimeUnit

class DuelResultsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDuelResultsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDuelResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val opponentName = intent.getStringExtra("opponent_name") ?: "Unknown"
        val correctAnswers = intent.getIntExtra("correct_answers", 0)
        val totalQuestions = intent.getIntExtra("total_questions", 0)
        val timeSpentMillis = intent.getLongExtra("time_spent", 0)

        binding.tvOpponentName.text = "Opponent: $opponentName"
        binding.tvYourScore.text = "$correctAnswers/$totalQuestions"
        
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeSpentMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeSpentMillis) % 60
        binding.tvTimeSpent.text = String.format("%02d:%02d", minutes, seconds)

        binding.btnFinish.setOnClickListener {
            finish()
        }
    }
} 