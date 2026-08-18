package com.example.duelingo.fragment

import android.os.Bundle
import android.content.Context
import android.content.ClipData
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.duelingo.R
import com.example.duelingo.activity.DuelActivity
import com.example.duelingo.adapters.OptionsAdapter
import com.example.duelingo.databinding.FragmentQuestionBinding
import com.example.duelingo.dto.response.QuestionDetailedResponse
import com.example.duelingo.utils.QuestionPromptParser
import com.google.android.material.chip.Chip
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator

class QuestionFragment : Fragment() {

    private lateinit var binding: FragmentQuestionBinding
    private lateinit var question: QuestionDetailedResponse
    private var selectedOption: String? = null
    private var feedbackShown: Boolean = false
    private var hintVisible: Boolean = false
    private var onAnswerListener: ((Boolean) -> Unit)? = null

    companion object {
        private const val STATE_HINT_VISIBLE = "question_hint_visible"

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
        hintVisible = savedInstanceState?.getBoolean(STATE_HINT_VISIBLE) ?: false

        setupQuestionUI()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_HINT_VISIBLE, hintVisible)
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
        binding.editTextAnswer.clearFocus()
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(binding.root.windowToken, 0)

        binding.tvFeedback.visibility = View.VISIBLE
        binding.tvCorrectAnswer.visibility = if (!isCorrect) View.VISIBLE else View.GONE

        if (isCorrect) {
            binding.tvFeedback.text = getString(R.string.correct_answer)
            binding.tvFeedback.setBackgroundResource(R.drawable.bg_feedback_correct)
        } else {
            binding.tvFeedback.text = getString(R.string.incorrect_answer)
            binding.tvFeedback.setBackgroundResource(R.drawable.bg_feedback_incorrect)

            val formattedAnswer = question.correctAnswers.joinToString(" ")
            binding.tvCorrectAnswer.text = getString(R.string.correct_answer_format, formattedAnswer)
        }

        binding.root.post {
            val target = if (isCorrect) binding.tvFeedback else binding.tvCorrectAnswer
            binding.root.smoothScrollTo(0, target.bottom)
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
        val prompt = QuestionPromptParser.parse(question.questionText)
        binding.tvQuestionText.text = prompt.text
        setupHint(prompt.hint)

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
        binding.rvOptions.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvOptions.adapter = optionsAdapter
    }

    private fun setupInputQuestion() {
        binding.editTextAnswer.visibility = View.VISIBLE
    }

    private fun setupSentenceConstruction() {
        binding.tvQuestionInstruction.visibility = View.VISIBLE
        binding.tvSentenceDraftLabel.visibility = View.VISIBLE
        binding.containerWordBank.visibility = View.VISIBLE
        binding.containerSelectedWords.visibility = View.VISIBLE

        binding.containerWordBank.removeAllViews()
        binding.containerSelectedWords.removeAllViews()
        binding.containerSelectedWords.minimumHeight =
            (72 * resources.displayMetrics.density).toInt()
        binding.containerSelectedWords.contentDescription =
            getString(R.string.sentence_answer_area_description)
        enableDropZone(binding.containerWordBank)
        enableDropZone(binding.containerSelectedWords)

        question.options.shuffled().forEach { word ->
            binding.containerWordBank.addView(createWordChip(word))
        }
    }

    private fun setupHint(hint: String?) {
        if (hint.isNullOrBlank()) {
            binding.tapHintLabel.visibility = View.GONE
            binding.cardHint.visibility = View.GONE
            binding.questionCard.isClickable = false
            return
        }

        binding.tvHint.text = hint
        binding.tvHintBack.text = hint
        val inDuel = activity is DuelActivity
        if (inDuel) {
            hintVisible = true
            binding.tapHintLabel.visibility = View.GONE
            binding.cardHint.visibility = View.VISIBLE
            binding.questionFront.visibility = View.VISIBLE
            binding.questionBack.visibility = View.GONE
            binding.questionCard.isClickable = false
            binding.questionCard.contentDescription = getString(R.string.question_with_visible_hint)
        } else {
            binding.cardHint.visibility = View.GONE
            binding.tapHintLabel.visibility = View.VISIBLE
            binding.questionCard.isClickable = true
            applyHintSide(showHint = hintVisible, animate = false)
            binding.questionCard.setOnClickListener {
                applyHintSide(showHint = !hintVisible, animate = true)
            }
        }
    }

