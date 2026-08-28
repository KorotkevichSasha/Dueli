package com.example.duelingo.activity

import android.os.Bundle
import android.view.View
import com.example.duelingo.utils.AppToast as Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.duelingo.R
import com.example.duelingo.adapters.WordListAdapter
import com.example.duelingo.databinding.ActivityWordListBinding
import com.example.duelingo.dto.response.WordProgressResponse
import com.example.duelingo.network.ApiClient
import com.example.duelingo.utils.UserMessage
import kotlinx.coroutines.launch

class WordListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWordListBinding
    private val adapter = WordListAdapter(::confirmDelete)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWordListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.backButton.setOnClickListener { finish() }
        binding.wordsList.layoutManager = LinearLayoutManager(this)
        binding.wordsList.adapter = adapter
        binding.swipeRefresh.setColorSchemeResources(R.color.blue_primary)
        binding.swipeRefresh.setOnRefreshListener(::loadWords)
        loadWords()
    }

    private fun loadWords() {
        binding.progress.visibility = if (adapter.currentList.isEmpty()) View.VISIBLE else View.GONE
        lifecycleScope.launch {
            try {
                val words = ApiClient.wordService.getAllWords()
                adapter.submitList(words)
                showEmpty(words.isEmpty())
            } catch (error: Exception) {
                Toast.makeText(this@WordListActivity, UserMessage.from(this@WordListActivity, error), Toast.LENGTH_LONG).show()
            } finally {
                binding.progress.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun confirmDelete(word: WordProgressResponse) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_word_title)
            .setMessage(getString(R.string.delete_word_message, word.term))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete_word) { _, _ -> deleteWord(word) }
            .show()
    }

    private fun deleteWord(word: WordProgressResponse) {
        lifecycleScope.launch {
            try {
                ApiClient.wordService.deleteWord(word.wordId)
                val remaining = adapter.currentList.filterNot { it.wordId == word.wordId }
                adapter.submitList(remaining)
                showEmpty(remaining.isEmpty())
                Toast.makeText(this@WordListActivity, R.string.word_deleted, Toast.LENGTH_SHORT).show()
            } catch (error: Exception) {
                Toast.makeText(this@WordListActivity, UserMessage.from(this@WordListActivity, error), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showEmpty(empty: Boolean) {
        binding.emptyState.visibility = if (empty) View.VISIBLE else View.GONE
        binding.wordsList.visibility = if (empty) View.GONE else View.VISIBLE
    }
}
