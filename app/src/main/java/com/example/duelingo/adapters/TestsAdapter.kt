package com.example.duelingo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.duelingo.R
import com.example.duelingo.dto.response.TestSummaryResponse

class TestsAdapter(
    private var tests: List<TestSummaryResponse>,
    private val onItemClick: (TestSummaryResponse) -> Unit
) : RecyclerView.Adapter<TestsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTestTitle: TextView = itemView.findViewById(R.id.tvTestTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_test, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val test = tests[position]
        holder.tvTestTitle.text = test.topic + " -- " + test.difficulty
        holder.itemView.setOnClickListener { onItemClick(test) }
    }

    override fun getItemCount() = tests.size

    fun updateData(newTests: List<TestSummaryResponse>) {
        tests = newTests
        notifyDataSetChanged()
    }
}