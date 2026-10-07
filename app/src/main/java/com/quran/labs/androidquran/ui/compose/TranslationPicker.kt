package com.quran.labs.androidquran.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranIcons

/** One translation in the picker: whether it is shown in the ayah window. */
class TranslationPickItem(val filename: String, val name: String, val checked: Boolean)

/**
 * The translation picker at the start of the ayah window's row: a pill with the name of the
 * translation (or how many are on) that opens a Material 3 menu, where each translation is a
 * choice that can be turned on and off. One always stays on.
 */
@Composable
fun TranslationPicker(
  items: List<TranslationPickItem>,
  onToggle: (filename: String) -> Unit,
  onMore: () -> Unit
) {
  var open by remember { mutableStateOf(false) }
  val on = items.filter { it.checked }
  val label = when (on.size) {
    0 -> stringResource(R.string.translations)
    1 -> on.first().name
    else -> pluralStringResource(R.plurals.translations_on, on.size, on.size)
  }
  Box(modifier = Modifier.padding(horizontal = 8.dp)) {
    Surface(
      onClick = { open = true },
      shape = CircleShape,
      color = MaterialTheme.colorScheme.secondaryContainer,
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      modifier = Modifier.fillMaxWidth().height(36.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, end = 8.dp)
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        Icon(QuranIcons.ExpandMore, contentDescription = stringResource(R.string.translations))
      }
    }
    ExpressiveMenu(
      expanded = open,
      onDismiss = { open = false },
      sections = listOf(
        // no title: the pill the menu opens from already says "translations"
        MenuSection(
          entries = items.map { item ->
            MenuEntry(item.name, selected = item.checked, keepOpen = true) { onToggle(item.filename) }
          }
        ),
        MenuSection(
          entries = listOf(MenuEntry(stringResource(R.string.more_translations), onClick = onMore))
        )
      )
    )
  }
}
