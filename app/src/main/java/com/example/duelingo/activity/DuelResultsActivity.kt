package com.example.duelingo.activity

import com.example.duelingo.R

import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.duelingo.databinding.ActivityDuelResultsBinding
import com.example.duelingo.databinding.DialogLeaguePromotionBinding
import java.util.concurrent.TimeUnit
import android.view.View
import android.app.Dialog
import android.view.Window
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import com.example.duelingo.utils.LeagueVisuals

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
        val goldAwarded = intent.getIntExtra("gold_awarded", 0)
        val ratingDelta = intent.getIntExtra("rating_delta", 0)
        val leagueId = intent.getStringExtra("league_id")
        val leaguePromoted = intent.getBooleanExtra("league_promoted", false)
        val previousLeagueId = intent.getStringExtra("previous_league_id")
        val leagueBonusGold = intent.getIntExtra("league_bonus_gold", 0)
        val friendlyDuel = intent.getBooleanExtra("friendly_duel", false)

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
        binding.tvResultIcon.setImageResource(when {
            isDraw -> com.example.duelingo.R.drawable.swords24
            isWinner -> com.example.duelingo.R.drawable.trophy24
            else -> com.example.duelingo.R.drawable.ic_book
        })
        binding.tvResultIcon.setColorFilter(
            androidx.core.content.ContextCompat.getColor(
                this,
                when {
                    isDraw -> com.example.duelingo.R.color.blue_primary
                    isWinner -> com.example.duelingo.R.color.gold
                    else -> com.example.duelingo.R.color.green_primary
                }
            )
        )
        binding.tvOpponentName.text = opponentName
        binding.tvYourScore.text = "$correctAnswers/$totalQuestions"
        binding.tvOpponentScore.text = "$opponentScore/$totalQuestions"
        
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeSpentMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeSpentMillis) % 60
        binding.tvTimeSpent.text = String.format("%02d:%02d", minutes, seconds)

        binding.friendlyRewardNotice.visibility = if (friendlyDuel) View.VISIBLE else View.GONE
        binding.rewardCard.visibility = if (!friendlyDuel && (goldAwarded > 0 || ratingDelta != 0)) View.VISIBLE else View.GONE
        if (binding.rewardCard.visibility == View.VISIBLE) {
            val visual = LeagueVisuals.forId(leagueId)
            binding.rewardLeagueIcon.setImageResource(visual.icon)
            binding.rewardLeagueText.text = getString(R.string.league_title_format, getString(visual.name))
            binding.goldRewardText.text = getString(R.string.duel_gold_reward, goldAwarded)
            binding.ratingRewardText.text = if (ratingDelta >= 0) {
                getString(R.string.duel_rating_gain, ratingDelta)
            } else {
                getString(R.string.duel_rating_loss, ratingDelta)
            }
        }

        binding.btnFinish.setOnClickListener { returnToMenu() }
        binding.btnReview.setOnClickListener { returnToMenu(openLatestHistory = true) }
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

        if (leaguePromoted && leagueBonusGold > 0 && leagueId != null) {
            binding.root.postDelayed({
                if (!isFinishing && !isDestroyed) {
                    showLeaguePromotion(previousLeagueId, leagueId, leagueBonusGold)
                }
            }, 650)
        }
    }

    private fun showLeaguePromotion(previousLeagueId: String?, leagueId: String, bonusGold: Int) {
        val content = DialogLeaguePromotionBinding.inflate(layoutInflater)
        val previous = LeagueVisuals.forId(previousLeagueId)
        val current = LeagueVisuals.forId(leagueId)
        content.promotionLeagueIcon.setImageResource(current.icon)
        content.promotionLeagueName.text = getString(current.name)
        content.promotionJourney.text = getString(
            R.string.league_promotion_journey,
            getString(previous.name),
            getString(current.name)
        )
        content.promotionGold.text = getString(R.string.league_promotion_gold, bonusGold)

        val dialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(content.root)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setOnShowListener {
                val horizontalPadding = (32 * resources.displayMetrics.density).toInt()
                val maxWidth = (500 * resources.displayMetrics.density).toInt()
                window?.setLayout(
                    (resources.displayMetrics.widthPixels - horizontalPadding).coerceAtMost(maxWidth),
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
                playPromotionAnimation(content)
            }
        }
        content.promotionContinue.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun playPromotionAnimation(content: DialogLeaguePromotionBinding) {
        content.promotionCard.alpha = 0f
        content.promotionCard.translationY = 70f
        content.promotionCard.animate().alpha(1f).translationY(0f).setDuration(360).start()

        content.promotionLeagueIcon.scaleX = 0.15f
        content.promotionLeagueIcon.scaleY = 0.15f
        content.promotionLeagueIcon.rotation = -18f
        content.promotionLeagueIcon.animate()
            .scaleX(1f).scaleY(1f).rotation(0f)
            .setStartDelay(140)
            .setDuration(720)
            .setInterpolator(OvershootInterpolator(1.7f))
            .start()

        listOf(content.promotionParticleLeft, content.promotionParticleTop, content.promotionParticleRight)
            .forEachIndexed { index, particle ->
                particle.alpha = 0f
                particle.scaleX = 0.2f
                particle.scaleY = 0.2f
                particle.translationY = 30f
                particle.animate()
                    .alpha(1f).scaleX(1f).scaleY(1f).translationY(0f).rotationBy(35f)
                    .setStartDelay(260L + index * 90L)
                    .setDuration(520)
                    .setInterpolator(OvershootInterpolator())
                    .start()
            }

        content.promotionGold.alpha = 0f
        content.promotionGold.translationY = 24f
        content.promotionGold.animate().alpha(1f).translationY(0f)
            .setStartDelay(520).setDuration(380).start()
    }

    private fun returnToMenu(openLatestHistory: Boolean = false) {
        startActivity(Intent(this, MenuActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MenuActivity.EXTRA_OPEN_LATEST_HISTORY, openLatestHistory)
            if (openLatestHistory) {
                putExtra(MenuActivity.EXTRA_OPEN_HISTORY_ID, intent.getStringExtra("duel_id"))
            }
        })
        finish()
    }
}
