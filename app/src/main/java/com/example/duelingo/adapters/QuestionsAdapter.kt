import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.example.duelingo.databinding.ItemQuestionFillInChoiceBinding
import com.example.duelingo.databinding.ItemQuestionFillInInputBinding
import com.example.duelingo.databinding.ItemQuestionSentenceConstructionBinding
import com.example.duelingo.dto.response.QuestionDetailedResponse

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

    inner class FillInChoiceViewHolder(
        private val binding: ItemQuestionFillInChoiceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(question: QuestionDetailedResponse, position: Int) {
            binding.tvQuestionText.text = question.questionText
            binding.radioGroupOptions.removeAllViews()

            question.options.forEachIndexed { index, option ->
                RadioButton(binding.root.context).apply {
                    text = option
                    id = index
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) userAnswers[position] = option
                    }
                }.let { binding.radioGroupOptions.addView(it) }
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
            binding.tvSentenceParts.text = "Слова для составления: ${question.options.joinToString()}"

            // Базовая реализация выбора слов (можно улучшить)
            binding.containerWordBank.removeAllViews()
            question.options.shuffled().forEach { word ->
                Button(binding.root.context).apply {
                    text = word
                    setOnClickListener {
                        addWordToSentence(word)
                        userAnswers[position] = getCurrentSentence()
                    }
                }.let { binding.containerWordBank.addView(it) }
            }
        }

        private fun addWordToSentence(word: String) {
            TextView(binding.root.context).apply {
                text = word
                setPadding(8, 4, 8, 4)
            }.let { binding.containerSelectedWords.addView(it) }
        }

        private fun getCurrentSentence(): String {
            val words = mutableListOf<String>()
            for (i in 0 until binding.containerSelectedWords.childCount) {
                (binding.containerSelectedWords.getChildAt(i) as? TextView)?.text?.toString()?.let {
                    words.add(it)
                }
            }
            return words.joinToString(" ")
        }
    }
}