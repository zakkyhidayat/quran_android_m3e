package com.quran.labs.androidquran.ui.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * An icon button on a disc that gives way under the finger, as the expressive icon buttons do: the
 * circle squares off into a rounded square and the button shrinks a little, on a bouncy spring, and
 * both come back when the finger lifts.
 */
@Composable
fun ExpressiveIconButton(
  onClick: () -> Unit,
  containerColor: Color,
  modifier: Modifier = Modifier,
  shape: Shape? = null,
  content: @Composable () -> Unit
) {
  val source = remember { MutableInteractionSource() }
  val pressed by source.collectIsPressedAsState()
  val wiggle by animateFloatAsState(
    targetValue = if (pressed) -10f else 0f,
    animationSpec = spring(dampingRatio = 0.35f, stiffness = 450f),
    label = "wiggle"
  )
  val corner by animateDpAsState(
    targetValue = if (pressed) 14.dp else 24.dp,
    animationSpec = spring(dampingRatio = 0.5f, stiffness = 520f),
    label = "corner"
  )
  val scale by animateFloatAsState(
    targetValue = if (pressed) 0.88f else 1f,
    animationSpec = spring(dampingRatio = 0.45f, stiffness = 520f),
    label = "scale"
  )
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .padding(horizontal = 2.dp)
      .size(44.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
        // a shaped button turns a little under the finger
        rotationZ = if (shape != null) wiggle else 0f
      }
      .clip(shape ?: RoundedCornerShape(corner))
      .background(containerColor)
      .clickable(
        interactionSource = source,
        indication = LocalIndication.current,
        role = Role.Button,
        onClick = onClick
      )
  ) {
    content()
  }
}
