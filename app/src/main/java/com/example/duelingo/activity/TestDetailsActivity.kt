package com.example.duelingo.activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.duelingo.R
import com.example.duelingo.adapters.QuestionsPagerAdapter
import com.example.duelingo.databinding.ActivityTestDetailsBinding
import com.example.duelingo.dto.response.QuestionDetailedResponse
import com.example.duelingo.dto.response.TestDetailedResponse
import com.example.duelingo.fragment.QuestionFragment
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import com.example.duelingo.utils.UserMessage
import kotlinx.coroutines.launch

class TestDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTestDetailsBinding
    private lateinit var questionsAdapter: QuestionsPagerAdapter
    private var testDetails: TestDetailedResponse? = null
    private val userAnswers = mutableMapOf<Int, String>()
    private var feedbackShownForCurrentQuestion: Boolean = false

    private var questions: List<QuestionDetailedResponse>? = null
    private var isRandomTest: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.backButton.setOnClickListener { finish() }

        val isRandomTest = intent.getBooleanExtra("randomTest", false)
        if (isRandomTest) {
            val questions = intent.getParcelableArrayListExtra<QuestionDetailedResponse>("questions")
            if (questions != null) {
                setupRandomTest(questions)
            } else {
                showToast(getString(R.string.no_questions_available))
                finish()
            }
        } else {
            setupViewPager()
            loadTestDetails()
        }

        setupButton()

    }
    private fun completeTest(testId: String) {
        val tokenManager = TokenManager(this)
        val accessToken = tokenManager.getAccessToken()

        if (accessToken != null) {
            val tokenWithBearer = "Bearer $accessToken"

            lifecycleScope.launch {
                try {
                    Log.d("TestCompletion", "Marking test $testId as passed...")
                    val response = ApiClient.testService.markTestAsPassed(tokenWithBearer, testId)
                    if (response.isSuccessful) {
                        Log.d("TestCompletion", "Test marked as passed successfully")
                    } else {
                        Log.e("TestCompletion", "Failed to mark test as passed: ${response.errorBody()?.string()}")
                    }

                    setResult(RESULT_OK)
                    finish()
                } catch (e: Exception) {
                    Log.e("TestCompletion", "Error marking test as passed: ${e.message}")
                }
            }
        } else {
            Log.e("TestCompletion", "Access token is null")
        }
    }

    private fun submitTest() {
        val questionsToCheck = if (intent.getBooleanExtra("randomTest", false)) {
            questions ?: run {
                showToast(getString(R.string.no_questions_available))
                return
            }
        } else {
            testDetails?.questions ?: run {
                showToast(getString(R.string.no_questions_available))
                return
            }
        }

        val correctAnswersCount = questionsToCheck.withIndex().count { (index, question) ->
            val userAnswer = userAnswers.getOrElse(index) { "" }

            Log.d("DEBUG", "Вопрос №$index (${question.type}):")
            Log.d("DEBUG", "Ответ пользователя: '$userAnswer'")
            Log.d("DEBUG", "Правильные ответы: '${question.correctAnswers}'")
            answersMatch(question, userAnswer)
        }

        if (correctAnswersCount == questionsAdapter.itemCount && !isRandomTest) {
            intent.getStringExtra("testId")?.let { testId ->
                completeTest(testId)
            }
        } else {
            Log.d("TestCompletion", "Test not completed successfully. Correct answers: $correctAnswersCount/${questionsAdapter.itemCount}")
        }

        showResultsDialog(correctAnswersCount)
    }

    private fun setupRandomTest(questions: List<QuestionDetailedResponse>) {
        this.questions = questions
        questions.forEach { question ->
            if (question.correctAnswers.isNullOrEmpty()) {
                showToast(getString(R.string.question_data_unavailable))
                finish()
                return
            }
        }
        questionsAdapter = QuestionsPagerAdapter(this, questions)
        binding.viewPager.adapter = questionsAdapter
        updateButtonText(0)
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
            showToast(getString(R.string.test_data_unavailable))
            finish()
            return
        }

        val token = TokenManager(this).getAccessToken() ?: run {
            showToast(getString(R.string.session_expired))
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
                showToast(UserMessage.from(this@TestDetailsActivity, e))
            }
        }
    }

    private fun setupButton() {
        binding.btnSubmit.setOnClickListener {
            val currentPosition = binding.viewPager.currentItem

            if (!feedbackShownForCurrentQuestion) {
                saveAnswer(currentPosition)

                val fragment = supportFragmentManager.findFragmentByTag("f${binding.viewPager.currentItem}")
                if (fragment is QuestionFragment) {
                    val userAnswer = fragment.getAnswer()
                    val currentQuestion = questionsAdapter.getItem(currentPosition)
                    val isCorrect = answersMatch(currentQuestion, userAnswer)

                    fragment.showFeedback(isCorrect)

                    if (currentPosition < questionsAdapter.itemCount - 1) {
                        binding.btnSubmit.setText(R.string.next_action)
                    } else {
                        binding.btnSubmit.setText(R.string.submit_action)
                    }

                    feedbackShownForCurrentQuestion = true
                }
            } else {
                if (binding.btnSubmit.text == getString(R.string.next_action)) {
                    binding.viewPager.currentItem = currentPosition + 1
                    feedbackShownForCurrentQuestion = false // Reset the state for the next question
                } else if (binding.btnSubmit.text == getString(R.string.submit_action)) {
                    submitTest()
                }
            }
        }
    }

    private fun saveAnswer(position: Int) {
        val fragment = supportFragmentManager.findFragmentByTag("f${binding.viewPager.currentItem}")
        if (fragment is QuestionFragment) {
            val answer = fragment.getAnswer()
            userAnswers[position] = answer
            Log.d("TestDetailsActivity", "Saved answer for position $position: $answer")
        } else {
            Log.e("TestDetailsActivity", "Fragment is not QuestionFragment")
        }
    }

    private fun updateButtonText(position: Int) {
        binding.btnSubmit.text = if (position == questionsAdapter.itemCount - 1) {
            getString(R.string.submit_action)
        } else {
            getString(R.string.next_action)
        }
        feedbackShownForCurrentQuestion = false
    }


    private fun answersMatch(question: QuestionDetailedResponse, userAnswer: String): Boolean {
        if (question.correctAnswers.isEmpty()) return false
        val normalizedUser = normalize(userAnswer)
        return if (question.type == "SENTENCE_CONSTRUCTION") {
            normalize(question.correctAnswers.joinToString(" ")) == normalizedUser
        } else {
            question.correctAnswers.any { normalize(it) == normalizedUser }
        }
    }

    private fun normalize(input: String): String = input
        .lowercase(java.util.Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}']+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
    private fun showResultsDialog(correct: Int) {
        AlertDialog.Builder(this)
            .setTitle(R.string.test_results)
            .setMessage(getString(R.string.correct_answers_format, correct, questionsAdapter.itemCount))
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
