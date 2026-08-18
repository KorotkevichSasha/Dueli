package com.example.duelingo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.example.duelingo.R

class TopicsAdapter(
    private var topics: List<String>,
    private val onTopicClick: (String) -> Unit,
    private val onRandomTestClick: () -> Unit
) : RecyclerView.Adapter<TopicsAdapter.TopicViewHolder>() {

    private var completionStatus: Map<String, Map<String, Boolean>> = emptyMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_topic, parent, false)
        return TopicViewHolder(view)
    }

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        val topic = topics[position]
        val difficultyStatus = completionStatus[topic] ?: emptyMap()

        val isEasyCompleted = difficultyStatus["EASY"] ?: false
        val isMediumCompleted = difficultyStatus["MEDIUM"] ?: false
        val isHardCompleted = difficultyStatus["HARD"] ?: false

        holder.bind(topic, isEasyCompleted, isMediumCompleted, isHardCompleted)

        holder.itemView.setOnClickListener {
            if (topic == "Random Test") {
                onRandomTestClick()
            } else {
                onTopicClick(topic)
            }
        }
    }

    override fun getItemCount() = topics.size

    fun updateData(newTopics: List<String>, newCompletionStatus: Map<String, Map<String, Boolean>>) {
        this.topics = newTopics
        this.completionStatus = newCompletionStatus
        notifyDataSetChanged()
    }
    class TopicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTopicTitle: TextView = itemView.findViewById(R.id.tvTopicTitle)
        private val ivEasyCheckmark: ImageView = itemView.findViewById(R.id.ivEasyCheckmark)
        private val ivMediumCheckmark: ImageView = itemView.findViewById(R.id.ivMediumCheckmark)
        private val ivHardCheckmark: ImageView = itemView.findViewById(R.id.ivHardCheckmark)
        private val description: TextView = itemView.findViewById(R.id.tvTopicDescription)
        private val progressText: TextView = itemView.findViewById(R.id.tvTopicProgress)
        private val progressBar: LinearProgressIndicator = itemView.findViewById(R.id.topicProgressBar)

        fun bind(topic: String, isEasyCompleted: Boolean, isMediumCompleted: Boolean, isHardCompleted: Boolean) {
            tvTopicTitle.text = topic
            val random = topic == "Random Test"
            val completed = listOf(isEasyCompleted, isMediumCompleted, isHardCompleted).count { it }
            description.text = topicDescription(itemView.context, topic)
            progressText.text = if (random) {
                itemView.context.getString(R.string.test_ten_questions)
            } else {
                itemView.context.getString(R.string.topic_progress_format, completed, 3)
            }
            progressBar.visibility = if (random) View.INVISIBLE else View.VISIBLE
            progressBar.setProgressCompat(completed, false)
            listOf(
                ivEasyCheckmark to isEasyCompleted,
                ivMediumCheckmark to isMediumCompleted,
                ivHardCheckmark to isHardCompleted
            ).forEach { (icon, done) ->
                icon.visibility = if (random) View.GONE else View.VISIBLE
                icon.alpha = if (done) 1f else 0.2f
            }
        }

        private fun topicDescription(context: android.content.Context, topic: String): String {
            val resource = when (topic) {
                "Random Test" -> R.string.topic_random_description
                "Present Simple" -> R.string.topic_present_simple_description
                "Past Simple" -> R.string.topic_past_simple_description
                "Future Simple" -> R.string.topic_future_simple_description
                "Present Continuous" -> R.string.topic_present_continuous_description
                "Present Perfect" -> R.string.topic_present_perfect_description
                "Modal Verbs" -> R.string.topic_modals_description
                "Articles" -> R.string.topic_articles_description
                "Prepositions" -> R.string.topic_prepositions_description
                "Passive Voice" -> R.string.topic_passive_description
                "Conditional Sentences" -> R.string.topic_conditionals_description
                "Reported Speech" -> R.string.topic_reported_description
                "Comparatives and Superlatives" -> R.string.topic_comparatives_description
                "Adjectives and Adverbs" -> R.string.topic_adjectives_description
                "Relative Clauses" -> R.string.topic_relative_description
                else -> R.string.topic_generic_description
            }
            return context.getString(resource)
        }
    }
}
