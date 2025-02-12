package com.example.duelingo.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.duelingo.dto.response.QuestionDetailedResponse
import com.example.duelingo.fragment.QuestionFragment

class QuestionsPagerAdapter(
    fragment: FragmentActivity,
    private val questions: List<QuestionDetailedResponse>
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = questions.size

    override fun createFragment(position: Int): Fragment {
        return QuestionFragment.newInstance(questions[position], position)
    }
}