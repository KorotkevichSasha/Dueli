package com.example.duelingo

import android.animation.Animator
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import com.airbnb.lottie.LottieAnimationView
import com.example.duelingo.databinding.ActivityMenuBinding

class MenuFragment : Fragment(R.layout.activity_menu) {

    private lateinit var binding: ActivityMenuBinding
    private lateinit var navController: NavController
    private var currentAnimationView: LottieAnimationView? = null
    private var currentIcon: ImageView? = null
    private var currentText: TextView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ActivityMenuBinding.bind(view)

        changeColorAndIcon(binding.mainIcon, binding.mainTest, R.drawable.swo)
        currentIcon = binding.mainIcon
        currentText = binding.mainTest

        binding.test.setOnClickListener {
            resetAll()
            changeColorAndIcon(binding.testIcon, binding.testTest, R.drawable.grad)
            playAnimation(binding.testAnimation, binding.testIcon, binding.testTest, "graAnim.json")
            navController.navigate(R.id.testFragment)
        }

        binding.cup.setOnClickListener {
            resetAll()
            changeColorAndIcon(binding.cupIcon, binding.cupTest, R.drawable.tro)
            playAnimation(binding.cupAnimation, binding.cupIcon, binding.cupTest, "cupAnim.json")
            navController.navigate(R.id.cupFragment)
        }

        binding.profile.setOnClickListener {
            resetAll()
            changeColorAndIcon(binding.profileIcon, binding.profileTest, R.drawable.prof)
            playAnimation(binding.profileAnimation, binding.profileIcon, binding.profileTest, "profAnim.json")
            navController.navigate(R.id.profileFragment)
        }
    }

    private fun changeColorAndIcon(icon: ImageView, text: TextView, iconResId: Int) {
        text.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_primary))
        icon.setImageResource(iconResId)
    }

    private fun playAnimation(animationView: LottieAnimationView, icon: ImageView, text: TextView, animationFile: String) {
        currentAnimationView = animationView
        currentIcon = icon
        currentText = text

        icon.visibility = View.GONE
        animationView.visibility = View.VISIBLE
        animationView.setAnimation(animationFile)
        animationView.playAnimation()

        animationView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                icon.visibility = View.VISIBLE
                animationView.visibility = View.GONE
            }
            override fun onAnimationCancel(animation: Animator) {
                icon.visibility = View.VISIBLE
                animationView.visibility = View.GONE
            }
            override fun onAnimationRepeat(animation: Animator) {}
        })
    }

    private fun resetAll() {
        binding.testTest.setTextColor(Color.parseColor("#7A7A7B"))
        binding.mainTest.setTextColor(Color.parseColor("#7A7A7B"))
        binding.cupTest.setTextColor(Color.parseColor("#7A7A7B"))
        binding.profileTest.setTextColor(Color.parseColor("#7A7A7B"))
        binding.testIcon.setImageResource(R.drawable.graduation24)
        binding.mainIcon.setImageResource(R.drawable.swords24)
        binding.cupIcon.setImageResource(R.drawable.trophy24)
        binding.profileIcon.setImageResource(R.drawable.profile24)
    }
}