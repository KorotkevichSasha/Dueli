package com.example.duelingo.utils

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.airbnb.lottie.LottieAnimationView
import com.example.duelingo.R
import com.example.duelingo.activity.AchievementActivity
import com.example.duelingo.activity.LearningActivity
import com.example.duelingo.activity.MenuActivity
import com.example.duelingo.activity.ProfileActivity
import com.example.duelingo.activity.RankActivity
import com.example.duelingo.activity.TestActivity
import com.example.duelingo.activity.TopicsActivity
import com.google.android.material.color.MaterialColors

/** Owns the complete visual and click state of the duplicated bottom navigation views. */
object BottomNavigationController {
    private const val CLICK_DEBOUNCE_MS = 220L
    private var lastNavigationAt = 0L

    private enum class Destination { LEARNING, DUEL, RANK, PROFILE }

    private data class Item(
        val destination: Destination,
        val container: View,
        val icon: ImageView,
        val label: TextView,
        val animation: LottieAnimationView?,
        val iconResource: Int
    )

    fun sync(activity: Activity) {
        val active = destinationFor(activity) ?: return
        val items = findItems(activity) ?: return
        val primary = MaterialColors.getColor(items.first().container, com.google.android.material.R.attr.colorPrimary)
        val onSurface = MaterialColors.getColor(items.first().container, com.google.android.material.R.attr.colorOnSurface)
        styleBar(activity, items.first().container.parent as? ViewGroup, onSurface)

        items.forEach { item ->
            val selected = item.destination == active
            item.animation?.apply {
                cancelAnimation()
                removeAllAnimatorListeners()
                visibility = View.GONE
            }
            item.icon.apply {
                visibility = View.VISIBLE
                setImageResource(item.iconResource)
                setColorFilter(if (selected) primary else ColorUtils.setAlphaComponent(onSurface, 155))
                animate().cancel()
                scaleX = if (selected) 1.08f else 1f
                scaleY = if (selected) 1.08f else 1f
                translationY = if (selected) -activity.dp(1f) else 0f
            }
            item.label.apply {
                animate().cancel()
                setTextColor(if (selected) primary else ColorUtils.setAlphaComponent(onSurface, 165))
                setTypeface(typeface, if (selected) Typeface.BOLD else Typeface.NORMAL)
                alpha = if (selected) 1f else 0.88f
            }
            item.container.apply {
                animate().cancel()
                scaleX = 1f
                scaleY = 1f
                background = navigationBackground(activity, primary, selected)
                isSelected = selected
                isEnabled = true
                setOnClickListener { navigate(activity, item, active) }
            }
        }

        items.first { it.destination == active }.container.apply {
            scaleX = 0.97f
            scaleY = 0.97f
            animate().scaleX(1f).scaleY(1f).setDuration(170).start()
        }
    }

