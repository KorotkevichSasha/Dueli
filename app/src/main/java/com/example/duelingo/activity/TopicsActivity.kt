package com.example.duelingo.activity

import android.animation.Animator
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.example.duelingo.utils.AppToast as Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieAnimationView
import com.example.duelingo.R
import com.example.duelingo.adapters.TopicsAdapter
import com.example.duelingo.databinding.ActivityTopicsBinding
import com.example.duelingo.databinding.DialogDuelDifficultyBinding
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import com.example.duelingo.utils.UserMessage
import com.example.duelingo.utils.openTopLevel
import com.example.duelingo.utils.LearningCatalogCache
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class TopicsActivity : AppCompatActivity() {
    private var currentAnimationView: LottieAnimationView? = null
    private var currentIcon: ImageView? = null
    private var currentText: TextView? = null

    private val MARK_TEST_AS_PASSED_REQUEST_CODE = 1001

    private lateinit var binding: ActivityTopicsBinding
    private lateinit var topicsAdapter: TopicsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTopicsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.backButton.setOnClickListener { finish() }

        binding.testIcon.setColorFilter(Color.parseColor("#FF00A5FE"))
        binding.testTest.setTextColor(Color.parseColor("#FF00A5FE"))

        setupRecyclerView()

        binding.tests.setOnClickListener {}
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

    override fun onResume() {
        super.onResume()
        loadTopics()
    }


    private fun loadTopics() {
        val tokenManager = TokenManager(this)
        val accessToken = tokenManager.getAccessToken()

        if (accessToken != null) {
            val tokenWithBearer = "Bearer $accessToken"
            val cached = LearningCatalogCache.read(this, accessToken)
            if (cached.isNotEmpty()) renderTopics(cached)
            binding.catalogState.visibility = if (cached.isEmpty()) View.VISIBLE else View.GONE
            binding.catalogProgress.visibility = View.VISIBLE
            binding.catalogMessage.setText(R.string.loading_learning_catalog)

            lifecycleScope.launch {
                try {
                    val allTests = ApiClient.testService.getAllTests(tokenWithBearer)
                    LearningCatalogCache.store(this@TopicsActivity, accessToken, allTests)
                    renderTopics(allTests)
                } catch (e: Exception) {
                    if (cached.isEmpty()) {
                        binding.catalogState.visibility = View.VISIBLE
                        binding.catalogProgress.visibility = View.GONE
                        binding.catalogMessage.text = getString(R.string.catalog_retry_message)
                        binding.catalogState.setOnClickListener { loadTopics() }
                    } else showToast(UserMessage.from(this@TopicsActivity, e))
                }
            }
        } else {
            showToast(getString(R.string.session_expired))
        }
    }

    private fun renderTopics(tests: List<com.example.duelingo.dto.response.TestSummaryResponse>) {
        val topics = tests.map { it.topic }.distinct()
        val completion = topics.associateWith { topic ->
            tests.filter { it.topic == topic }.groupBy { it.difficulty }
                .mapValues { (_, values) -> values.all { it.isCompleted } }
        }
        topicsAdapter.updateData(listOf("Random Test") + topics, completion)
        binding.catalogState.visibility = if (topics.isEmpty()) View.VISIBLE else View.GONE
        binding.rvTopics.visibility = if (topics.isEmpty()) View.GONE else View.VISIBLE
        if (topics.isEmpty()) {
            binding.catalogProgress.visibility = View.GONE
            binding.catalogMessage.setText(R.string.no_questions_available)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MARK_TEST_AS_PASSED_REQUEST_CODE && resultCode == RESULT_OK) {
            loadTopics()
        }
    }

    private fun setupRecyclerView() {
        binding.rvTopics.layoutManager = LinearLayoutManager(this)
        topicsAdapter = TopicsAdapter(
            emptyList(),
            onTopicClick = { topic ->
                val intent = Intent(this, TestActivity::class.java).apply {
                    putExtra("topic", topic)
                }
                startActivity(intent)
            },
            onRandomTestClick = {
                showRandomTestDifficulty()
            }
        )
        binding.rvTopics.adapter = topicsAdapter
    }

    private fun showRandomTestDifficulty() {
        val content = DialogDuelDifficultyBinding.inflate(layoutInflater)
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
        content.titleText.setText(R.string.random_test_dialog_title)
        content.subtitleText.setText(R.string.random_test_dialog_subtitle)
        content.easyButton.setText(R.string.random_test_easy)
        content.mediumButton.setText(R.string.random_test_medium)
        content.hardButton.setText(R.string.random_test_hard)
        content.closeButton.setOnClickListener { dialog.dismiss() }
        content.easyButton.setOnClickListener { dialog.dismiss(); loadRandomTest("EASY") }
        content.mediumButton.setOnClickListener { dialog.dismiss(); loadRandomTest("MEDIUM") }
        content.hardButton.setOnClickListener { dialog.dismiss(); loadRandomTest("HARD") }
        dialog.show()
    }

    private fun loadRandomTest(difficulty: String) {
        val tokenManager = TokenManager(this)
        val accessToken = tokenManager.getAccessToken()

        if (accessToken != null) {
            val tokenWithBearer = "Bearer $accessToken"

            lifecycleScope.launch {
                try {
                    val randomQuestions = ApiClient.questionService.getRandomQuestions(
                        token = tokenWithBearer,
                        topic = null,
                        difficulty = difficulty,
                        size = 10
                    )

                    if (randomQuestions.isNotEmpty()) {
                        val intent = Intent(this@TopicsActivity, TestDetailsActivity::class.java).apply {
                            putExtra("randomTest", true)
                            putExtra("difficulty", difficulty)
                            putParcelableArrayListExtra("questions", ArrayList(randomQuestions))
                        }
                        startActivity(intent)
                    } else {
                        showToast(getString(R.string.no_questions_available))
                    }
                } catch (e: Exception) {
                    showToast(UserMessage.from(this@TopicsActivity, e))
                }
            }
        } else {
            showToast(getString(R.string.session_expired))
        }
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
