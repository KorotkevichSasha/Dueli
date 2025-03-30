package com.example.duelingo.activity


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.duelingo.dto.event.DuelFoundEvent
import com.example.duelingo.databinding.ActivityDuelBinding
import com.google.gson.Gson

class DuelActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDuelBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDuelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val duelInfoJson = intent.getStringExtra("DUEL_INFO")
        val duelInfo = Gson().fromJson(duelInfoJson, DuelFoundEvent::class.java)

        // todo
        binding.opponentName.text = "Opponent: ${duelInfo.opponentId}"
    }
}