package com.example.duelingo.activity

import android.os.Bundle
import com.example.duelingo.utils.AppToast as Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.duelingo.R
import com.example.duelingo.databinding.ActivityStoreBinding
import com.example.duelingo.dto.response.EconomyResponse
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import com.example.duelingo.utils.UserMessage
import com.example.duelingo.utils.EconomyCache
import com.example.duelingo.manager.RewardedAdManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.ceil

class StoreActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStoreBinding
    private val tokenManager by lazy { TokenManager(this) }
    private var loading = false
    private var currentEconomy: EconomyResponse? = null
    private var removeAdObserver: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        EconomyCache.read(this)?.let(::render)

        binding.pocketPack.setOnClickListener { purchase("POCKET") }
        binding.boostPack.setOnClickListener { purchase("BOOST") }
        binding.vaultPack.setOnClickListener { purchase("VAULT") }
        binding.adRewardCard.setOnClickListener { showRewardedAd() }
        removeAdObserver = RewardedAdManager.observe(::renderRewardedAdState)
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
        currentEconomy = economy
        EconomyCache.store(this, economy)
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

    private fun showRewardedAd() {
        val shown = RewardedAdManager.show(
            activity = this,
            onRewarded = { claimRewardedAdGold() },
            onDismissed = {},
            onFailure = {
                setRewardedAdState(true, R.string.store_rewarded_ad_retry)
            }
        )
        if (!shown) {
            RewardedAdManager.retry()
            Toast.makeText(this, R.string.store_rewarded_ad_wait, Toast.LENGTH_SHORT).show()
        }
    }

    private fun claimRewardedAdGold() {
        val token = tokenManager.getAccessToken() ?: return
        lifecycleScope.launch {
            runCatching { ApiClient.userService.claimRewardedAdGold("Bearer $token") }
                .onSuccess { reward ->
                    currentEconomy?.copy(gold = reward.totalGold)?.let(::render)
                    val message = if (reward.goldAwarded > 0) {
                        getString(R.string.store_rewarded_ad_reward_received, reward.goldAwarded, reward.totalGold)
                    } else {
                        getString(R.string.store_rewarded_ad_reward_limit)
                    }
                    Toast.makeText(this@StoreActivity, message, Toast.LENGTH_LONG).show()
                }
                .onFailure {
                    Toast.makeText(
                        this@StoreActivity,
                        UserMessage.from(this@StoreActivity, it),
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun renderRewardedAdState(state: RewardedAdManager.State) {
        when (state) {
            RewardedAdManager.State.DISABLED -> setRewardedAdState(false, R.string.store_rewarded_ad_unavailable)
            RewardedAdManager.State.INITIALIZING,
            RewardedAdManager.State.LOADING -> setRewardedAdState(false, R.string.store_rewarded_ad_loading)
            RewardedAdManager.State.READY -> setRewardedAdState(true, R.string.store_rewarded_ad_ready)
            RewardedAdManager.State.SHOWING -> setRewardedAdState(false, R.string.store_rewarded_ad_showing)
            RewardedAdManager.State.FAILED -> setRewardedAdState(true, R.string.store_rewarded_ad_retry)
        }
    }

    private fun setRewardedAdState(enabled: Boolean, message: Int) {
        binding.adRewardCard.isEnabled = enabled
        binding.adRewardCard.alpha = if (enabled) 1f else 0.72f
        binding.adRewardDescription.setText(message)
    }

    override fun onDestroy() {
        removeAdObserver?.invoke()
        removeAdObserver = null
        super.onDestroy()
    }
}
