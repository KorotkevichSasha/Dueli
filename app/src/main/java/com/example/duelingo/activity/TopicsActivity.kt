package com.example.duelingo.activity

import android.animation.Animator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieAnimationView
import com.example.duelingo.R
import com.example.duelingo.adapters.TopicsAdapter
import com.example.duelingo.databinding.ActivityTopicsBinding
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
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

        binding.testIcon.setColorFilter(Color.parseColor("#FF00A5FE"))
        binding.testTest.setTextColor(Color.parseColor("#FF00A5FE"))

        setupRecyclerView()
        loadTopics()

        binding.tests.setOnClickListener {}
        binding.duel.setOnClickListener {
            resetAll();
            startActivity(Intent(this@TopicsActivity, MenuActivity::class.java))
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
            startActivity(Intent(this@TopicsActivity, RankActivity::class.java))
            changeColorAndIcon(binding.cupIcon, binding.cupTest, R.drawable.tro)
            playAnimation(binding.cupAnimation, binding.cupIcon, binding.cupTest, "cupAnim.json")
        }
        binding.profile.setOnClickListener {
            resetAll();
            startActivity(Intent(this@TopicsActivity, ProfileActivity::class.java))
            changeColorAndIcon(binding.profileIcon, binding.profileTest, R.drawable.prof)
            playAnimation(
                binding.profAnimation,
                binding.profileIcon,
                binding.profileTest,
                "profAnim.json"
            )
        }
    }


    private fun loadTopics() {
        val tokenManager = TokenManager(this)
        val accessToken = tokenManager.getAccessToken()

        if (accessToken != null) {
            val tokenWithBearer = "Bearer $accessToken"

            lifecycleScope.launch {
                try {
                    val topicsDeferred = async { ApiClient.testService.getUniqueTestTopics(tokenWithBearer) }
                    val testsDeferred = topicsDeferred.await().map { topic ->
                        async {
                            val tests = ApiClient.testService.getTestsForTopic(tokenWithBearer, topic)
                            topic to tests.groupBy { it.difficulty }
                                .mapValues { (_, testsInDifficulty) ->
                                    testsInDifficulty.all { it.isCompleted }
                                }
                        }
                    }

                    val results = testsDeferred.awaitAll()
                    val completionStatus = results.toMap()

                    val randomTestTopic = "Random Test"
                    val updatedTopics = listOf(randomTestTopic) + topicsDeferred.await()

                    topicsAdapter.updateData(updatedTopics, completionStatus)
                } catch (e: Exception) {
                    showToast("Error loading topics: ${e.message}")
                }
            }
        } else {
            showToast("Authentication error")
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
                loadRandomTest()
            }
        )
        binding.rvTopics.adapter = topicsAdapter
    }

    private fun loadRandomTest() {
        val tokenManager = TokenManager(this)
        val accessToken = tokenManager.getAccessToken()

        if (accessToken != null) {
            val tokenWithBearer = "Bearer $accessToken"

            lifecycleScope.launch {
                try {
                    val randomQuestions = ApiClient.questionService.getRandomQuestions(
                        tokenWithBearer,
                        null,
                        null,
                        10
                    )

                    if (randomQuestions.isNotEmpty()) {
                        val intent = Intent(this@TopicsActivity, TestDetailsActivity::class.java).apply {
                            putExtra("randomTest", true)
                            putParcelableArrayListExtra("questions", ArrayList(randomQuestions))
                        }
                        startActivity(intent)
                    } else {
                        showToast("No random questions available")
                    }
                } catch (e: Exception) {
                    showToast("Error loading random test: ${e.message}")
                }
            }
        } else {
            showToast("Authentication error")
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