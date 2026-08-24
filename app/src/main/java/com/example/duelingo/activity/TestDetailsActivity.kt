package com.example.duelingo.activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.duelingo.R
import com.example.duelingo.adapters.QuestionsPagerAdapter
import com.example.duelingo.databinding.ActivityTestDetailsBinding
import com.example.duelingo.databinding.DialogTestResultsBinding
import com.example.duelingo.dto.response.QuestionDetailedResponse
import com.example.duelingo.dto.response.TestDetailedResponse
import com.example.duelingo.fragment.QuestionFragment
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import com.example.duelingo.storage.LearningHabitTracker
import com.example.duelingo.utils.UserMessage
import kotlinx.coroutines.launch
import com.example.duelingo.dto.response.LearningRewardResponse

class TestDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTestDetailsBinding
    private lateinit var questionsAdapter: QuestionsPagerAdapter
    private var testDetails: TestDetailedResponse? = null
    private val userAnswers = mutableMapOf<Int, String>()
    private var feedbackShownForCurrentQuestion: Boolean = false

    private var questions: List<QuestionDetailedResponse>? = null
    private var isRandomTest: Boolean = false
    private var resultsShown = false
    private var learningReward: LearningRewardResponse? = null
    private var resultsBinding: DialogTestResultsBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.backButton.setOnClickListener { finish() }

        setupViewPager()
        isRandomTest = intent.getBooleanExtra("randomTest", false)
        if (isRandomTest) {
            val questions = intent.getParcelableArrayListExtra<QuestionDetailedResponse>("questions")
            if (questions != null) {
                setupRandomTest(questions)
            } else {
                showToast(getString(R.string.no_questions_available))
                finish()
            }
        } else {
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
                        learningReward = response.body()
                        resultsBinding?.let(::renderLearningReward)
                    } else {
                        Log.e("TestCompletion", "Failed to mark test as passed: ${response.errorBody()?.string()}")
                    }

                    setResult(RESULT_OK)
                } catch (e: Exception) {
                    Log.e("TestCompletion", "Error marking test as passed: ${e.message}")
                }
            }
        } else {
            Log.e("TestCompletion", "Access token is null")
        }
    }

    private fun submitTest() {
        if (resultsShown) return
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

        resultsShown = true
        LearningHabitTracker(this).recordPractice(5)
        showResultsDialog(correctAnswersCount, questionsToCheck)
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
        binding.tvTestInfo.text = getString(
            R.string.random_test_header_format,
            localizedDifficulty(intent.getStringExtra("difficulty").orEmpty())
        )
        updateButtonText(0)
    }

    private fun setupViewPager() {
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateButtonText(position)
                updateQuestionProgress(position)
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
        if (!::questionsAdapter.isInitialized || questionsAdapter.itemCount == 0) return
        binding.btnSubmit.text = if (position == questionsAdapter.itemCount - 1) {
            getString(R.string.submit_action)
        } else {
            getString(R.string.next_action)
        }
        feedbackShownForCurrentQuestion = false
        updateQuestionProgress(position)
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
    private fun showResultsDialog(correct: Int, checkedQuestions: List<QuestionDetailedResponse>) {
        val content = DialogTestResultsBinding.inflate(layoutInflater)
        resultsBinding = content
        val dialog = android.app.Dialog(this).apply {
            requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            setContentView(content.root)
            setCancelable(false)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setOnShowListener {
                val width = (resources.displayMetrics.widthPixels - 32 * resources.displayMetrics.density)
                    .toInt().coerceAtMost((520 * resources.displayMetrics.density).toInt())
                window?.setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
            }
        }
        val total = questionsAdapter.itemCount
        val percentage = if (total == 0) 0 else correct * 100 / total
        content.resultScore.text = getString(R.string.test_result_score_format, correct, total, percentage)
        content.resultMessage.setText(
            when {
                percentage == 100 -> R.string.test_result_perfect
                percentage >= 70 -> R.string.test_result_good
                else -> R.string.test_result_practice
            }
        )
        renderLearningReward(content)
        val firstMistake = checkedQuestions.indices.firstOrNull { index ->
            !answersMatch(checkedQuestions[index], userAnswers[index].orEmpty())
        }
        content.reviewButton.visibility = if (firstMistake == null) android.view.View.GONE else android.view.View.VISIBLE
        content.reviewButton.setOnClickListener {
            dialog.dismiss()
            firstMistake?.let { binding.viewPager.setCurrentItem(it, true) }
            resultsShown = false
        }
        content.retryButton.setOnClickListener {
            startActivity(Intent(this, TestDetailsActivity::class.java).putExtras(intent))
            finish()
            dialog.dismiss()
        }
        content.doneButton.setOnClickListener {
            setResult(RESULT_OK)
            dialog.dismiss()
            finish()
        }
        dialog.show()
    }

    private fun renderLearningReward(content: DialogTestResultsBinding) {
        val reward = learningReward ?: run {
            content.learningRewardContainer.visibility = android.view.View.GONE
            return
        }
        content.learningRewardContainer.visibility = android.view.View.VISIBLE
        content.learningRewardText.text = if (reward.goldAwarded > 0) {
            getString(R.string.learning_gold_reward, reward.goldAwarded)
        } else {
            getString(R.string.learning_reward_claimed)
        }
    }
    private fun updateTestInfo(test: TestDetailedResponse) {
        binding.tvTestInfo.text = getString(
            R.string.test_header_format,
            test.topic,
            localizedDifficulty(test.difficulty)
        )
    }

    private fun updateQuestionProgress(position: Int) {
        if (!::questionsAdapter.isInitialized || questionsAdapter.itemCount == 0) return
        binding.questionProgress.setProgressCompat(
            ((position + 1) * 100 / questionsAdapter.itemCount),
            true
        )
        binding.tvTestInfo.contentDescription = getString(
            R.string.question_progress_accessibility,
            position + 1,
            questionsAdapter.itemCount
        )
    }

    private fun localizedDifficulty(value: String): String = when (value) {
        "EASY" -> getString(R.string.easy)
        "HARD" -> getString(R.string.hard)
        else -> getString(R.string.medium)
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
