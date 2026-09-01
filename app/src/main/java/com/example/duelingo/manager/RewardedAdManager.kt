package com.example.duelingo.manager

import android.app.Activity
import android.content.Context
import android.util.Log
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.YandexAds
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader

/** Keeps one rewarded video warm for the whole application session. */
object RewardedAdManager {
    enum class State { DISABLED, INITIALIZING, LOADING, READY, SHOWING, FAILED }

    private const val TAG = "RewardedAdManager"
    private var loader: RewardedAdLoader? = null
    private var ad: RewardedAd? = null
    private var adUnitId: String = ""
    private var currentState = State.DISABLED
    private val observers = LinkedHashSet<(State) -> Unit>()

    fun initialize(context: Context, configuredAdUnitId: String) {
        if (configuredAdUnitId.isBlank()) {
            updateState(State.DISABLED)
            return
        }
        if (adUnitId == configuredAdUnitId && currentState != State.DISABLED) return
        adUnitId = configuredAdUnitId
        updateState(State.INITIALIZING)
        val appContext = context.applicationContext
        YandexAds.initialize(appContext) {
            loader = RewardedAdLoader(appContext)
            load()
        }
    }

    fun observe(observer: (State) -> Unit): () -> Unit {
        observers += observer
        observer(currentState)
        return { observers -= observer }
    }

    fun retry() {
        if (currentState == State.FAILED) load()
    }

    fun show(
        activity: Activity,
        onRewarded: (Reward) -> Unit,
        onDismissed: () -> Unit,
        onFailure: (String) -> Unit
    ): Boolean {
        val loadedAd = ad ?: return false
        ad = null
        updateState(State.SHOWING)
        loadedAd.setAdEventListener(object : RewardedAdEventListener {
            override fun onAdShown() = Unit
            override fun onAdFailedToShow(error: AdError) {
                Log.w(TAG, "Rewarded ad failed to show: ${error.description}")
                loadedAd.setAdEventListener(null)
                onFailure(error.description)
                load()
            }
            override fun onAdDismissed() {
                loadedAd.setAdEventListener(null)
                onDismissed()
                load()
            }
            override fun onAdClicked() = Unit
            override fun onAdImpression(impressionData: ImpressionData?) = Unit
            override fun onRewarded(reward: Reward) = onRewarded(reward)
        })
        loadedAd.show(activity)
        return true
    }

    private fun load() {
        val activeLoader = loader ?: return
        if (adUnitId.isBlank() || currentState == State.LOADING || currentState == State.READY) return
        updateState(State.LOADING)
        activeLoader.loadAd(
            AdRequest.Builder(adUnitId).build(),
            object : RewardedAdLoadListener {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    ad = rewardedAd
                    updateState(State.READY)
                }
                override fun onAdFailedToLoad(error: com.yandex.mobile.ads.common.AdRequestError) {
                    Log.w(TAG, "Rewarded ad failed to load: ${error.description}")
                    ad = null
                    updateState(State.FAILED)
                }
            }
        )
    }

    private fun updateState(state: State) {
        currentState = state
        observers.toList().forEach { it(state) }
    }
}
