package com.quran.labs.androidquran.ui.compose

import androidx.compose.animation.animateColorAsState
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
fun ReaderModeBar(current: ReaderView, onSelect: (ReaderView) -> Unit, modifier: Modifier = Modifier) {
  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation = 3.dp,
    shadowElevation = 6.dp,
    modifier = modifier.padding(8.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 4.dp).height(48.dp)
    ) {
      ModeButton(ReaderView.PAGE, QuranIcons.MenuBook, R.string.reader_view_page, current, onSelect)
      ModeButton(ReaderView.BOTH, HomeIcons.Notes, R.string.reader_view_both, current, onSelect)
      ModeButton(ReaderView.TRANSLATION, HomeIcons.Translate, R.string.reader_view_translation, current, onSelect)
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
  val container by animateColorAsState(
    if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
    label = "container"
  )
  val tint by animateColorAsState(
    if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    label = "tint"
  )
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .width(64.dp)
      .height(40.dp)
      .clip(CircleShape)
      .background(container)
      .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(view) })
  ) {
    Icon(icon, contentDescription = stringResource(label), tint = tint, modifier = Modifier.size(24.dp))
  }
}
