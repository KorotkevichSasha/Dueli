package com.example.duelingo.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.duelingo.R
import com.example.duelingo.dto.response.DuelInHistoryResponse
import com.example.duelingo.manager.AvatarManager
import de.hdodenhof.circleimageview.CircleImageView
import java.util.concurrent.TimeUnit

class DuelHistoryAdapter(
    private val duelsList: List<DuelInHistoryResponse>, private val avatarManager: AvatarManager
) :
    RecyclerView.Adapter<DuelHistoryAdapter.DuelHistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DuelHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_duel_history, parent, false)
        return DuelHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DuelHistoryViewHolder, position: Int) {
        val duel = duelsList[position]
        holder.bind(duel, avatarManager)
    }

    override fun getItemCount(): Int = duelsList.size

    class DuelHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val player1Avatar: CircleImageView = itemView.findViewById(R.id.player1Avatar)
        private val player1Name: TextView = itemView.findViewById(R.id.player1Name)
        private val player1Score: TextView = itemView.findViewById(R.id.player1Score)
        private val player1Time: TextView = itemView.findViewById(R.id.player1Time)
        
        private val player2Avatar: CircleImageView = itemView.findViewById(R.id.player2Avatar)
        private val player2Name: TextView = itemView.findViewById(R.id.player2Name)
        private val player2Score: TextView = itemView.findViewById(R.id.player2Score)
        private val player2Time: TextView = itemView.findViewById(R.id.player2Time)

        fun bind(duel: DuelInHistoryResponse, avatarManager: AvatarManager) {
            // Player 1
            player1Name.text = duel.player1.username
            player1Score.text = duel.player1Score.toString()
            player1Time.text = formatTime(duel.player1Time)
            avatarManager.loadAvatar(duel.player1.userId.toString(), player1Avatar)

            // Player 2
            player2Name.text = duel.player2.username
            player2Score.text = duel.player2Score.toString()
            player2Time.text = formatTime(duel.player2Time)
            Log.d("DuelHistoryAdapter","Player 1 id" + duel.player1.userId)
            avatarManager.loadAvatar(duel.player2.userId.toString(), player2Avatar)

            val winner = if (duel.player1Score > duel.player2Score) 1 else 2
            player1Score.setTextColor(itemView.context.getColor(
                if (winner == 1) R.color.winner_color else R.color.loser_color
            ))
            player2Score.setTextColor(itemView.context.getColor(
                if (winner == 2) R.color.winner_color else R.color.loser_color
            ))
        }

        private fun formatTime(timeInMillis: Long): String {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
    }
} 