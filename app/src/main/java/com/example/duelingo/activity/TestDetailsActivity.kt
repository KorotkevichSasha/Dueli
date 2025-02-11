package com.example.duelingo.activity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.duelingo.adapters.QuestionsPagerAdapter
import com.example.duelingo.databinding.ActivityTestDetailsBinding
import com.example.duelingo.dto.response.TestDetailedResponse
import com.example.duelingo.fragment.QuestionFragment
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import kotlinx.coroutines.launch

class TestDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTestDetailsBinding
    private lateinit var questionsAdapter: QuestionsPagerAdapter
    private var testDetails: TestDetailedResponse? = null
    private val userAnswers = mutableMapOf<Int, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        loadTestDetails()
        setupButton()
    }

    private fun setupViewPager() {
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateButtonText(position)
            }
        })
    }

    private fun loadTestDetails() {
        val testId = intent.getStringExtra("testId") ?: run {
            showToast("Test ID not found")
            finish()
            return
        }

        val token = TokenManager(this).getAccessToken() ?: run {
            showToast("Authentication required")
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                val test = ApiClient.testService.getTestById("Bearer $token", testId)
                testDetails = test
                updateTestInfo(test)
                questionsAdapter = QuestionsPagerAdapter(this@TestDetailsActivity, test.questions)
                binding.viewPager.adapter = questionsAdapter
                updateButtonText(0)
            } catch (e: Exception) {
                showToast("Error loading test: ${e.message}")
            }
        }
    }

    private fun setupButton() {
        binding.btnSubmit.setOnClickListener {
            val currentPosition = binding.viewPager.currentItem
            saveAnswer(currentPosition)

            if (currentPosition < questionsAdapter.itemCount - 1) {
                binding.viewPager.currentItem = currentPosition + 1
            } else {
                submitTest()
            }
        }
    }

    private fun saveAnswer(position: Int) {
        val fragment = supportFragmentManager.findFragmentByTag("f${binding.viewPager.currentItem}")
        if (fragment is QuestionFragment) {
            userAnswers[position] = fragment.getAnswer()
        }
    }

    private fun updateButtonText(position: Int) {
        binding.btnSubmit.text = if (position == questionsAdapter.itemCount - 1) {
            "Submit Test"
        } else {
            "Next"
        }
    }

    private fun submitTest() {
        val correctAnswers = testDetails?.questions?.mapIndexed { index, question ->
            userAnswers[index] in question.correctAnswers
        }?.count { it } ?: 0

        showResultsDialog(correctAnswers)
    }

    private fun showResultsDialog(correct: Int) {
        AlertDialog.Builder(this)
            .setTitle("Test Results")
            .setMessage("Correct answers: $correct/${questionsAdapter.itemCount}")
            .setPositiveButton("OK") { _, _ -> finish() }
            .show()
    }

    private fun updateTestInfo(test: TestDetailedResponse) {
        binding.tvTestInfo.text = "${test.topic} - ${test.difficulty}"
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}