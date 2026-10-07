package com.quran.labs.androidquran.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranIcons

/** What the reader shows: the Quran page, the page's verses with their translation, or just the translation. */
enum class ReaderView { PAGE, BOTH, TRANSLATION }

/**
 * Three round choices floating at the bottom centre of the reader: Page, Arabic and translation,
 * Translation only. The current one sits in a green pill; each has the same width so nothing
 * shifts when you switch.
 */
@Composable
fun ReaderModeBar(current: ReaderView, onSelect: (ReaderView) -> Unit) {
  ModeRow(current, onSelect)
}

/**
 * "¼ Hizb 3": settles at the top of the page, in the blank margin above the text, when a new juz
 * or hizb starts, and leaves by itself.
 */
@Composable
fun MarkerPill(marker: String?) {
  var shown by remember { mutableStateOf("") }
  if (marker != null) shown = marker
  AnimatedVisibility(
    visible = marker != null,
    enter = fadeIn() + slideInVertically(spring(dampingRatio = 0.55f, stiffness = 420f)) { -it },
    exit = fadeOut() + slideOutVertically { -it / 2 }
  ) {
    Surface(
      shape = CircleShape,
      color = MaterialTheme.colorScheme.secondaryContainer,
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      shadowElevation = 3.dp
    ) {
      Text(
        text = shown,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
      )
    }
  }
}

@Composable
private fun ModeRow(current: ReaderView, onSelect: (ReaderView) -> Unit) {
  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation = 3.dp,
    shadowElevation = 4.dp,
    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
  ) {
    // the green pill is one shape that travels to the chosen button on a bouncy spring
    val pillOffset by animateDpAsState(
      targetValue = (64 * current.ordinal).dp,
      animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
      label = "pill"
    )
    Box(modifier = Modifier.padding(horizontal = 4.dp).height(48.dp)) {
      Box(
        Modifier
          .offset(x = pillOffset)
          .align(Alignment.CenterStart)
          .width(64.dp)
          .height(40.dp)
          .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
      )
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(48.dp)) {
        ModeButton(ReaderView.PAGE, QuranIcons.MenuBook, R.string.reader_view_page, current, onSelect)
        ModeButton(ReaderView.BOTH, HomeIcons.Notes, R.string.reader_view_both, current, onSelect)
        ModeButton(ReaderView.TRANSLATION, HomeIcons.Translate, R.string.reader_view_translation, current, onSelect)
      }
    }
  }
}

@Composable
private fun ModeButton(
  view: ReaderView,
  icon: ImageVector,
  label: Int,
  current: ReaderView,
  onSelect: (ReaderView) -> Unit
) {
  val selected = view == current
  val tint by animateColorAsState(
    if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    label = "tint"
  )
  // the chosen icon swells a little as the pill arrives
  val swell by animateFloatAsState(
    targetValue = if (selected) 1.14f else 1f,
    animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f),
    label = "swell"
  )
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .width(64.dp)
      .height(40.dp)
      .clip(CircleShape)
      .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(view) })
  ) {
    Icon(
      icon,
      contentDescription = stringResource(label),
      tint = tint,
      modifier = Modifier
        .size(24.dp)
        .graphicsLayer {
          scaleX = swell
          scaleY = swell
        }
    )
  }
}
