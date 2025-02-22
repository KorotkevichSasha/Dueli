package com.example.duelingo.manager

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import com.example.duelingo.dto.response.UserProfileResponse
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class AvatarManager(
    private val context: Context,
    private val tokenManager: TokenManager,
    private val sharedPreferences: SharedPreferences
) {

    private val apiClient = ApiClient.userService

    fun loadAvatar(userId: String, imageView: ImageView) {
        val accessToken = tokenManager.getAccessToken() ?: return
        val tokenWithBearer = "Bearer $accessToken"
        val savedETag = sharedPreferences.getString("avatar_etag_$userId", null)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiClient.getAvatar(tokenWithBearer, savedETag, userId)

                if (response.code() == 304) {
                    Log.d("Avatar", "Not modified, using cached version.")
                    loadCachedAvatar(userId, imageView)
                    return@launch
                }

                response.body()?.let { body ->
                    val bitmap = BitmapFactory.decodeStream(body.byteStream())
                    saveAvatarToCache(userId, bitmap, response.headers()["ETag"])
                    withContext(Dispatchers.Main) {
                        imageView.setImageBitmap(bitmap)
                    }
                }
            } catch (e: Exception) {
                Log.e("Avatar", "Error loading avatar: ${e.message}")
                loadCachedAvatar(userId, imageView)
            }
        }
    }
    private fun saveAvatarToCache(userId: String, bitmap: Bitmap, eTag: String?) {
        val cacheDir = context.cacheDir
        val file = File(cacheDir, "avatar_$userId.jpg")

        try {
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }
            if (eTag != null) {
                sharedPreferences.edit().putString("avatar_etag_$userId", eTag).apply()
            }
        } catch (e: Exception) {
            Log.e("AvatarCache", "Error saving avatar to cache: ${e.message}")
        }
    }
    private suspend fun loadCachedAvatar(userId: String, imageView: ImageView) {
        val cacheDir = context.cacheDir
        val file = File(cacheDir, "avatar_$userId.jpg")

        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            withContext(Dispatchers.Main) {
                imageView.setImageBitmap(bitmap)
            }
        } else {
            Log.d("AvatarCache", "No cached avatar found for user $userId")
        }
    }

    fun uploadImage(uri: Uri, onSuccess: (UserProfileResponse) -> Unit, onError: (String) -> Unit) {
        val mimeType = context.contentResolver.getType(uri) ?: "image/*"
        val accessToken = tokenManager.getAccessToken() ?: run {
            onError("Access token is missing.")
            return
        }

        val tokenWithBearer = "Bearer $accessToken"
        val file = createTempFileFromUri(uri) ?: run {
            onError("Failed to create file from URI")
            return
        }

        if (file.length() > 2 * 1024 * 1024) {
            onError("File is too large")
            file.delete()
            return
        }

        val sanitizedFileName = file.name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", sanitizedFileName, requestFile)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiClient.uploadAvatar(tokenWithBearer, body)
                withContext(Dispatchers.Main) {
                    onSuccess(response)
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("UploadError", "HTTP ${e.code()}: $errorBody")
                onError("Error uploading image: ${e.message()}")
            } catch (e: Exception) {
                Log.e("UploadError", "Unexpected error: ${e.message}")
                onError("Unexpected error: ${e.message}")
            } finally {
                file.delete()
            }
        }
    }

    private fun createTempFileFromUri(uri: Uri): File? {
        return try {
            val contentResolver: ContentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)

            if (inputStream != null) {
                val file = File.createTempFile("temp_avatar", ".jpg", context.cacheDir)
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
}