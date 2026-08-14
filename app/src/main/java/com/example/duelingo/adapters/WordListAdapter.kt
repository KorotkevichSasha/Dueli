package com.example.duelingo.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.duelingo.databinding.ItemDictionaryWordBinding
import com.example.duelingo.dto.response.WordProgressResponse

class WordListAdapter(private val onDelete: (WordProgressResponse) -> Unit) :
    ListAdapter<WordProgressResponse, WordListAdapter.WordViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = WordViewHolder(
        ItemDictionaryWordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) = holder.bind(getItem(position))

    inner class WordViewHolder(private val binding: ItemDictionaryWordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(word: WordProgressResponse) {
            binding.term.text = word.term
            binding.translation.text = word.translation
            binding.deleteButton.setOnClickListener { onDelete(word) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<WordProgressResponse>() {
        override fun areItemsTheSame(oldItem: WordProgressResponse, newItem: WordProgressResponse) = oldItem.wordId == newItem.wordId
        override fun areContentsTheSame(oldItem: WordProgressResponse, newItem: WordProgressResponse) = oldItem == newItem
    }
}
