import android.content.ClipData
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import com.example.duelingo.R
import com.example.duelingo.databinding.ItemQuestionFillInChoiceBinding
import com.example.duelingo.databinding.ItemQuestionFillInInputBinding
import com.example.duelingo.databinding.ItemQuestionSentenceConstructionBinding
import com.example.duelingo.dto.response.QuestionDetailedResponse
import com.google.android.material.chip.Chip

class QuestionsAdapter(
    private var questions: List<QuestionDetailedResponse>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_FILL_IN_CHOICE = 0
        private const val TYPE_FILL_IN_INPUT = 1
        private const val TYPE_SENTENCE_CONSTRUCTION = 2
    }

    private val userAnswers = mutableMapOf<Int, String>()

    override fun getItemViewType(position: Int): Int {
        return when (questions[position].type) {
            "FILL_IN_CHOICE" -> TYPE_FILL_IN_CHOICE
            "FILL_IN_INPUT" -> TYPE_FILL_IN_INPUT
            "SENTENCE_CONSTRUCTION" -> TYPE_SENTENCE_CONSTRUCTION
            else -> throw IllegalArgumentException("Unknown question type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_FILL_IN_CHOICE -> {
                val binding = ItemQuestionFillInChoiceBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                FillInChoiceViewHolder(binding)
            }
            TYPE_FILL_IN_INPUT -> {
                val binding = ItemQuestionFillInInputBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                FillInInputViewHolder(binding)
            }
            TYPE_SENTENCE_CONSTRUCTION -> {
                val binding = ItemQuestionSentenceConstructionBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SentenceConstructionViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val question = questions[position]
        when (holder) {
            is FillInChoiceViewHolder -> holder.bind(question, position)
            is FillInInputViewHolder -> holder.bind(question, position)
            is SentenceConstructionViewHolder -> holder.bind(question, position)
        }
    }

    override fun getItemCount() = questions.size

    fun updateData(newQuestions: List<QuestionDetailedResponse>) {
        questions = newQuestions
        notifyDataSetChanged()
    }

    fun getCorrectAnswers(): Map<Int, List<String>> {
        return questions.withIndex().associate { (index, question) ->
            index to question.correctAnswers
        }
    }

    fun getUserAnswers(): Map<Int, String> {
        return userAnswers
    }

    inner class FillInChoiceViewHolder(
        private val binding: ItemQuestionFillInChoiceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(question: QuestionDetailedResponse, position: Int) {
            binding.tvQuestionText.text = question.questionText
            binding.rvOptions.removeAllViews()

            question.options.forEachIndexed { index, option ->
                RadioButton(binding.root.context).apply {
                    text = option
                    id = index
                    setTextColor(ContextCompat.getColor(binding.root.context, R.color.word_chip_text))
                    textSize = 16f
                    minHeight = (48 * resources.displayMetrics.density).toInt()
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) userAnswers[position] = option
                    }
                }.let { binding.rvOptions.addView(it) }
            }
        }
    }

    inner class FillInInputViewHolder(
        private val binding: ItemQuestionFillInInputBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(question: QuestionDetailedResponse, position: Int) {
            binding.tvQuestionText.text = question.questionText
            binding.etUserAnswer.doAfterTextChanged {
                userAnswers[position] = it?.toString() ?: ""
            }
        }
    }

    inner class SentenceConstructionViewHolder(
        private val binding: ItemQuestionSentenceConstructionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(question: QuestionDetailedResponse, position: Int) {
            binding.tvQuestionText.text = question.questionText

            binding.containerWordBank.removeAllViews()
            binding.containerSelectedWords.removeAllViews()
            enableDropZone(binding.containerWordBank, position)
            enableDropZone(binding.containerSelectedWords, position)

            question.options.shuffled().forEach { word ->
                binding.containerWordBank.addView(createWordChip(word, position))
            }
        }

        private fun createWordChip(word: String, position: Int): Chip =
            Chip(binding.root.context).apply {
                text = word
                isCheckable = false
                isClickable = true
                setEnsureMinTouchTargetSize(false)
                chipBackgroundColor = ContextCompat.getColorStateList(binding.root.context, R.color.word_chip_background)
                setTextColor(ContextCompat.getColor(binding.root.context, R.color.word_chip_text))
                chipStrokeColor = ContextCompat.getColorStateList(binding.root.context, R.color.word_chip_stroke)
                chipStrokeWidth = resources.displayMetrics.density
                minHeight = (44 * resources.displayMetrics.density).toInt()
                gravity = android.view.Gravity.CENTER
                includeFontPadding = false
                setOnClickListener {
                    moveWordChip(this, position)
                    userAnswers[position] = getCurrentSentence()
                }
                setOnLongClickListener {
                    startDragAndDrop(ClipData.newPlainText("duelrush-word", word), View.DragShadowBuilder(this), this, 0)
                    true
                }
            }

        private fun enableDropZone(destination: ViewGroup, position: Int) {
            destination.setOnDragListener { view, event ->
                val group = view as ViewGroup
                when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> event.localState is Chip
                    DragEvent.ACTION_DRAG_ENTERED -> { group.alpha = 0.72f; true }
                    DragEvent.ACTION_DRAG_EXITED -> { group.alpha = 1f; true }
                    DragEvent.ACTION_DROP -> {
                        group.alpha = 1f
                        val source = event.localState as? Chip ?: return@setOnDragListener false
                        val word = source.text.toString()
                        (source.parent as? ViewGroup)?.removeView(source)
                        group.addView(createWordChip(word, position), dropIndex(group, event.x, event.y))
                        userAnswers[position] = getCurrentSentence()
                        true
                    }
                    DragEvent.ACTION_DRAG_ENDED -> { group.alpha = 1f; true }
                    else -> true
                }
            }
        }

        private fun dropIndex(group: ViewGroup, x: Float, y: Float): Int {
            for (index in 0 until group.childCount) {
                val child = group.getChildAt(index)
                if (y < child.bottom && (y < child.top || x < child.left + child.width / 2f)) return index
            }
            return group.childCount
        }

        private fun moveWordChip(chip: Chip, position: Int) {
            val isInWordBank = chip.parent === binding.containerWordBank
            val word = chip.text.toString()
            (chip.parent as? ViewGroup)?.removeView(chip)
            val destination = if (isInWordBank) {
                binding.containerSelectedWords
            } else {
                binding.containerWordBank
            }
            destination.addView(createWordChip(word, position))
        }

        private fun getCurrentSentence(): String {
            val words = mutableListOf<String>()
            for (i in 0 until binding.containerSelectedWords.childCount) {
                (binding.containerSelectedWords.getChildAt(i) as? Chip)?.text?.toString()?.let {
                    words.add(it)
                }
            }
            return words.joinToString(" ")
        }
    }

}
