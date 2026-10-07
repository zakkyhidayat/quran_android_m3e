package com.quran.labs.androidquran.ui.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import com.quran.labs.androidquran.common.ui.core.QuranIcons

/**
 * One row of an [ExpressiveMenu]. A non-null [selected] makes it a choice (like a sort order or a
 * reading mode): the current one takes the green container, and shows a check unless it has an
 * icon of its own.
 */
class MenuEntry(
  val label: String,
  val icon: ImageVector? = null,
  val selected: Boolean? = null,
  val keepOpen: Boolean = false,
  /** A non-null value makes it an on/off row: a switch at the end instead of a check. */
  val toggled: Boolean? = null,
  val onClick: () -> Unit
)

/** A run of related rows, drawn as one rounded container, optionally with a title. */
class MenuSection(val title: String? = null, val entries: List<MenuEntry>)

/**
 * The Material 3 Expressive menu: related items sit in rounded groups that are separated by a gap
 * (instead of a single list cut by dividers), and each item has a leading icon where one helps.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveMenu(
  expanded: Boolean,
  onDismiss: () -> Unit,
  sections: List<MenuSection>
) {
  DropdownMenuPopup(expanded = expanded, onDismissRequest = onDismiss) {
    sections.forEachIndexed { sectionIndex, section ->
      DropdownMenuGroup(shapes = MenuDefaults.groupShape(sectionIndex, sections.size)) {
        section.title?.let { title ->
          MenuDefaults.DropdownMenuGroupLabel { Text(title) }
        }
        section.entries.forEachIndexed { index, entry ->
          val shapes = MenuDefaults.itemShape(index, section.entries.size)
          val click = {
            if (!entry.keepOpen) onDismiss()
            entry.onClick()
          }
          val leading: @Composable (() -> Unit)? = when {
            entry.icon != null -> ({ Icon(entry.icon, contentDescription = null) })
            entry.selected == true -> ({ Icon(QuranIcons.Check, contentDescription = null) })
            else -> null
          }
          if (entry.toggled != null) {
            DropdownMenuItem(
              onClick = click,
              text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(entry.label, modifier = Modifier.weight(1f, fill = false))
                  Spacer(Modifier.width(16.dp))
                  Switch(checked = entry.toggled, onCheckedChange = null)
                }
              },
              shape = shapes.shape,
              leadingIcon = leading
            )
          } else if (entry.selected != null) {
            SelectableDropdownMenuItem(
              selected = entry.selected,
              onClick = click,
              text = { Text(entry.label) },
              shapes = shapes,
              // the chosen item takes the green secondary container; the default is the tertiary
              // one, which in this palette is gold and reads as brown on the dark theme
              colors = MenuDefaults.selectableItemColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
              ),
              leadingIcon = leading
            )
          } else {
            DropdownMenuItem(
              onClick = click,
              text = { Text(entry.label) },
              shape = shapes.shape,
              leadingIcon = leading
            )
          }
        }
      }
      if (sectionIndex != sections.lastIndex) {
        Spacer(Modifier.height(MenuDefaults.GroupSpacing))
      }
    }
  }
}
