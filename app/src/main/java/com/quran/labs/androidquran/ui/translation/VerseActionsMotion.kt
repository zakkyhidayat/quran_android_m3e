package com.quran.labs.androidquran.ui.translation

import android.view.View
import android.view.ViewGroup
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

/**
 * The motion of the actions beside a selected verse: expressive springs, not a plain fade. The pill
 * slides in from the end and settles with a little bounce, and its buttons pop in one after the
 * other.
 */
internal object VerseActionsMotion {
  private const val STIFFNESS = 420f
  private const val BOUNCY = 0.55f
  private const val STAGGER_MS = 35L

  fun popIn(pill: View) {
    val density = pill.resources.displayMetrics.density
    pill.animate().cancel()
    pill.alpha = 0f
    pill.translationX = 28 * density
    pill.animate().alpha(1f).setDuration(140).start()
    spring(pill, DynamicAnimation.TRANSLATION_X, 0f, 0.7f)

    val buttons = pill as? ViewGroup ?: return
    for (i in 0 until buttons.childCount) {
      val button = buttons.getChildAt(i)
      button.scaleX = 0f
      button.scaleY = 0f
      button.postDelayed({
        spring(button, DynamicAnimation.SCALE_X, 1f, BOUNCY)
        spring(button, DynamicAnimation.SCALE_Y, 1f, BOUNCY)
      }, 60 + i * STAGGER_MS)
    }
  }

  fun popOut(pill: View) {
    pill.animate().cancel()
    pill.animate()
      .alpha(0f)
      .translationX(16 * pill.resources.displayMetrics.density)
      .setDuration(110)
      .withEndAction {
        pill.visibility = View.GONE
        pill.alpha = 1f
        pill.translationX = 0f
        (pill as? ViewGroup)?.let { group ->
          for (i in 0 until group.childCount) {
            group.getChildAt(i).apply { scaleX = 1f; scaleY = 1f }
          }
        }
      }
      .start()
  }

  private fun spring(view: View, property: DynamicAnimation.ViewProperty, target: Float, damping: Float) {
    SpringAnimation(view, property, target).apply {
      spring.stiffness = STIFFNESS
      spring.dampingRatio = damping
      start()
    }
  }
}
