package com.example.duelingo

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.WindowManager
import com.example.duelingo.manager.LocaleManager
import com.example.duelingo.manager.ThemeManager
import com.example.duelingo.network.ApiClient
import com.example.duelingo.network.AuthSessionManager
import com.example.duelingo.utils.KeyboardInsets

class DuelingoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LocaleManager.init(this)
        // Инициализируем менеджер темы
        ThemeManager.init(this)
        AuthSessionManager.initialize(this)
        ApiClient.initialize(this)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                activity.window.decorView.post {
                    activity.findViewById<android.view.View>(android.R.id.content)?.let(KeyboardInsets::apply)
                }
            }

            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
