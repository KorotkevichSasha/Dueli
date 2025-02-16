package com.example.duelingo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.duelingo.R

class OptionsAdapter(
    private val options: List<String>,
    private val onOptionSelected: (String) -> Unit
) : RecyclerView.Adapter<OptionsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvOption: TextView = view.findViewById(R.id.tvOption)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = options[position]
        holder.tvOption.text = option

        holder.itemView.setOnClickListener {
            onOptionSelected(option)
        }
    }

    override fun getItemCount(): Int = options.size
}