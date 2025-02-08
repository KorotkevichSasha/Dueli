package com.example.duelingo.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.duelingo.activity.auth.LoginActivity
import com.example.duelingo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val accessToken = sharedPreferences.getString("access_token", null)
        Log.d("MainActivity", "AccessToken: $accessToken")

        if (accessToken.isNullOrEmpty()) {
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
        } else {
            Toast.makeText(applicationContext, "User is authenticated", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this@MainActivity, MenuActivity::class.java))
        }
    }
}
