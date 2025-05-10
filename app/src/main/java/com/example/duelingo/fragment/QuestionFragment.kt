package com.example.duelingo.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.duelingo.R
import com.example.duelingo.activity.DuelActivity
import com.example.duelingo.adapters.OptionsAdapter
import com.example.duelingo.databinding.FragmentQuestionBinding
import com.example.duelingo.dto.response.QuestionDetailedResponse

class QuestionFragment : Fragment() {

    private lateinit var binding: FragmentQuestionBinding
    private lateinit var question: QuestionDetailedResponse
    private var selectedOption: String? = null
    private var feedbackShown: Boolean = false
    private var onAnswerListener: ((Boolean) -> Unit)? = null

    companion object {
        fun newInstance(question: QuestionDetailedResponse): QuestionFragment {
            return QuestionFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("question", question)
                }
            }
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentQuestionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        question = arguments?.getParcelable<QuestionDetailedResponse>("question") ?: return

        setupQuestionUI()
    }

    fun setQuestionAnsweredListener(listener: (isCorrect: Boolean) -> Unit) {
        this.onAnswerListener = listener
    }

    // В обработчике ответа добавляем:
    private fun handleAnswer(isCorrect: Boolean) {
        onAnswerListener?.invoke(isCorrect)
        (activity as? DuelActivity)?.loadNextQuestion()
    }


    fun showFeedback(isCorrect: Boolean) {
        binding.tvFeedback.visibility = View.VISIBLE
        binding.tvCorrectAnswer.visibility = if (!isCorrect) View.VISIBLE else View.GONE

        if (isCorrect) {
            binding.tvFeedback.text = "Correct!"
            binding.tvFeedback.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green))
        } else {
            binding.tvFeedback.text = "Incorrect!"
            binding.tvFeedback.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))

            val formattedAnswer = formatCorrectAnswer(question.correctAnswers.toString())
            binding.tvCorrectAnswer.text = "Correct answer: $formattedAnswer"
        }
    }
    private fun formatCorrectAnswer(correctAnswer: String): String {
        return correctAnswer
            .removeSurrounding("[", "]")
            .replace(",", "")
            .replace("  ", " ")
            .trim()
    }

    private fun setupQuestionUI() {
        binding.tvQuestionText.text = question.questionText

        when (question.type) {
            "FILL_IN_CHOICE" -> setupChoiceQuestion()
            "FILL_IN_INPUT" -> setupInputQuestion()
            "SENTENCE_CONSTRUCTION" -> setupSentenceConstruction()
        }
    }

    private fun setupChoiceQuestion() {
        val optionsAdapter = OptionsAdapter(question.options) { option ->
            onOptionSelected(option)
        }
        binding.rvOptions.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvOptions.adapter = optionsAdapter
    }

    private fun setupInputQuestion() {
        binding.editTextAnswer.visibility = View.VISIBLE
    }

    private fun setupSentenceConstruction() {
        binding.containerWordBank.visibility = View.VISIBLE
        binding.containerSelectedWords.visibility = View.VISIBLE

        binding.containerWordBank.removeAllViews()
        binding.containerSelectedWords.removeAllViews()

        question.options.shuffled().forEach { word ->
            val button = Button(requireContext()).apply {
                text = word
                setOnClickListener {
                    moveWordToSelected(word)
                }
                setPadding(16, 8, 16, 8)
                textSize = 14f
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_option)
            }
            binding.containerWordBank.addView(button)
        }
    }

    private fun moveWordToSelected(word: String) {
        val sourceContainer = if (isWordInBank(word)) binding.containerWordBank else binding.containerSelectedWords
        val targetContainer = if (isWordInBank(word)) binding.containerSelectedWords else binding.containerWordBank

        for (i in 0 until sourceContainer.childCount) {
            val view = sourceContainer.getChildAt(i)
            if (view is Button && view.text == word) {
                sourceContainer.removeView(view)
                break
            }
        }

        val button = Button(requireContext()).apply {
            text = word
            setOnClickListener {
                moveWordToSelected(word)
            }
        }
        targetContainer.addView(button)
    }

    private fun isWordInBank(word: String): Boolean {
        for (i in 0 until binding.containerWordBank.childCount) {
            val view = binding.containerWordBank.getChildAt(i)
            if (view is Button && view.text == word) {
                return true
            }
        }
        return false
    }

    private fun onOptionSelected(option: String) {
        if (selectedOption == option) {
            selectedOption = null
            binding.tvSelectedWord.visibility = View.GONE
        } else {
            selectedOption = option
            binding.tvSelectedWord.text = option
            binding.tvSelectedWord.visibility = View.VISIBLE
        }

        binding.rvOptions.adapter?.notifyDataSetChanged()
    }

    fun getAnswer(): String {
        return when (question.type) {
            "FILL_IN_CHOICE" -> selectedOption ?: ""
            "FILL_IN_INPUT" -> binding.editTextAnswer.text.toString()
            "SENTENCE_CONSTRUCTION" -> {
                val sentence = getSelectedSentence()
                Log.d("QuestionFragment: Selected Sentence:", sentence)
                sentence
            }
            else -> ""
        }
    }

    private fun getSelectedSentence(): String {
        val words = mutableListOf<String>()
        for (i in 0 until binding.containerSelectedWords.childCount) {
            val view = binding.containerSelectedWords.getChildAt(i)
            if (view is Button) {
                words.add(view.text.toString())
            }
        }
        return words.joinToString(" ").trim().lowercase()
    }
}