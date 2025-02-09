package com.example.duelingo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.duelingo.R

class TopicsAdapter(
    private var topics: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<TopicsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTopicTitle: TextView = itemView.findViewById(R.id.tvTopicTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_topic, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val topic = topics[position]
        holder.tvTopicTitle.text = topic
        holder.itemView.setOnClickListener { onItemClick(topic) }
    }

    override fun getItemCount() = topics.size

    fun updateData(newTopics: List<String>) {
        topics = newTopics
        notifyDataSetChanged()
    }
}