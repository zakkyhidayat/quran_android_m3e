package com.quran.labs.androidquran.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.data.Constants

/** One translation in the picker: whether it is shown under the Arabic. */
class ReaderTranslationItem(val filename: String, val name: String, val checked: Boolean)

/** What the reader's top bar shows. The activity updates these as you turn pages. */
@Stable
class ReaderBarState {
  var title by mutableStateOf("")
  var subtitle by mutableStateOf("")
  var isBookmarked by mutableStateOf(false)
  var showingTranslation by mutableStateOf(false)
  var translations by mutableStateOf<List<ReaderTranslationItem>>(emptyList())
  var readingMode by mutableStateOf(Constants.READING_MODE_LIGHT)
}

/** What the reader's top bar can trigger. The activity owns what each of these does. */
class ReaderBarActions(
  val onBack: () -> Unit,
  val onBookmark: () -> Unit,
  val onToggleTranslation: () -> Unit,
  val onTranslationChecked: (filename: String) -> Unit,
  val onMoreTranslations: () -> Unit,
  val onReadingMode: (mode: String) -> Unit,
  val onSearch: () -> Unit,
  val onGoToPage: () -> Unit,
  val onSettings: () -> Unit,
  val onHelp: () -> Unit
)

/**
 * The reader's top bar: always visible above the page, with the surah, the page and where you are
 * in the Quran, and the actions you reach for while reading.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBar(state: ReaderBarState, actions: ReaderBarActions) {
  TopAppBar(
    title = { ReaderTitle(state, actions) },
    navigationIcon = {
      IconButton(onClick = actions.onBack) {
        Icon(QuranIcons.ArrowBack, contentDescription = stringResource(R.string.menu_back_to_page))
      }
    },
    actions = {
      IconButton(onClick = actions.onBookmark) {
        Icon(
          imageVector = if (state.isBookmarked) HomeIcons.BookmarkFilled else QuranIcons.BookmarkBorder,
          contentDescription = stringResource(R.string.menu_bookmarks),
          tint = if (state.isBookmarked) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          }
        )
      }
      IconButton(onClick = actions.onToggleTranslation) {
        Icon(
          imageVector = if (state.showingTranslation) QuranIcons.MenuBook else HomeIcons.Translate,
          contentDescription = stringResource(
            if (state.showingTranslation) R.string.menu_back_to_page else R.string.menu_translation
          ),
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      ReaderOverflowMenu(state, actions)
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
  )
}

/**
 * The surah, then the page and juz in smaller type. while reading translations, tapping it opens
 * the list of translations to show.
 */
@Composable
private fun ReaderTitle(state: ReaderBarState, actions: ReaderBarActions) {
  var pickerOpen by remember { mutableStateOf(false) }
  val canPick = state.showingTranslation && state.translations.isNotEmpty()

  Column(
    modifier = if (canPick) Modifier.clickable { pickerOpen = true } else Modifier
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = state.title,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f, fill = false)
      )
      if (canPick) {
        Icon(
          QuranIcons.ExpandMore,
          contentDescription = stringResource(R.string.translations),
          modifier = Modifier.padding(start = 4.dp)
        )
      }
    }
    Text(
      text = state.subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }

  ExpressiveMenu(
    expanded = pickerOpen,
    onDismiss = { pickerOpen = false },
    sections = listOf(
      MenuSection(
        title = stringResource(R.string.translations),
        entries = state.translations.map { item ->
          MenuEntry(item.name, selected = item.checked, keepOpen = true) {
            actions.onTranslationChecked(item.filename)
          }
        }
      ),
      MenuSection(
        entries = listOf(MenuEntry(stringResource(R.string.more_translations), onClick = actions.onMoreTranslations))
      )
    )
  )
}

@Composable
private fun ReaderOverflowMenu(state: ReaderBarState, actions: ReaderBarActions) {
  var expanded by remember { mutableStateOf(false) }
  IconButton(onClick = { expanded = true }) {
    Icon(
      HomeIcons.MoreVert,
      contentDescription = stringResource(androidx.appcompat.R.string.abc_action_menu_overflow_description)
    )
  }
  val modes = listOf(
    Constants.READING_MODE_LIGHT to R.string.reading_mode_light,
    Constants.READING_MODE_SEPIA to R.string.reading_mode_sepia,
    Constants.READING_MODE_NIGHT to R.string.reading_mode_night
  ).map { (mode, label) ->
    MenuEntry(stringResource(label), selected = state.readingMode == mode) { actions.onReadingMode(mode) }
  }
  ExpressiveMenu(
    expanded = expanded,
    onDismiss = { expanded = false },
    sections = listOf(
      MenuSection(title = stringResource(R.string.prefs_reading_mode_title), entries = modes),
      MenuSection(
        entries = listOf(
          MenuEntry(stringResource(R.string.menu_search), QuranIcons.Search, onClick = actions.onSearch),
          MenuEntry(stringResource(R.string.menu_jump), HomeIcons.Numbers, onClick = actions.onGoToPage)
        )
      ),
      MenuSection(
        entries = listOf(
          MenuEntry(stringResource(R.string.menu_settings), HomeIcons.Settings, onClick = actions.onSettings),
          MenuEntry(stringResource(R.string.menu_help), HomeIcons.Help, onClick = actions.onHelp)
        )
      )
    )
  )
}
