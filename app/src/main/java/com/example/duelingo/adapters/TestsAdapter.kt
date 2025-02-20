package com.example.duelingo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.duelingo.R
import com.example.duelingo.dto.response.TestSummaryResponse

class TestsAdapter(
    private var tests: List<TestSummaryResponse>,
    private val onTestClick: (TestSummaryResponse) -> Unit
) : RecyclerView.Adapter<TestsAdapter.TestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_test, parent, false)
        return TestViewHolder(view)
    }

    override fun onBindViewHolder(holder: TestViewHolder, position: Int) {
        val test = tests[position]
        holder.bind(test)

        holder.itemView.setOnClickListener {
            onTestClick(test)
        }
    }

    override fun getItemCount() = tests.size

    fun updateData(newTests: List<TestSummaryResponse>) {
        this.tests = newTests
        notifyDataSetChanged()
    }

    class TestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTestTitle: TextView = itemView.findViewById(R.id.tvTestTitle)
        private val ivTestCheckmark: ImageView = itemView.findViewById(R.id.ivTestCheckmark)

        fun bind(test: TestSummaryResponse) {
            tvTestTitle.text = test.difficulty
            ivTestCheckmark.visibility = if (test.isCompleted) View.VISIBLE else View.GONE
        }
    }
}