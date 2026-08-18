package com.example.duelingo.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.duelingo.R
import com.example.duelingo.databinding.ActivityOnboardingBinding
import com.example.duelingo.manager.LocaleManager
import com.example.duelingo.storage.OnboardingPreferences

class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var preferences: OnboardingPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferences = OnboardingPreferences(this)

        binding.languageGroup.check(
            if (LocaleManager.getLanguage(this) == "en") R.id.languageEnglish
            else R.id.languageRussian
        )
        binding.languageGroup.setOnCheckedChangeListener { _, checkedId ->
            val language = if (checkedId == R.id.languageEnglish) "en" else "ru"
            if (language != LocaleManager.getLanguage(this)) {
                LocaleManager.setLanguage(this, language)
            }
        }

        binding.backButton.setOnClickListener {
            if (binding.onboardingPages.displayedChild > 0) {
                binding.onboardingPages.showPrevious()
                renderStep()
            }
        }
        binding.nextButton.setOnClickListener {
            if (binding.onboardingPages.displayedChild < binding.onboardingPages.childCount - 1) {
                binding.onboardingPages.showNext()
                renderStep()
            } else {
                finishOnboarding(openLearning = true)
            }
        }
        binding.skipButton.setOnClickListener { finishOnboarding(openLearning = false) }
        renderStep()
    }

    private fun renderStep() {
        val step = binding.onboardingPages.displayedChild
        binding.stepIndicator.text = getString(
            R.string.onboarding_step_format,
            step + 1,
            binding.onboardingPages.childCount
        )
        binding.backButton.isEnabled = step > 0
        binding.backButton.alpha = if (step > 0) 1f else 0f
        binding.skipButton.text = if (step == binding.onboardingPages.childCount - 1) {
            getString(R.string.onboarding_later)
        } else {
            getString(R.string.onboarding_skip)
        }
        binding.nextButton.setText(
            if (step == binding.onboardingPages.childCount - 1) {
                R.string.onboarding_start_learning
            } else {
                R.string.next_action
            }
        )
    }

    private fun finishOnboarding(openLearning: Boolean) {
        val level = when (binding.levelGroup.checkedRadioButtonId) {
            R.id.levelAdvanced -> OnboardingPreferences.LEVEL_ADVANCED
            R.id.levelIntermediate -> OnboardingPreferences.LEVEL_INTERMEDIATE
            else -> OnboardingPreferences.LEVEL_BEGINNER
        }
        val goal = when (binding.goalGroup.checkedRadioButtonId) {
            R.id.goalTwenty -> 20
            R.id.goalFive -> 5
            else -> 10
        }
        preferences.complete(level, goal)
        startActivity(
            Intent(
                this,
                if (openLearning) LearningActivity::class.java else MenuActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}
