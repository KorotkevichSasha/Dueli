package com.example.duelingo

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.duelingo.databinding.ActivityFragmentCupBinding

class CupFragment : Fragment(R.layout.activity_fragment_cup) {

    private lateinit var binding: ActivityFragmentCupBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ActivityFragmentCupBinding.bind(view)

        // Логика для экрана "Тесты"
//        binding.textView.text = "Это экран Тестов"
    }
}