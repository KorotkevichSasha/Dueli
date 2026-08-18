package com.example.duelingo.utils

import android.util.Base64
import org.json.JSONObject
import java.security.MessageDigest

/** Stable per-account cache key that survives access-token refreshes. */
object CacheIdentity {
    fun key(accessToken: String): String {
        val subject = runCatching {
            val payload = accessToken.split('.').getOrNull(1) ?: return@runCatching null
            val json = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
            JSONObject(json).optString("sub").takeIf { it.isNotBlank() }
        }.getOrNull()
        return sha256(subject ?: accessToken)
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .take(12)
        .joinToString("") { "%02x".format(it) }
}
