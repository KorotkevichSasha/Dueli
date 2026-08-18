package com.example.duelingo.utils

import android.content.Context
import com.example.duelingo.R
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Converts internal/network failures into short messages suitable for UI. */
object UserMessage {
    fun from(context: Context, error: Throwable): String {
        return when (error) {
            is HttpException -> fromHttp(context, error)
            is UnknownHostException, is ConnectException -> context.getString(R.string.error_server_unavailable)
            is SocketTimeoutException -> context.getString(R.string.error_timeout)
            is IOException -> context.getString(R.string.error_network)
            else -> context.getString(R.string.error_generic)
        }
    }

    fun fromServerText(context: Context, raw: String?): String {
        if (raw.isNullOrBlank()) return context.getString(R.string.error_generic)
        val message = runCatching { JSONObject(raw).optString("message") }
            .getOrNull().takeUnless { it.isNullOrBlank() } ?: raw
        val normalized = message.lowercase()
        return when {
            "username must be between" in normalized -> context.getString(R.string.error_username_length)
            "username cannot be empty" in normalized -> context.getString(R.string.error_username_required)
            "password must be between" in normalized -> context.getString(R.string.error_password_length)
            "password must contain at least one letter" in normalized -> context.getString(R.string.error_password_format)
            "password cannot be empty" in normalized -> context.getString(R.string.error_password_required)
            "email address must be in the format" in normalized -> context.getString(R.string.error_email_invalid)
            "email address cannot be empty" in normalized -> context.getString(R.string.error_email_required)
            "user with username" in normalized && "already exists" in normalized -> context.getString(R.string.error_username_taken)
            "user with email" in normalized && "already exists" in normalized -> context.getString(R.string.error_email_taken)
            "invalid username or password" in normalized -> context.getString(R.string.error_invalid_credentials)
            "email address is not verified" in normalized -> context.getString(R.string.error_email_not_verified)
            "too many authentication attempts" in normalized -> context.getString(R.string.error_too_many_attempts)
            "verification code" in normalized && "6 digits" in normalized -> context.getString(R.string.verification_code_required)
            "no pending email verification" in normalized -> context.getString(R.string.verification_no_pending)
            "requested after one minute" in normalized -> context.getString(R.string.verification_wait_before_resend)
            "verification code is invalid or expired" in normalized -> context.getString(R.string.verification_invalid_code)
            "too many verification attempts" in normalized -> context.getString(R.string.verification_too_many_attempts)
            "email address is already verified" in normalized -> context.getString(R.string.verification_already_complete)
            "reset code" in normalized && "six digits" in normalized -> context.getString(R.string.password_reset_invalid_code)
            "reset code is invalid or expired" in normalized -> context.getString(R.string.password_reset_invalid_code)
            "too many reset attempts" in normalized -> context.getString(R.string.password_reset_too_many_attempts)
            "new password must be different" in normalized -> context.getString(R.string.password_reset_same_password)
            "challenge expired" in normalized -> context.getString(R.string.duel_challenge_expired)
            "friend request" in normalized && "already exists" in normalized -> context.getString(R.string.error_friend_request_exists)
            "you cannot" in normalized && "friend" in normalized -> context.getString(R.string.error_friend_action)
            "avatar" in normalized && "too large" in normalized -> context.getString(R.string.error_image_too_large)
            "avatar" in normalized || "image" in normalized -> context.getString(R.string.error_image_invalid)
            message.any { it.code in 0x0400..0x04FF } -> message
            else -> context.getString(R.string.error_generic)
        }
    }

    private fun fromHttp(context: Context, error: HttpException): String {
        val raw = runCatching { error.response()?.errorBody()?.string() }.getOrNull()
        val friendly = fromServerText(context, raw)
        if (friendly != context.getString(R.string.error_generic)) return friendly
        return when (error.code()) {
            400 -> context.getString(R.string.error_check_fields)
            401 -> context.getString(R.string.error_invalid_credentials)
            403 -> context.getString(R.string.error_access_denied)
            404 -> context.getString(R.string.error_not_found)
            409 -> context.getString(R.string.error_conflict)
            413 -> context.getString(R.string.error_image_too_large)
            429 -> context.getString(R.string.error_too_many_attempts)
            in 500..599 -> context.getString(R.string.error_server_unavailable)
            else -> context.getString(R.string.error_generic)
        }
    }
}
