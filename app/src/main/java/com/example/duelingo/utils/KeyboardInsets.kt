package com.example.duelingo.utils

import android.graphics.Rect
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.max

/** Keeps focused controls above the IME on edge-to-edge Android versions. */
object KeyboardInsets {
    fun apply(root: View) {
        if (!appliedViews.add(root)) return

        val initialLeft = root.paddingLeft
        val initialTop = root.paddingTop
        val initialRight = root.paddingRight
        val initialBottom = root.paddingBottom
        val focusMargin = (24 * root.resources.displayMetrics.density).toInt()

        root.viewTreeObserver.addOnGlobalFocusChangeListener { _, focused ->
            focused ?: return@addOnGlobalFocusChangeListener
            // Some vendor keyboards (notably older EMUI + SwiftKey) report
            // their final height after the focus event. Repeat the request
            // once the resize animation has settled.
            focused.postDelayed({ revealFocusedView(focused, focusMargin) }, 280L)
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            view.updatePadding(
                left = initialLeft + bars.left,
                top = initialTop + bars.top,
                right = initialRight + bars.right,
                bottom = initialBottom + max(bars.bottom, ime.bottom)
            )

            if (imeVisible) {
                view.post {
                    view.findFocus()?.let { revealFocusedView(it, focusMargin) }
                }
                view.postDelayed({ view.findFocus()?.let { revealFocusedView(it, focusMargin) } }, 280L)
            }
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun revealFocusedView(focused: View, margin: Int) {
        focused.requestRectangleOnScreen(
            Rect(0, -margin, focused.width, focused.height + margin * 2),
            true
        )
    }

    private val appliedViews = Collections.newSetFromMap(WeakHashMap<View, Boolean>())
}
