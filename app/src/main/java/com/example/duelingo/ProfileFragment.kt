package com.example.duelingo

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.duelingo.databinding.ActivityFragmentProfileBinding

class ProfileFragment : Fragment(R.layout.activity_fragment_profile) {
    private lateinit var binding: ActivityFragmentProfileBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ActivityFragmentProfileBinding.bind(view)

        // Логика для экрана "Тесты"
//        binding.textView.text = "Это экран Тестов"
    }
}