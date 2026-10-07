package com.quran.labs.androidquran.ui.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Something arriving on screen (a list row scrolling in, a dialog opening) rises a little and
 * settles on a bouncy spring while it fades in, instead of just being there.
 */
fun Modifier.expressiveAppear(rise: Float = 28f, from: Float = 0.94f, delayMillis: Long = 0L): Modifier = composed {
  val progress = remember { Animatable(0f) }
  LaunchedEffect(Unit) {
    if (delayMillis > 0) kotlinx.coroutines.delay(delayMillis)
    progress.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 420f))
  }
  graphicsLayer {
    val p = progress.value
    alpha = (p * 1.6f).coerceIn(0f, 1f)
    translationY = (1f - p) * rise
    val scale = from + (1f - from) * p
    scaleX = scale
    scaleY = scale
  }
}

/** The surface gives way under the finger: it shrinks a little and springs back when released. */
fun Modifier.pressScale(source: InteractionSource, pressed: Float = 0.96f): Modifier = composed {
  val isPressed by source.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) pressed else 1f,
    animationSpec = spring(dampingRatio = 0.45f, stiffness = 520f),
    label = "pressScale"
  )
  graphicsLayer {
    scaleX = scale
    scaleY = scale
  }
}

/**
 * The background of a screen that opens right after the system splash: it starts from the color
 * the window already has (the splash's) and glides to the theme's own, so there is no flash of a
 * different color between the two.
 */
@Composable
fun arrivalBackground(target: Color): Color {
  val context = LocalContext.current
  // the system splash is painted in the app theme's surface color, before any dynamic color applies
  val from = remember {
    Color(com.google.android.material.color.MaterialColors.getColor(
      context, com.google.android.material.R.attr.colorSurface, target.toArgb()
    ))
  }
  val color = remember { androidx.compose.animation.Animatable(from) }
  LaunchedEffect(target) {
    color.animateTo(target, tween(320))
  }
  return color.value
}
