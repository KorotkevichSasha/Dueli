package com.example.duelingo

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.duelingo.databinding.ActivityTestFragmentBinding

class TestFragment : Fragment(R.layout.activity_test_fragment) {

    private lateinit var binding: ActivityTestFragmentBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ActivityTestFragmentBinding.bind(view)

        // Логика для экрана "Тесты"
//        binding.textView.text = "Это экран Тестов"
    }
}