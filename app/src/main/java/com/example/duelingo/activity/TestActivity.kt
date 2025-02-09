package com.example.duelingo.activity

import android.animation.Animator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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
import com.example.duelingo.adapters.TestsAdapter
import com.example.duelingo.databinding.ActivityTestBinding
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import kotlinx.coroutines.launch

class TestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTestBinding
    private var currentAnimationView: LottieAnimationView? = null
    private var currentIcon: ImageView? = null
    private var currentText: TextView? = null

    private lateinit var testsAdapter: TestsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.testIcon.setColorFilter(Color.parseColor("#FF00A5FE"))
        binding.testTest.setTextColor(Color.parseColor("#FF00A5FE"))

        setupRecyclerView()
        loadTests()

        binding.tests.setOnClickListener {
        }
        binding.duel.setOnClickListener {
            resetAll();
            startActivity(Intent(this@TestActivity, MenuActivity::class.java))
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
            startActivity(Intent(this@TestActivity, RankActivity::class.java))
            changeColorAndIcon(binding.cupIcon, binding.cupTest, R.drawable.tro)
            playAnimation(binding.cupAnimation, binding.cupIcon, binding.cupTest, "cupAnim.json")
        }
        binding.profile.setOnClickListener {
            resetAll();
            startActivity(Intent(this@TestActivity, ProfileActivity::class.java))
            changeColorAndIcon(binding.profileIcon, binding.profileTest, R.drawable.prof)
            playAnimation(
                binding.profAnimation,
                binding.profileIcon,
                binding.profileTest,
                "profAnim.json"
            )
        }
    }

    private fun setupRecyclerView() {
        binding.rvTests.layoutManager = LinearLayoutManager(this)
        testsAdapter = TestsAdapter(emptyList()) { test ->
            val intent = Intent(this, TestDetailsActivity::class.java).apply {
                putExtra("testId", test.id)
            }
            startActivity(intent)
        }
        binding.rvTests.adapter = testsAdapter
    }

    private fun loadTests() {
        val tokenManager = TokenManager(this)
        val accessToken = tokenManager.getAccessToken()
        val topic = intent.getStringExtra("topic") ?: ""

        if (accessToken != null && topic.isNotEmpty()) {
            val tokenWithBearer = "Bearer $accessToken"

            lifecycleScope.launch {
                try {
                    val tests = ApiClient.testService.getTestsForTopic(tokenWithBearer, topic)
                    testsAdapter.updateData(tests)
                } catch (e: Exception) {
                    showToast("Error loading tests: ${e.message}")
                }
            }
        } else {
            showToast("Authentication error or missing topic")
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