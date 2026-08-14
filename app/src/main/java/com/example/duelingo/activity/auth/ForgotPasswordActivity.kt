package com.example.duelingo.activity.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.duelingo.R
import com.example.duelingo.databinding.ActivityForgotPasswordBinding
import com.example.duelingo.dto.request.PasswordResetConfirmRequest
import com.example.duelingo.dto.request.PasswordResetRequest
import com.example.duelingo.network.ApiClient
import com.example.duelingo.utils.UserMessage
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding
    private var codeRequested = false
    private var loading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        codeRequested = savedInstanceState?.getBoolean(STATE_CODE_REQUESTED) == true
        renderStep()
        binding.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.requestCodeButton.setOnClickListener { requestCode() }
        binding.resetPasswordButton.setOnClickListener { resetPassword() }
        binding.confirmPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.resetPasswordButton.performClick()
                true
            } else false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_CODE_REQUESTED, codeRequested)
        super.onSaveInstanceState(outState)
    }

    private fun requestCode() {
        val email = binding.email.text.toString().trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.email.error = getString(R.string.error_email_invalid)
            binding.email.requestFocus()
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            runCatching { ApiClient.authService.requestPasswordReset(PasswordResetRequest(email)) }
                .onSuccess {
                    codeRequested = true
                    renderStep()
                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        R.string.password_reset_code_sent,
                        Toast.LENGTH_LONG
                    ).show()
                    binding.code.requestFocus()
                }
                .onFailure { showError(it) }
            setLoading(false)
        }
    }

    private fun resetPassword() {
        val email = binding.email.text.toString().trim()
        val code = binding.code.text.toString().trim()
        val password = binding.newPassword.text.toString()
        val confirmation = binding.confirmPassword.text.toString()
        when {
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> binding.email.error = getString(R.string.error_email_invalid)
            !code.matches(Regex("\\d{6}")) -> binding.code.error = getString(R.string.verification_code_required)
            password.length < 8 -> binding.newPassword.error = getString(R.string.error_password_length)
            !password.any(Char::isLetter) || !password.any(Char::isDigit) ->
                binding.newPassword.error = getString(R.string.error_password_format)
            password != confirmation -> binding.confirmPassword.error = getString(R.string.passwords_do_not_match)
            else -> {
                setLoading(true)
                lifecycleScope.launch {
                    runCatching {
                        ApiClient.authService.confirmPasswordReset(
                            PasswordResetConfirmRequest(email, code, password)
                        )
                    }.onSuccess {
                        Toast.makeText(
                            this@ForgotPasswordActivity,
                            R.string.password_reset_success,
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }.onFailure { showError(it) }
                    setLoading(false)
                }
            }
        }
    }

    private fun renderStep() {
        binding.resetForm.visibility = if (codeRequested) View.VISIBLE else View.GONE
        binding.requestCodeButton.setText(
            if (codeRequested) R.string.password_reset_resend else R.string.password_reset_send_code
        )
        binding.email.isEnabled = !codeRequested
    }

    private fun setLoading(value: Boolean) {
        loading = value
        binding.progress.visibility = if (value) View.VISIBLE else View.GONE
        binding.requestCodeButton.isEnabled = !value
        binding.resetPasswordButton.isEnabled = !value
    }

    private fun showError(error: Throwable) {
        Toast.makeText(this, UserMessage.from(this, error), Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val STATE_CODE_REQUESTED = "password_reset_code_requested"
    }
}
