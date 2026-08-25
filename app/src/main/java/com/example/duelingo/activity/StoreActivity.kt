package com.example.duelingo.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.duelingo.R
import com.example.duelingo.databinding.ActivityStoreBinding
import com.example.duelingo.dto.response.EconomyResponse
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import com.example.duelingo.utils.UserMessage
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.ceil

class StoreActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStoreBinding
    private val tokenManager by lazy { TokenManager(this) }
    private var loading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pocketPack.setOnClickListener { purchase("POCKET") }
        binding.boostPack.setOnClickListener { purchase("BOOST") }
        binding.vaultPack.setOnClickListener { purchase("VAULT") }
        binding.adRewardCard.setOnClickListener {
            Toast.makeText(this, R.string.store_admob_pending, Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadEconomy()
    }

    private fun loadEconomy() {
        val token = tokenManager.getAccessToken() ?: return
        lifecycleScope.launch {
            runCatching { ApiClient.userService.getEconomy("Bearer $token") }
                .onSuccess(::render)
                .onFailure { Toast.makeText(this@StoreActivity, UserMessage.from(this@StoreActivity, it), Toast.LENGTH_LONG).show() }
        }
    }

    private fun purchase(pack: String) {
        if (loading) return
        val token = tokenManager.getAccessToken() ?: return
        lifecycleScope.launch {
            setPackButtonsEnabled(false)
            runCatching { ApiClient.userService.purchaseRushPack("Bearer $token", pack) }
                .onSuccess {
                    render(it)
                    Toast.makeText(this@StoreActivity, R.string.rush_pack_purchased, Toast.LENGTH_SHORT).show()
                }
                .onFailure { Toast.makeText(this@StoreActivity, UserMessage.from(this@StoreActivity, it), Toast.LENGTH_LONG).show() }
            setPackButtonsEnabled(true)
        }
    }

    private fun render(economy: EconomyResponse) {
        binding.goldBalance.text = NumberFormat.getIntegerInstance(resources.configuration.locales[0])
            .format(economy.gold)
        binding.rushBalance.text = "${economy.rushCharges}/${economy.maxRushCharges}"
        binding.rushTimer.text = if (economy.rushCharges >= economy.maxRushCharges || economy.nextRushChargeAt == null) {
            getString(R.string.rush_sparks_ready)
        } else {
            val minutes = runCatching {
                ceil(
                    Duration.between(LocalDateTime.now(), LocalDateTime.parse(economy.nextRushChargeAt))
                        .seconds.coerceAtLeast(0) / 60.0
                ).toInt().coerceAtLeast(1)
            }.getOrDefault(economy.minutesPerCharge)
            getString(R.string.rush_sparks_next, minutes)
        }
    }

    private fun setPackButtonsEnabled(enabled: Boolean) {
        loading = !enabled
        binding.pocketPack.isEnabled = enabled
        binding.boostPack.isEnabled = enabled
        binding.vaultPack.isEnabled = enabled
    }
}
