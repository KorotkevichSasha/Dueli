package com.example.duelingo.activity
import android.animation.Animator
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.example.duelingo.R
import com.example.duelingo.activity.auth.LoginActivity
import com.example.duelingo.adapters.FriendRequestsAdapter
import com.example.duelingo.databinding.ActivityProfileBinding
import com.example.duelingo.dto.response.UserProfileResponse
import com.example.duelingo.manager.AvatarManager
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID


class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private var currentAnimationView: LottieAnimationView? = null
    private var currentIcon: ImageView? = null
    private var currentText: TextView? = null
    private val tokenManager by lazy { TokenManager(this) }
    private lateinit var avatarManager: AvatarManager
    private val sharedPreferences by lazy { getSharedPreferences("user_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.profileIcon.setColorFilter(Color.parseColor("#FF00A5FE"))
        binding.profileTest.setTextColor(Color.parseColor("#FF00A5FE"))

        avatarManager = AvatarManager(this, tokenManager, sharedPreferences)
        binding.profileImage.setOnClickListener { getContent.launch("image/*") }
        loadProfile()

        binding.logout.setOnClickListener { logout() }

        binding.acceptFriends.setOnClickListener { showFriendRequestsDialog() }
        binding.achievementsButton.setOnClickListener{ startActivity(Intent(this@ProfileActivity, AchievementActivity::class.java)) }

        binding.tests.setOnClickListener {
            resetAll()
            startActivity(Intent(this@ProfileActivity, LearningActivity::class.java))
            changeColorAndIcon(binding.testIcon, binding.testTest, R.drawable.grad)
            playAnimation(binding.testAnimation, binding.testIcon, binding.testTest, "graAnim.json")
        }
        binding.duel.setOnClickListener {
            resetAll()
            startActivity(Intent(this@ProfileActivity, MenuActivity::class.java))
            changeColorAndIcon(binding.mainIcon, binding.mainTest, R.drawable.swo)
            playAnimation(binding.duelAnimation, binding.mainIcon, binding.mainTest, "swordAnim.json")
        }
        binding.leaderboard.setOnClickListener {
            resetAll()
            startActivity(Intent(this@ProfileActivity, RankActivity::class.java))
            changeColorAndIcon(binding.cupIcon, binding.cupTest, R.drawable.tro)
            playAnimation(binding.cupAnimation, binding.cupIcon, binding.cupTest, "cupAnim.json")
        }
        binding.profile.setOnClickListener {}
    }

    private fun showFriendRequestsDialog() {
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(R.layout.dialog_add_friend)

        val recyclerView = dialog.findViewById<RecyclerView>(R.id.requestsRecyclerView)

        val adapter = FriendRequestsAdapter(
            avatarManager = avatarManager,
            onAccept = { requestId -> updateRequestStatus(requestId, "accept", recyclerView) },
            onReject = { requestId -> updateRequestStatus(requestId, "reject", recyclerView) }
        )

        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.adapter = adapter

        loadFriendRequests(adapter)
        dialog.show()
    }
    private fun updateRequestStatus(requestId: UUID, action: String, recyclerView: RecyclerView?) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.relationshipService.updateRelationshipStatus(
                    "Bearer ${tokenManager.getAccessToken()}",
                    requestId,
                    action
                )

                if (response.isSuccessful) {
                    showToast("Request updated")
                    val adapter = (recyclerView?.adapter as? FriendRequestsAdapter)
                    adapter?.let { loadFriendRequests(it) }
                } else {
                    val errorBody = response.errorBody()?.string()
                    showToast("Error: ${errorBody ?: "Unknown error"}")
                }
            } catch (e: Exception) {
                showToast("Error updating request: ${e.message}")
            }
        }
    }
    private fun loadFriendRequests(adapter: FriendRequestsAdapter) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.relationshipService.getIncomingRequests(
                    "Bearer ${tokenManager.getAccessToken()}"
                )

                if (response.isSuccessful) {
                    response.body()?.let { requests ->
                        adapter.submitList(requests)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    showToast("Error: ${errorBody ?: "Unknown error"}")
                }
            } catch (e: Exception) {
                showToast("Error loading requests: ${e.message}")
            }
        }
    }


    private fun updateUI(response: UserProfileResponse) {
        binding.playerName.text = response.username
        binding.playerEmail.text = response.email
        binding.pointCount.text = "Очки: ${response.points}"

        avatarManager.loadAvatar(response.id, binding.profileImage)
    }
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            avatarManager.uploadImage(it,
                onSuccess = { response ->
                    updateUI(response)
                },
                onError = { message ->
                    showToast(message)
                }
            )
        }
    }

    private fun loadProfile() {
        val accessToken = tokenManager.getAccessToken() ?: run {
            showToast("Access token is missing.")
            return
        }

        val tokenWithBearer = "Bearer $accessToken"

        lifecycleScope.launch {
            try {
                val response = ApiClient.userService.getProfile(tokenWithBearer)
                withContext(Dispatchers.Main) {
                    updateUI(response)
                }
            } catch (e: Exception) {
                Log.e("ProfileError", "Error loading profile: ${e.message}")
                showToast("Error loading profile. Please try again.")
            }
        }
    }
    private fun logout() {
        tokenManager.clearTokens()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    private fun changeColorAndIcon(icon: ImageView, text: TextView, iconRes: Int) {
        text.setTextColor(ContextCompat.getColor(this, R.color.blue_primary))
        icon.setColorFilter(ContextCompat.getColor(this, R.color.blue_primary))
        icon.setImageResource(iconRes)
    }
    private fun playAnimation(animationView: LottieAnimationView, icon: ImageView, text: TextView, animationFile: String) {
        currentAnimationView?.apply {
            cancelAnimation()
            visibility = View.GONE
        }

        currentIcon?.setColorFilter(Color.parseColor("#7A7A7B"))
        currentText?.setTextColor(Color.parseColor("#7A7A7B"))
        currentIcon?.visibility = View.VISIBLE

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

        binding.mainIcon.setColorFilter(Color.parseColor("#7A7A7B"))

        binding.testIcon.setImageResource(R.drawable.graduation24)
        binding.mainIcon.setImageResource(R.drawable.swords24)
        binding.cupIcon.setImageResource(R.drawable.trophy24)
        binding.profileIcon.setImageResource(R.drawable.profile24)
    }
}