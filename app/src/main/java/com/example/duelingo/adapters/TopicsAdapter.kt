package com.example.duelingo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.duelingo.R

class TopicsAdapter(
    private var topics: List<String>,
    private val onTopicClick: (String) -> Unit,
    private val onRandomTestClick: () -> Unit
) : RecyclerView.Adapter<TopicsAdapter.TopicViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_topic, parent, false)
        return TopicViewHolder(view)
    }

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        val topic = topics[position]
        holder.bind(topic)

        holder.itemView.setOnClickListener {
            if (topic == "Random Test") {
                onRandomTestClick()
            } else {
                onTopicClick(topic)
            }
        }
    }

    override fun getItemCount() = topics.size

    fun updateData(newTopics: List<String>) {
        topics = newTopics
        notifyDataSetChanged()
    }
    class TopicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(topic: String) {
            itemView.findViewById<TextView>(R.id.tvTopicTitle).text = topic
        }
    }
}