package com.example.duelingo.activity

import android.animation.Animator
import android.content.ContentResolver
import android.content.Intent
import android.graphics.BitmapFactory
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
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.example.duelingo.R
import com.example.duelingo.activity.auth.LoginActivity
import com.example.duelingo.databinding.ActivityProfileBinding
import com.example.duelingo.dto.response.UserProfileResponse
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream



class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private var currentAnimationView: LottieAnimationView? = null
    private var currentIcon: ImageView? = null
    private var currentText: TextView? = null

    private val tokenManager by lazy { TokenManager(this) }
    private val sharedPreferences by lazy { getSharedPreferences("user_prefs", MODE_PRIVATE) }

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.profileIcon.setColorFilter(Color.parseColor("#FF00A5FE"))
        binding.profileTest.setTextColor(Color.parseColor("#FF00A5FE"))

        binding.logout.setOnClickListener {
            logout()
        }
        binding.profileImage.setOnClickListener {
            getContent.launch("image/*")
        }
        loadProfile()

        binding.tests.setOnClickListener {
            resetAll()
            startActivity(Intent(this@ProfileActivity, TopicsActivity::class.java))
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

    private fun loadAvatar(userId: String) {
        val accessToken = tokenManager.getAccessToken() ?: return
        val tokenWithBearer = "Bearer $accessToken"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val savedETag = sharedPreferences.getString("avatar_etag", null)
                val response = ApiClient.profileService.getAvatar(tokenWithBearer, savedETag, userId)

                if (response.code() == 304) {
                    Log.d("Avatar", "Not modified, using cached version.")
                    return@launch
                }

                response.body()?.let { body ->
                    val bitmap = BitmapFactory.decodeStream(body.byteStream())
                    withContext(Dispatchers.Main) {
                        binding.profileImage.setImageBitmap(bitmap)
                    }

                    response.headers()["ETag"]?.let { newETag ->
                        sharedPreferences.edit().putString("avatar_etag", newETag).apply()
                    }
                }
            } catch (e: Exception) {
                Log.e("Avatar", "Error loading avatar: ${e.message}")
            }
        }
    }

    private fun updateUI(response: UserProfileResponse) {
        binding.playerName.text = response.username
        binding.playerEmail.text = response.email
        binding.pointCount.text = "Очки: ${response.points}"

        loadAvatar(response.id)
    }

    private fun uploadImage(uri: Uri) {
        val mimeType = getMimeType(uri) ?: "image/*"
        println("MIME type: $mimeType")

        val accessToken = tokenManager.getAccessToken() ?: run {
            showToast("Access token is missing.")
            return
        }

        val tokenWithBearer = "Bearer $accessToken"

        val file = createTempFileFromUri(uri) ?: run {
            showToast("Failed to create file from URI")
            return
        }

        if (file.length() > 2 * 1024 * 1024) {
            showToast("File is too large")
            file.delete()
            return
        }

        val sanitizedFileName = file.name.replace(Regex("[^a-zA-Z0-9._-]"), "_")

        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", sanitizedFileName, requestFile)

        lifecycleScope.launch {
            try {
                val response = ApiClient.profileService.uploadAvatar(tokenWithBearer, body)
                withContext(Dispatchers.Main) {
                    updateUI(response)
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("UploadError", "HTTP ${e.code()}: $errorBody")
                showToast("Error uploading image: ${e.message()}")
            } catch (e: Exception) {
                Log.e("UploadError", "Unexpected error: ${e.message}")
                showToast("Unexpected error: ${e.message}")
            } finally {
                file.delete()
            }
        }
    }

    private fun createTempFileFromUri(uri: Uri): File? {
        return try {
            val contentResolver: ContentResolver = applicationContext.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)

            if (inputStream != null) {
                val file = File.createTempFile("temp_avatar", ".jpg", cacheDir)
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                inputStream.close()
                file
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("FileCreation", "Error creating temp file: ${e.message}")
            null
        }
    }

    private fun getMimeType(uri: Uri): String? {
        return applicationContext.contentResolver.getType(uri)
    }
    private fun logout() {
        tokenManager.clearTokens()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    private fun loadProfile() {
        val accessToken = tokenManager.getAccessToken() ?: run {
            showToast("Access token is missing.")
            return
        }

        val tokenWithBearer = "Bearer $accessToken"

        lifecycleScope.launch {
            try {
                val response = ApiClient.profileService.getProfile(tokenWithBearer)
                withContext(Dispatchers.Main) {
                    updateUI(response)
                }
            } catch (e: Exception) {
                Log.e("ProfileError", "Error loading profile: ${e.message}")
                showToast("Error loading profile. Please try again.")
            }
        }
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