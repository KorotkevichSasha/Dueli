package com.example.duelingo.activity

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
import com.example.duelingo.adapters.AchievementsAdapter
import com.example.duelingo.databinding.ActivityAchievementsBinding
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import kotlinx.coroutines.launch

class AchievementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAchievementsBinding
    private lateinit var adapter: AchievementsAdapter
    private var currentAnimationView: LottieAnimationView? = null
    private var currentIcon: ImageView? = null
    private var currentText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAchievementsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadAchievements()
        setupNavigationButtons()
    }

    private fun setupRecyclerView() {
        adapter = AchievementsAdapter(emptyList())
        binding.achievementRecycler.layoutManager = LinearLayoutManager(this)
        binding.achievementRecycler.adapter = adapter
    }

    private fun loadAchievements() {
        val tokenManager = TokenManager(this)
        val token = tokenManager.getAccessToken()

        if (token != null) {
            val bearerToken = "Bearer $token"
            lifecycleScope.launch {
                try {
                    val achievements = ApiClient.achievementService.getUserAchievements(bearerToken)
                    adapter.updateData(achievements)
                } catch (e: Exception) {
                    showToast("Ошибка загрузки: ${e.localizedMessage}")
                }
            }
        } else {
            showToast("Ошибка авторизации")
        }
    }

    private fun setupNavigationButtons() {
        binding.tests.setOnClickListener {
            resetAll()
            startActivity(Intent(this, LearningActivity::class.java))
            changeColorAndIcon(binding.testIcon, binding.testTest, R.drawable.grad)
            playAnimation(binding.testAnimation, binding.testIcon, binding.testTest, "graAnim.json")
        }

        binding.duel.setOnClickListener {
            resetAll()
            startActivity(Intent(this, MenuActivity::class.java))
            changeColorAndIcon(binding.mainIcon, binding.mainTest, R.drawable.swo)
            playAnimation(binding.duelAnimation, binding.mainIcon, binding.mainTest, "swordAnim.json")
        }

        binding.leaderboard.setOnClickListener {
            resetAll()
            startActivity(Intent(this, RankActivity::class.java))
            changeColorAndIcon(binding.cupIcon, binding.cupTest, R.drawable.tro)
            playAnimation(binding.cupAnimation, binding.cupIcon, binding.cupTest, "cupAnim.json")
        }

        binding.profile.setOnClickListener {
            resetAll()
            startActivity(Intent(this, ProfileActivity::class.java))
            changeColorAndIcon(binding.profileIcon, binding.profileTest, R.drawable.prof)
            playAnimation(binding.profAnimation, binding.profileIcon, binding.profileTest, "profAnim.json")
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

        animationView.addAnimatorListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {}
            override fun onAnimationEnd(animation: android.animation.Animator) {
                icon.visibility = View.VISIBLE
                animationView.visibility = View.GONE
            }
            override fun onAnimationCancel(animation: android.animation.Animator) {
                icon.visibility = View.VISIBLE
                animationView.visibility = View.GONE
            }
            override fun onAnimationRepeat(animation: android.animation.Animator) {}
        })
    }

    private fun resetAll() {
        binding.testTest.setTextColor(Color.parseColor("#7A7A7B"))
        binding.mainTest.setTextColor(Color.parseColor("#7A7A7B"))
        binding.cupTest.setTextColor(Color.parseColor("#7A7A7B"))
        binding.profileTest.setTextColor(Color.parseColor("#7A7A7B"))

        binding.testIcon.setImageResource(R.drawable.graduation24)
        binding.mainIcon.setImageResource(R.drawable.swords24)
        binding.cupIcon.setImageResource(R.drawable.trophy24)
        binding.profileIcon.setImageResource(R.drawable.profile24)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}