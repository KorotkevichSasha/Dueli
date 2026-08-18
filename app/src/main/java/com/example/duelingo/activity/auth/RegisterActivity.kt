package com.example.duelingo.activity.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.duelingo.activity.MenuActivity
import com.example.duelingo.activity.OnboardingActivity
import com.example.duelingo.BuildConfig
import com.example.duelingo.databinding.ActivityRegisterBinding
import com.example.duelingo.dto.request.SignUpRequest
import com.example.duelingo.dto.request.VerifyEmailRequest
import com.example.duelingo.dto.request.ResendVerificationRequest
import com.example.duelingo.dto.response.JwtAuthenticationResponse
import com.example.duelingo.network.ApiClient
import com.example.duelingo.network.AuthSessionManager
import com.example.duelingo.storage.TokenManager
import com.example.duelingo.utils.UserMessage
import kotlinx.coroutines.launch
import retrofit2.HttpException

class RegisterActivity : AppCompatActivity() {

    companion object {
        private const val STATE_VERIFICATION_EMAIL = "verification_email"
    }

    private lateinit var binding: ActivityRegisterBinding
    private var verificationEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        binding.legalNotice.setText(com.example.duelingo.R.string.registration_legal_notice_short)
        binding.legalNotice.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_POLICY_URL)))
        }

        listOf(binding.username, binding.email, binding.password, binding.confirmPassword).forEach { field ->
            field.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    binding.main.postDelayed({
                        val keepSubmitVisible = field === binding.password || field === binding.confirmPassword
                        val desiredBottom = if (keepSubmitVisible) {
                            binding.loginBtn.bottom - field.top + resources.displayMetrics.density.times(12).toInt()
                        } else {
                            field.height
                        }
                        field.requestRectangleOnScreen(
                            android.graphics.Rect(
                                0,
                                0,
                                field.width,
                                desiredBottom
                            ),
                            true
                        )
                    }, 250L)
                }
            }
        }

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        savedInstanceState?.getString(STATE_VERIFICATION_EMAIL)?.let {
            showVerificationStep(it, true)
        }

        binding.loginBtn.setOnClickListener {
            val email = binding.email.text.toString()
            val username = binding.username.text.toString()
            val password = binding.password.text.toString()
            val confirmPassword = binding.confirmPassword.text.toString()

            when {
                email.isEmpty() || password.isEmpty() || username.isEmpty() || confirmPassword.isEmpty() -> {
                    showToast(getString(com.example.duelingo.R.string.error_check_fields))
                }

                username.length !in 5..50 -> showToast(getString(com.example.duelingo.R.string.error_username_length))

                password != confirmPassword -> {
                    showToast(getString(com.example.duelingo.R.string.passwords_do_not_match))
                }

                password.length < 8 -> {
                    showToast(getString(com.example.duelingo.R.string.error_password_length))
                }

                !password.any(Char::isLetter) || !password.any(Char::isDigit) ->
                    showToast(getString(com.example.duelingo.R.string.error_password_format))

                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    showToast(getString(com.example.duelingo.R.string.error_email_invalid))
                }

                else -> {
                    val signUpRequest = SignUpRequest(username, email, password)
                    registerUser(signUpRequest)
                }
            }
        }


        binding.verifyEmailButton.setOnClickListener { verifyEmail() }
        binding.resendCodeButton.setOnClickListener { resendVerificationCode() }
        binding.confirmPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.loginBtn.performClick()
                true
            } else false
        }
        binding.verificationCode.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                verifyEmail()
                true
            } else false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        verificationEmail?.let { outState.putString(STATE_VERIFICATION_EMAIL, it) }
    }

    private fun registerUser(signUpRequest: SignUpRequest) {
        if (!binding.loginBtn.isEnabled) return
        binding.loginBtn.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = ApiClient.authService.signUp(signUpRequest)
                showVerificationStep(response.email, response.emailSent)
            } catch (e: HttpException) {
                handleServerError(e)
            } catch (e: Exception) {
                showToast(UserMessage.from(this@RegisterActivity, e))
            } finally {
                if (binding.verificationContainer.visibility != View.VISIBLE) {
                    binding.loginBtn.isEnabled = true
                }
            }
        }
    }

    private fun showVerificationStep(email: String, emailSent: Boolean) {
        verificationEmail = email
        binding.imageView3.visibility = View.GONE
        binding.registerSubtitle.visibility = View.GONE
        binding.username.visibility = View.GONE
        binding.email.visibility = View.GONE
        binding.password.visibility = View.GONE
        binding.confirmPassword.visibility = View.GONE
        binding.legalNotice.visibility = View.GONE
        binding.loginBtn.visibility = View.GONE
        binding.verificationContainer.visibility = View.VISIBLE
        binding.registerTitle.setText(com.example.duelingo.R.string.verify_email_title)
        binding.verificationInfo.text = getString(
            if (emailSent) com.example.duelingo.R.string.verification_email_sent
            else com.example.duelingo.R.string.verification_email_not_sent,
            email
        )
        binding.verificationCode.requestFocus()
    }

    private fun verifyEmail() {
        val email = verificationEmail ?: return
        val code = binding.verificationCode.text.toString().trim()
        if (code.length != 6) {
            showToast(getString(com.example.duelingo.R.string.verification_code_required))
            return
        }
        binding.verifyEmailButton.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = ApiClient.authService.verifyEmail(VerifyEmailRequest(email, code))
                saveTokens(response)
                showToast(getString(com.example.duelingo.R.string.verification_success))
                startActivity(Intent(this@RegisterActivity, OnboardingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            } catch (error: HttpException) {
                handleServerError(error)
            } catch (error: Exception) {
                showToast(UserMessage.from(this@RegisterActivity, error))
            } finally {
                binding.verifyEmailButton.isEnabled = true
            }
        }
    }

    private fun resendVerificationCode() {
        val email = verificationEmail ?: return
        binding.resendCodeButton.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = ApiClient.authService.resendVerification(ResendVerificationRequest(email))
                showVerificationStep(email, response.emailSent)
                if (response.emailSent) showToast(getString(com.example.duelingo.R.string.verification_resent))
            } catch (error: HttpException) {
                handleServerError(error)
            } catch (error: Exception) {
                showToast(UserMessage.from(this@RegisterActivity, error))
            } finally {
                binding.resendCodeButton.isEnabled = true
            }
        }
    }

    private fun handleServerError(e: HttpException) {
        showToast(UserMessage.from(this, e))
    }

    private fun saveTokens(response: JwtAuthenticationResponse) {
        val tokenManager = TokenManager(this)
        tokenManager.saveTokens(response.accessToken, response.refreshToken, newSession = true)
        AuthSessionManager.onAuthenticated()
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }
}
