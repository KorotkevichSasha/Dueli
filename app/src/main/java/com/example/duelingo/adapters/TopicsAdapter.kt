package com.example.duelingo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
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

        val isTopicCompleted = difficultyStatus.values.all { it }

        holder.bind(topic, isTopicCompleted)

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
        private val ivTopicCheckmark: ImageView = itemView.findViewById(R.id.ivTopicCheckmark)

        fun bind(topic: String, isTopicCompleted: Boolean) {
            tvTopicTitle.text = topic
            ivTopicCheckmark.visibility = if (isTopicCompleted) View.VISIBLE else View.GONE
        }
    }
}