package com.example.duelingo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.duelingo.adapters.OptionsAdapter
import com.example.duelingo.databinding.FragmentQuestionBinding
import com.example.duelingo.dto.response.QuestionDetailedResponse

class QuestionFragment : Fragment() {

    private lateinit var binding: FragmentQuestionBinding
    private lateinit var question: QuestionDetailedResponse
    private var selectedOption: String? = null

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
        // Логика для построения предложений (пока пусто)
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
            else -> ""
        }
    }
}