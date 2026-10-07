package com.quran.labs.androidquran.ui.compose

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Where a button sits in a split group: the outer ends are round, the corners that face a neighbour stay tight. */
enum class ButtonSegment { ALONE, START, MIDDLE, END }

/**
 * An icon button in the split style of the expressive button groups: a pill whose neighbours are
 * separate pills with a small gap between them. Under the finger the button squeezes and every
 * corner rounds off to the same small radius, on a bouncy spring, then springs back.
 */
@Composable
fun ExpressiveIconButton(
  onClick: () -> Unit,
  containerColor: Color,
  modifier: Modifier = Modifier,
  segment: ButtonSegment = ButtonSegment.ALONE,
  content: @Composable () -> Unit
) {
  val source = remember { MutableInteractionSource() }
  val pressed by source.collectIsPressedAsState()
  val outer = 22.dp
  val inner = 6.dp
  val spec = spring<androidx.compose.ui.unit.Dp>(dampingRatio = 0.5f, stiffness = 520f)
  val startCorner by animateDpAsState(
    if (pressed) 12.dp else if (segment == ButtonSegment.ALONE || segment == ButtonSegment.START) outer else inner,
    spec, label = "start"
  )
  val endCorner by animateDpAsState(
    if (pressed) 12.dp else if (segment == ButtonSegment.ALONE || segment == ButtonSegment.END) outer else inner,
    spec, label = "end"
  )
  val scale by animateFloatAsState(
    targetValue = if (pressed) 0.9f else 1f,
    animationSpec = spring(dampingRatio = 0.45f, stiffness = 520f),
    label = "scale"
  )
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .width(if (segment == ButtonSegment.ALONE) 44.dp else 50.dp)
      .height(44.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .clip(
        RoundedCornerShape(
          topStart = startCorner,
          bottomStart = startCorner,
          topEnd = endCorner,
          bottomEnd = endCorner
        )
      )
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