    private fun applyHintSide(showHint: Boolean, animate: Boolean) {
        val current = if (hintVisible) binding.questionBack else binding.questionFront
        val next = if (showHint) binding.questionBack else binding.questionFront
        hintVisible = showHint
        binding.questionCard.contentDescription = getString(
            if (showHint) R.string.hint_visible_description else R.string.show_hint_description
        )

        if (!animate) {
            binding.questionFront.visibility = if (showHint) View.GONE else View.VISIBLE
            binding.questionBack.visibility = if (showHint) View.VISIBLE else View.GONE
            binding.questionFront.rotationY = 0f
            binding.questionBack.rotationY = 0f
            return
        }

        binding.questionCard.isClickable = false
        val distance = 10_000f * resources.displayMetrics.density
        current.cameraDistance = distance
        next.cameraDistance = distance
        current.animate()
            .rotationY(90f)
            .setDuration(150L)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                current.visibility = View.GONE
                current.rotationY = 0f
                next.rotationY = -90f
                next.visibility = View.VISIBLE
                next.animate()
                    .rotationY(0f)
                    .setDuration(190L)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction { binding.questionCard.isClickable = true }
                    .start()
            }
            .start()
    }

    private fun createWordChip(word: String): Chip {
        return Chip(requireContext()).apply {
            text = word
            isCheckable = false
            isClickable = true
            setEnsureMinTouchTargetSize(false)
            chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.word_chip_background)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.word_chip_text))
            chipStrokeWidth = resources.displayMetrics.density
            chipStrokeColor = ContextCompat.getColorStateList(requireContext(), R.color.word_chip_stroke)
            chipCornerRadius = 14f * resources.displayMetrics.density
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            chipStartPadding = 14f * resources.displayMetrics.density
            chipEndPadding = 14f * resources.displayMetrics.density
            textStartPadding = 0f
            textEndPadding = 0f
            minHeight = (44 * resources.displayMetrics.density).toInt()
            includeFontPadding = false
            setOnClickListener { moveWordChip(this) }
            setOnLongClickListener {
                val clip = ClipData.newPlainText("duelrush-word", word)
                startDragAndDrop(clip, View.DragShadowBuilder(this), this, 0)
                true
            }
        }
    }

    private fun enableDropZone(destination: ViewGroup) {
        destination.setOnDragListener { view, event ->
            val group = view as ViewGroup
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> event.localState is Chip
                DragEvent.ACTION_DRAG_ENTERED -> {
                    group.animate().alpha(0.72f).setDuration(100).start()
                    true
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    group.animate().alpha(1f).setDuration(100).start()
                    true
                }
                DragEvent.ACTION_DROP -> {
                    group.alpha = 1f
                    val source = event.localState as? Chip ?: return@setOnDragListener false
                    val word = source.text.toString()
                    (source.parent as? ViewGroup)?.removeView(source)
                    group.addView(createWordChip(word), dropIndex(group, event.x, event.y))
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    group.animate().alpha(1f).setDuration(100).start()
                    true
                }
                else -> true
            }
        }
    }

    private fun dropIndex(group: ViewGroup, x: Float, y: Float): Int {
        for (index in 0 until group.childCount) {
            val child = group.getChildAt(index)
            if (y < child.bottom && (y < child.top || x < child.left + child.width / 2f)) {
                return index
            }
        }
        return group.childCount
    }

    private fun moveWordChip(chip: Chip) {
        val isInWordBank = chip.parent === binding.containerWordBank
        val word = chip.text.toString()
        (chip.parent as? ViewGroup)?.removeView(chip)
        val destination = if (isInWordBank) {
            binding.containerSelectedWords
        } else {
            binding.containerWordBank
        }

        // ChipGroup can keep a clicked CompoundButton attached until the click
        // dispatch finishes. Adding that same instance immediately then crashes
        // with "child already has a parent", so move the word as a fresh chip.
        destination.addView(createWordChip(word))
    }

    private fun onOptionSelected(option: String) {
        if (selectedOption == option) {
            selectedOption = null
        } else {
            selectedOption = option
        }
    }

    fun getQuestion(): QuestionDetailedResponse {
        return question
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
            if (view is Chip) {
                words.add(view.text.toString())
            }
        }
        return words.joinToString(" ").trim().lowercase()
    }
}