    private fun navigate(activity: Activity, item: Item, active: Destination) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastNavigationAt < CLICK_DEBOUNCE_MS) return
        lastNavigationAt = now
        item.container.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

        if (item.destination == active && isRootFor(activity, active)) {
            item.container.animate().cancel()
            item.container.animate()
                .scaleX(0.95f).scaleY(0.95f).setDuration(70)
                .withEndAction {
                    item.container.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }.start()
            return
        }

        val destination = when (item.destination) {
            Destination.LEARNING -> LearningActivity::class.java
            Destination.DUEL -> MenuActivity::class.java
            Destination.RANK -> RankActivity::class.java
            Destination.PROFILE -> ProfileActivity::class.java
        }
        showPendingDestination(activity, item.destination)
        activity.openTopLevel(destination)
    }

    private fun showPendingDestination(activity: Activity, destination: Destination) {
        val items = findItems(activity) ?: return
        val primary = MaterialColors.getColor(
            items.first().container,
            com.google.android.material.R.attr.colorPrimary
        )
        val onSurface = MaterialColors.getColor(
            items.first().container,
            com.google.android.material.R.attr.colorOnSurface
        )
        items.forEach { candidate ->
            val selected = candidate.destination == destination
            candidate.container.isSelected = selected
            candidate.container.background = navigationBackground(activity, primary, selected)
            candidate.icon.setColorFilter(
                if (selected) primary else ColorUtils.setAlphaComponent(onSurface, 155)
            )
            candidate.label.setTextColor(
                if (selected) primary else ColorUtils.setAlphaComponent(onSurface, 165)
            )
        }
        items.first { it.destination == destination }.container.apply {
            animate().cancel()
            scaleX = 0.96f
            scaleY = 0.96f
            animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        }
    }

    private fun navigationBackground(activity: Activity, primary: Int, selected: Boolean): InsetDrawable {
        val content = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = activity.dp(18f)
            setColor(if (selected) ColorUtils.setAlphaComponent(primary, 30) else Color.TRANSPARENT)
            if (selected) setStroke(activity.dp(1f).toInt(), ColorUtils.setAlphaComponent(primary, 48))
        }
        val ripple = RippleDrawable(
            ColorStateList.valueOf(ColorUtils.setAlphaComponent(primary, 42)),
            content,
            null
        )
        return InsetDrawable(ripple, activity.dp(5f).toInt(), activity.dp(5f).toInt(),
            activity.dp(5f).toInt(), activity.dp(4f).toInt())
    }

    private fun styleBar(activity: Activity, bar: ViewGroup?, onSurface: Int) {
        bar ?: return
        val parent = bar.parent as? ViewGroup
        parent?.clipChildren = false
        parent?.clipToPadding = false
        val targetWidth = parent?.width?.takeIf { it > 0 }
            ?: activity.window.decorView.width.takeIf { it > 0 }
            ?: activity.resources.displayMetrics.widthPixels
        bar.layoutParams = bar.layoutParams.apply {
            width = targetWidth
            height = activity.dp(60f).toInt()
        }
        bar.post {
            // Some screens apply horizontal padding to their root. The navigation bar must
            // still occupy the same full-window geometry on every top-level destination.
            bar.translationX = -bar.left.toFloat()
        }
        val surface = MaterialColors.getColor(bar, com.google.android.material.R.attr.colorSurface)
        bar.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = floatArrayOf(
                activity.dp(22f), activity.dp(22f),
                activity.dp(22f), activity.dp(22f),
                0f, 0f, 0f, 0f
            )
            setColor(surface)
            setStroke(activity.dp(1f).toInt(), ColorUtils.setAlphaComponent(onSurface, 22))
        }
        bar.elevation = activity.dp(12f)
        bar.clipToOutline = false
        bar.setPadding(activity.dp(3f).toInt(), 0, activity.dp(3f).toInt(), 0)
    }

    private fun findItems(activity: Activity): List<Item>? {
        fun item(
            destination: Destination,
            containerId: Int,
            iconId: Int,
            labelId: Int,
            animationId: Int,
            iconResource: Int
        ): Item? {
            val container = activity.findViewById<View>(containerId) ?: return null
            val icon = activity.findViewById<ImageView>(iconId) ?: return null
            val label = activity.findViewById<TextView>(labelId) ?: return null
            return Item(destination, container, icon, label,
                activity.findViewById(animationId), iconResource)
        }

        return listOfNotNull(
            item(Destination.LEARNING, R.id.tests, R.id.testIcon, R.id.testTest,
                R.id.testAnimation, R.drawable.graduation24),
            item(Destination.DUEL, R.id.duel, R.id.mainIcon, R.id.mainTest,
                R.id.duelAnimation, R.drawable.swords24),
            item(Destination.RANK, R.id.leaderboard, R.id.cupIcon, R.id.cupTest,
                R.id.cupAnimation, R.drawable.trophy24),
            item(Destination.PROFILE, R.id.profile, R.id.profileIcon, R.id.profileTest,
                R.id.profAnimation, R.drawable.profile24)
        ).takeIf { it.size == 4 }
    }

    private fun destinationFor(activity: Activity): Destination? = when (activity) {
        is LearningActivity, is TestActivity, is TopicsActivity, is AchievementActivity -> Destination.LEARNING
        is MenuActivity -> Destination.DUEL
        is RankActivity -> Destination.RANK
        is ProfileActivity -> Destination.PROFILE
        else -> null
    }

    private fun isRootFor(activity: Activity, destination: Destination): Boolean = when (destination) {
        Destination.LEARNING -> activity is LearningActivity
        Destination.DUEL -> activity is MenuActivity
        Destination.RANK -> activity is RankActivity
        Destination.PROFILE -> activity is ProfileActivity
    }

    private fun Activity.dp(value: Float): Float = value * resources.displayMetrics.density
}
