package com.example.duelingo.adapters

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.duelingo.dto.response.QuestionDetailedResponse
import com.example.duelingo.fragment.QuestionFragment

class QuestionsPagerAdapter(
    activity: AppCompatActivity,
    private val questions: List<QuestionDetailedResponse>
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = questions.size

    override fun createFragment(position: Int): Fragment {
        val fragment = QuestionFragment()
        fragment.arguments = Bundle().apply {
            putParcelable("question", questions[position])
        }
        return fragment
    }
}
