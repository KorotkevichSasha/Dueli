package com.example.duelingo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.example.duelingo.databinding.FragmentQuestionBinding
import com.example.duelingo.dto.response.QuestionDetailedResponse

class QuestionFragment : Fragment() {
    private var _binding: FragmentQuestionBinding? = null
    private val binding get() = _binding!!
    private lateinit var question: QuestionDetailedResponse
    private var position: Int = 0

    companion object {
        fun newInstance(question: QuestionDetailedResponse, position: Int): QuestionFragment {
            return QuestionFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("question", question)
                    putInt("position", position)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuestionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        question = arguments?.getParcelable("question") ?: throw IllegalStateException("Question is null!")
        position = arguments?.getInt("position") ?: 0

        setupQuestionUI()
    }

    private fun setupQuestionUI() {
        binding.tvQuestion.text = question.questionText

        when(question.type) {
            "FILL_IN_CHOICE" -> setupChoiceQuestion()
            "FILL_IN_INPUT" -> setupInputQuestion()
            "SENTENCE_CONSTRUCTION" -> setupSentenceConstruction()
        }
    }

    private fun setupChoiceQuestion() {
        question.options.forEach { option ->
            val radioButton = RadioButton(requireContext()).apply {
                text = option
            }
            binding.radioGroup.addView(radioButton)
        }
    }

    private fun setupInputQuestion() {
        binding.editTextAnswer.visibility = View.VISIBLE
    }

    private fun setupSentenceConstruction() {
    }

    fun getAnswer(): String {
        return when (question.type) {
            "FILL_IN_CHOICE" -> {
                val checkedId = binding.radioGroup.checkedRadioButtonId
                if (checkedId != -1) {
                    binding.radioGroup.findViewById<RadioButton>(checkedId).text.toString()
                } else {
                    ""
                }
            }
            "FILL_IN_INPUT" -> binding.editTextAnswer.text.toString()
            else -> ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}