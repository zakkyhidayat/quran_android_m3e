package com.quran.labs.androidquran.ui.compose

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.quran.labs.androidquran.common.ui.core.QuranIcons

/**
 * One row of an [ExpressiveMenu]. A non-null [selected] makes it a choice (like a sort order or a
 * reading mode) that shows a check while it is the current one.
 */
class MenuEntry(
  val label: String,
  val icon: ImageVector? = null,
  val selected: Boolean? = null,
  val keepOpen: Boolean = false,
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
            entry.selected == true -> ({ Icon(QuranIcons.Check, contentDescription = null) })
            entry.icon != null -> ({ Icon(entry.icon, contentDescription = null) })
            else -> null
          }
          if (entry.selected != null) {
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
