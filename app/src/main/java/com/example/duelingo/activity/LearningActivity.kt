package com.example.duelingo.activity
import android.animation.Animator
import android.content.Intent
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.example.duelingo.R
import com.example.duelingo.databinding.ActivityLearningBinding
import com.example.duelingo.databinding.DialogDailyTipBinding
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.Calendar

class LearningActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLearningBinding
    private var currentAnimationView: LottieAnimationView? = null
    private var currentIcon: ImageView? = null
    private var currentText: TextView? = null
    private val tokenManager by lazy { TokenManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLearningBinding.inflate(layoutInflater)
        setContentView(binding.root)
        showDailyTip()
        Glide.with(this)
            .load("file:///android_asset/app-icon-512.png")
            .circleCrop()
            .into(binding.learningHeroIcon)

        binding.testIcon.setColorFilter(Color.parseColor("#FF00A5FE"))
        binding.testTest.setTextColor(Color.parseColor("#FF00A5FE"))


      binding.listeningCard.setOnClickListener {
            val intent = Intent(this, ListeningActivity::class.java)
            startActivity(intent)
        }

        binding.testsCard.setOnClickListener {
            val intent = Intent(this, TopicsActivity::class.java)
            startActivity(intent)
        }

        binding.vocabularyCard.setOnClickListener {
            startActivity(Intent(this, VocabularyActivity::class.java))
        }

        binding.duel.setOnClickListener {
            resetAll();
            startActivity(Intent(this@LearningActivity, MenuActivity::class.java))
            changeColorAndIcon(binding.mainIcon, binding.mainTest, R.drawable.swo)
            playAnimation(
                binding.duelAnimation,
                binding.mainIcon,
                binding.mainTest,
                "swordAnim.json"
            )
        }

        binding.leaderboard.setOnClickListener {
            resetAll();
            startActivity(Intent(this@LearningActivity, RankActivity::class.java))
            changeColorAndIcon(binding.cupIcon, binding.cupTest, R.drawable.tro)
            playAnimation(binding.cupAnimation, binding.cupIcon, binding.cupTest, "cupAnim.json")
        }
        binding.profile.setOnClickListener {
            resetAll();
            startActivity(Intent(this@LearningActivity, ProfileActivity::class.java))
            changeColorAndIcon(binding.profileIcon, binding.profileTest, R.drawable.prof)
            playAnimation(
                binding.profAnimation,
                binding.profileIcon,
                binding.profileTest,
                "profAnim.json"
            )
        }

        listOf(binding.learningHeroCard, binding.testsCard, binding.listeningCard,
            binding.vocabularyCard, binding.learningTipCard).forEachIndexed { index, view ->
            view.alpha = 1f
            view.translationY = 22f
            view.animate().translationY(0f)
                .setStartDelay(index * 70L).setDuration(320).start()
        }
    }

    private fun showDailyTip() {
        val tips = resources.getStringArray(R.array.daily_learning_tips)
        if (tips.isNotEmpty()) {
            val calendar = Calendar.getInstance()
            val dayKey = calendar.get(Calendar.YEAR) * 366 + calendar.get(Calendar.DAY_OF_YEAR)
            val index = Math.floorMod(dayKey, tips.size)
            binding.learningTipText.text = tips[index]
            binding.learningTipCard.isClickable = true
            binding.learningTipCard.isFocusable = true
            binding.learningTipCard.setOnClickListener {
                showTipDetails(tips[index])
            }
        }
    }

    private fun showTipDetails(tip: String) {
        val content = DialogDailyTipBinding.inflate(layoutInflater)
        val dialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(content.root)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setOnShowListener {
                val width = (resources.displayMetrics.widthPixels - 32 * resources.displayMetrics.density)
                    .toInt().coerceAtMost((520 * resources.displayMetrics.density).toInt())
                window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
            }
        }
        content.tipText.text = tip
        content.closeButton.setOnClickListener { dialog.dismiss() }
        content.doneButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        loadLearningSummary()
    }

    private fun loadLearningSummary() {
        val token = tokenManager.getAccessToken() ?: return
        lifecycleScope.launch {
            runCatching {
                val auth = "Bearer $token"
                val profile = async { ApiClient.userService.getProfile(auth) }
                val topics = async { ApiClient.testService.getUniqueTestTopics(auth) }
                profile.await() to topics.await()
            }.onSuccess { (profile, topics) ->
                binding.learningPoints.text = getString(R.string.learning_points_format, profile.points)
                binding.learningTopics.text = getString(R.string.learning_topics_format, topics.size)
            }
        }
    }

    private fun changeColorAndIcon(icon: ImageView, text: TextView, iconRes: Int) {
        text.setTextColor(ContextCompat.getColor(this, R.color.blue_primary))
        icon.setColorFilter(ContextCompat.getColor(this, R.color.blue_primary))
        icon.setImageResource(iconRes)
    }
    private fun playAnimation(animationView: LottieAnimationView, icon: ImageView, text: TextView, animationFile: String) {
        currentAnimationView?.apply {
            cancelAnimation()
            visibility = View.GONE
        }

        currentIcon?.setColorFilter(Color.parseColor("#7A7A7B"))
        currentText?.setTextColor(Color.parseColor("#7A7A7B"))
        currentIcon?.visibility = View.VISIBLE

        currentAnimationView = animationView
        currentIcon = icon
        currentText = text

        icon.visibility = View.GONE

        animationView.visibility = View.VISIBLE
        animationView.setAnimation(animationFile)
        animationView.playAnimation()

        animationView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
            }

            override fun onAnimationEnd(animation: Animator) {
                icon.visibility = View.VISIBLE
                animationView.visibility = View.GONE
            }

            override fun onAnimationCancel(animation: Animator) {
                icon.visibility = View.VISIBLE
                animationView.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animator) {
            }

            private fun playAnimation(animationFile: String) {
                binding.animationView.setAnimation(animationFile)
                binding.animationView.playAnimation()
            }
        })
    }
    private fun resetAll() {
        binding.testTest.setTextColor(Color.parseColor("#7A7A7B"))
        binding.mainTest.setTextColor(Color.parseColor("#7A7A7B"))
        binding.cupTest.setTextColor(Color.parseColor("#7A7A7B"))
        binding.profileTest.setTextColor(Color.parseColor("#7A7A7B"))

        binding.mainIcon.setColorFilter(Color.parseColor("#7A7A7B"))

        binding.testIcon.setImageResource(R.drawable.graduation24)
        binding.mainIcon.setImageResource(R.drawable.swords24)
        binding.cupIcon.setImageResource(R.drawable.trophy24)
        binding.profileIcon.setImageResource(R.drawable.profile24)
    }
}
