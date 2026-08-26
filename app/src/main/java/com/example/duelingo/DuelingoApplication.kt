package com.example.duelingo

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.WindowManager
import com.example.duelingo.manager.LocaleManager
import com.example.duelingo.manager.ThemeManager
import com.example.duelingo.network.ApiClient
import com.example.duelingo.network.AuthSessionManager
import com.example.duelingo.utils.BottomNavigationController
import com.example.duelingo.utils.KeyboardInsets
import com.bumptech.glide.Glide

class DuelingoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LocaleManager.init(this)
        // Инициализируем менеджер темы
        ThemeManager.init(this)
        AuthSessionManager.initialize(this)
        ApiClient.initialize(this)
        ApiClient.warmUpServer()
        Glide.with(this).load(R.drawable.duel_hero_wide).preload(1536, 512)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                activity.window.decorView.post {
                    activity.findViewById<android.view.View>(android.R.id.content)?.let(KeyboardInsets::apply)
                    // onActivityResumed can be delivered before the first layout pass on some
                    // vendor builds (notably EMUI). In that case the fifth item cannot yet be
                    // attached to the legacy navigation row. Repeat the idempotent sync once
                    // the content view is laid out so every device always gets all five tabs.
                    BottomNavigationController.sync(activity)
                }
            }

            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) {
                activity.window.decorView.post { BottomNavigationController.sync(activity) }
            }
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
