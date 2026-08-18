package com.example.duelingo.activity

import android.animation.Animator
import android.app.Dialog
import android.content.Intent
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
import com.example.duelingo.R
import com.example.duelingo.databinding.ActivityLearningBinding
import com.example.duelingo.databinding.DialogDailyTipBinding
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import com.example.duelingo.storage.LearningHabitTracker
import com.example.duelingo.storage.OnboardingPreferences
import com.example.duelingo.dto.response.TestSummaryResponse
import com.example.duelingo.utils.LearningCatalogCache
import com.example.duelingo.utils.ProfileCache
import com.example.duelingo.utils.openTopLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.Calendar

class LearningActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLearningBinding
    private var currentAnimationView: LottieAnimationView? = null
    private var currentIcon: ImageView? = null
    private var currentText: TextView? = null
    private val tokenManager by lazy { TokenManager(this) }
    private var recommendedTopic: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLearningBinding.inflate(layoutInflater)
        setContentView(binding.root)
        showDailyTip()
        binding.learningHeroIcon.setImageResource(R.mipmap.ic_launcher_round)

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

        findViewById<View?>(R.id.learningPathCard)?.setOnClickListener {
            val topic = recommendedTopic
            startActivity(
                if (topic.isNullOrBlank()) {
                    Intent(this, TopicsActivity::class.java)
                } else {
                    Intent(this, TestActivity::class.java).putExtra("topic", topic)
                }
            )
        }

        binding.duel.setOnClickListener {
            resetAll();
            openTopLevel(MenuActivity::class.java)
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
            openTopLevel(RankActivity::class.java)
            changeColorAndIcon(binding.cupIcon, binding.cupTest, R.drawable.tro)
            playAnimation(binding.cupAnimation, binding.cupIcon, binding.cupTest, "cupAnim.json")
        }
        binding.profile.setOnClickListener {
            resetAll();
            openTopLevel(ProfileActivity::class.java)
            changeColorAndIcon(binding.profileIcon, binding.profileTest, R.drawable.prof)
            playAnimation(
                binding.profAnimation,
                binding.profileIcon,
                binding.profileTest,
                "profAnim.json"
            )
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
        val details = tipDetails(tip)
        content.tipStepOne.setText(details[0])
        content.tipStepTwo.setText(details[1])
        content.tipStepThree.setText(details[2])
        content.closeButton.setOnClickListener { dialog.dismiss() }
        content.doneButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun tipDetails(tip: String): IntArray {
        val normalized = tip.lowercase()
        return when {
            listOf("слуш", "голос", "произнес", "реч", "фильм", "ролик", "аудирован", "listen", "voice", "speak", "film", "video", "pronunciation", "shadow").any(normalized::contains) ->
                intArrayOf(R.string.tip_audio_one, R.string.tip_audio_two, R.string.tip_audio_three)
            listOf("слов", "лексик", "предлог", "артикл", "глагол", "word", "vocabulary", "preposition", "article", "verb").any(normalized::contains) ->
                intArrayOf(R.string.tip_word_one, R.string.tip_word_two, R.string.tip_word_three)
            listOf("заголов", "чита", "дневник", "пишите", "текст", "headline", "read", "journal", "write", "text").any(normalized::contains) ->
                intArrayOf(R.string.tip_read_one, R.string.tip_read_two, R.string.tip_read_three)
            listOf("кажд", "сер", "цель", "регуляр", "daily", "streak", "goal", "consistency").any(normalized::contains) ->
                intArrayOf(R.string.tip_habit_one, R.string.tip_habit_two, R.string.tip_habit_three)
            else -> intArrayOf(R.string.tip_grammar_one, R.string.tip_grammar_two, R.string.tip_grammar_three)
        }
    }

    override fun onResume() {
        super.onResume()
        loadLearningSummary()
    }

    private fun loadLearningSummary() {
        val token = tokenManager.getAccessToken() ?: return
        val cachedTests = LearningCatalogCache.read(this, token)
        val cachedProfile = ProfileCache.read(this, token)
        if (cachedTests.isNotEmpty() || cachedProfile != null) {
            renderLearningSummary(cachedProfile?.points, cachedTests)
        }
        lifecycleScope.launch {
            runCatching {
                val auth = "Bearer $token"
                val profile = async(Dispatchers.IO) { ApiClient.userService.getProfile(auth) }
                val allTests = async(Dispatchers.IO) { ApiClient.testService.getAllTests(auth) }
                val tests = allTests.await()
                profile.await() to tests
            }.onSuccess { (profile, tests) ->
                ProfileCache.store(this@LearningActivity, token, profile)
                LearningCatalogCache.store(this@LearningActivity, token, tests)
                renderLearningSummary(profile.points, tests)
            }
        }
        renderHabit()
    }

    private fun renderLearningSummary(points: Int?, tests: List<TestSummaryResponse>) {
        points?.let { binding.learningPoints.text = getString(R.string.learning_points_format, it) }
        if (tests.isNotEmpty()) {
            binding.learningTopics.text = getString(
                R.string.learning_topics_format,
                tests.map { it.topic }.distinct().size
            )
        }
        val completed = tests.count { it.isCompleted }
        val percent = if (tests.isEmpty()) 0 else completed * 100 / tests.size
        findViewById<com.google.android.material.progressindicator.LinearProgressIndicator?>(
            R.id.learningOverallProgress
        )?.setProgressCompat(percent, true)
        findViewById<TextView?>(R.id.learningProgressText)?.text =
            getString(R.string.learning_progress_format, completed, tests.size)

        val preferredDifficulty = when (OnboardingPreferences(this).learnerLevel) {
            OnboardingPreferences.LEVEL_ADVANCED -> "HARD"
            OnboardingPreferences.LEVEL_INTERMEDIATE -> "MEDIUM"
            else -> "EASY"
        }
        val next = tests.firstOrNull { !it.isCompleted && it.difficulty == preferredDifficulty }
            ?: tests.firstOrNull { !it.isCompleted }
        recommendedTopic = next?.topic
        findViewById<TextView?>(R.id.learningRecommendation)?.text = if (next == null && tests.isNotEmpty()) {
            getString(R.string.learning_all_complete)
        } else if (next == null) {
            getString(R.string.learning_topics_loading)
        } else {
            getString(
                R.string.learning_recommendation_format,
                next.topic,
                localizedDifficulty(next.difficulty)
            )
        }
    }

    private fun renderHabit() {
        val habit = LearningHabitTracker(this).snapshot()
        val goal = OnboardingPreferences(this).dailyGoalMinutes
        findViewById<TextView?>(R.id.learningHabit)?.text = getString(
            R.string.learning_habit_format,
            habit.streakDays,
            habit.minutesToday.coerceAtMost(goal),
            goal
        )
    }

    private fun localizedDifficulty(value: String): String = when (value) {
        "EASY" -> getString(R.string.easy)
        "HARD" -> getString(R.string.hard)
        else -> getString(R.string.medium)
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

        animationView.removeAllAnimatorListeners()
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
