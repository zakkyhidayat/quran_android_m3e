package com.quran.labs.androidquran.ui.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.TwoStatePreference
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.ComposePreferenceRow
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.ui.fragment.QuranPreferenceFragment
import com.quran.labs.androidquran.ui.preference.QuranHeaderPreference
import com.quran.labs.androidquran.ui.preference.SegmentedListPreference
import com.quran.labs.androidquran.ui.preference.SeekBarPreference
import com.quran.labs.androidquran.util.QuranUtils
import kotlin.math.roundToInt

/**
 * A settings screen: a collapsing app bar over the preferences of [fragment], drawn as grouped
 * Material 3 rows. The fragment (which has no view) still builds the preferences and handles what
 * a change does; this only draws them.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreferencesScreen(
  @StringRes title: Int,
  fragment: QuranPreferenceFragment,
  onBack: () -> Unit
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.surface,
    topBar = {
      LargeFlexibleTopAppBar(
        title = { Text(stringResource(title)) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(QuranIcons.ArrowBack, contentDescription = stringResource(R.string.menu_back_to_page))
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        scrollBehavior = scrollBehavior
      )
    }
  ) { innerPadding ->
    PreferencesContent(
      group = fragment.preferenceScreen,
      changeCount = fragment.changeCount,
      contentPadding = PaddingValues(
        top = innerPadding.calculateTopPadding() + 4.dp,
        bottom = innerPadding.calculateBottomPadding() + 24.dp
      )
    )
  }
}

private sealed interface Entry {
  class Label(val title: CharSequence) : Entry
  class Hero(val preference: Preference) : Entry
  class Item(val preference: Preference, val position: Int, val count: Int) : Entry
}

/** Preferences under a category become one group; loose ones at the top level become another. */
private fun flatten(root: PreferenceGroup): List<Entry> {
  val out = ArrayList<Entry>()

  fun addGroup(title: CharSequence?, prefs: List<Preference>) {
    if (prefs.isEmpty()) return
    if (!title.isNullOrEmpty()) out += Entry.Label(title)
    var run = ArrayList<Preference>()
    fun flushRun() {
      run.forEachIndexed { i, p -> out += Entry.Item(p, i, run.size) }
      run = ArrayList()
    }
    for (p in prefs) {
      if (p is QuranHeaderPreference) {
        flushRun()
        out += Entry.Hero(p)
      } else {
        run += p
      }
    }
    flushRun()
  }

  val loose = ArrayList<Preference>()
  for (i in 0 until root.preferenceCount) {
    val p = root.getPreference(i)
    if (!p.isVisible) continue
    if (p is PreferenceCategory) {
      addGroup(null, loose.toList())
      loose.clear()
      addGroup(p.title, (0 until p.preferenceCount).map { p.getPreference(it) }.filter { it.isVisible })
    } else {
      loose += p
    }
  }
  addGroup(null, loose)
  return out
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreferencesContent(
  group: PreferenceGroup,
  changeCount: Int,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(0.dp)
) {
  // taps change preferences in place, so a counter is what tells the list to read them again
  var taps by remember { mutableIntStateOf(0) }
  val version = changeCount + taps
  val entries = remember(group, version) { flatten(group) }
  val onChanged: () -> Unit = { taps++ }

  LazyColumn(
    modifier = modifier,
    contentPadding = contentPadding,
    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
  ) {
    itemsIndexed(entries) { _, entry ->
      when (entry) {
        is Entry.Label -> SettingsLabel(entry.title.toString())
        is Entry.Hero -> SettingsHero(entry.preference, version)
        is Entry.Item -> PreferenceItem(
          preference = entry.preference,
          shapes = ListItemDefaults.segmentedShapes(entry.position, entry.count),
          version = version,
          onChanged = onChanged
        )
      }
    }
  }
}

@Composable
private fun SettingsLabel(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 8.dp)
  )
}

/** The logo, name and description at the top of the about screen. */
@Composable
@Suppress("UNUSED_PARAMETER")
private fun SettingsHero(preference: Preference, version: Int) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Image(
      painter = painterResource(R.drawable.icon),
      contentDescription = null,
      modifier = Modifier.size(88.dp)
    )
    Spacer(Modifier.height(12.dp))
    Text(
      text = preference.title?.toString().orEmpty(),
      style = MaterialTheme.typography.headlineSmall,
      textAlign = TextAlign.Center
    )
    preference.summary?.let {
      Text(
        text = it.toString(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp)
      )
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
@Suppress("UNUSED_PARAMETER")
private fun PreferenceItem(
  preference: Preference,
  shapes: ListItemShapes,
  version: Int,
  onChanged: () -> Unit
) {
  val modifier = Modifier
    .padding(horizontal = 16.dp)
    .then(if (preference.isEnabled) Modifier else Modifier.alpha(0.38f))

  when (preference) {
    is ComposePreferenceRow -> Box(modifier = Modifier.padding(horizontal = 16.dp)) {
      preference.Content()
    }

    is TwoStatePreference -> {
      val summary = when {
        preference.isChecked && !preference.summaryOn.isNullOrEmpty() -> preference.summaryOn
        !preference.isChecked && !preference.summaryOff.isNullOrEmpty() -> preference.summaryOff
        else -> preference.summary
      }
      SettingsItem(
        title = preference.title,
        summary = summary,
        enabled = preference.isEnabled,
        shapes = shapes,
        modifier = modifier,
        onClick = {
          preference.performClick()
          onChanged()
        },
        trailing = {
          Switch(
            checked = preference.isChecked,
            onCheckedChange = null,
            thumbContent = if (preference.isChecked) {
              { Icon(QuranIcons.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
            } else {
              null
            }
          )
        }
      )
    }

    is SegmentedListPreference -> SegmentedPreferenceItem(preference, shapes, modifier, onChanged)
    is ListPreference -> ListPreferenceItem(preference, shapes, modifier, onChanged)
    is SeekBarPreference -> SeekBarItem(preference, shapes, modifier, onChanged)
    else -> SettingsItem(
      title = preference.title,
      summary = preference.summary,
      enabled = preference.isEnabled && preference.isSelectable,
      shapes = shapes,
      modifier = modifier,
      onClick = {
        preference.performClick()
        onChanged()
      }
    )
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsItem(
  title: CharSequence?,
  summary: CharSequence?,
  enabled: Boolean,
  shapes: ListItemShapes,
  modifier: Modifier,
  onClick: () -> Unit,
  trailing: (@Composable () -> Unit)? = null,
  supporting: (@Composable () -> Unit)? = null
) {
  SegmentedListItem(
    onClick = onClick,
    enabled = enabled,
    shapes = shapes,
    colors = ListItemDefaults.segmentedColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ),
    modifier = modifier,
    supportingContent = when {
      supporting != null -> supporting
      !summary.isNullOrEmpty() -> ({ Text(summary.toString()) })
      else -> null
    },
    trailingContent = trailing
  ) {
    Text(
      text = title?.toString().orEmpty(),
      style = MaterialTheme.typography.titleMedium
    )
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ListPreferenceItem(
  preference: ListPreference,
  shapes: ListItemShapes,
  modifier: Modifier,
  onChanged: () -> Unit
) {
  var showDialog by remember { mutableStateOf(false) }
  val summary = preference.summary?.takeIf { it.isNotEmpty() }
    ?: preference.entry?.let { entry -> entry.toString().lineSequence().first() }

  SettingsItem(
    title = preference.title,
    summary = summary,
    enabled = preference.isEnabled,
    shapes = shapes,
    modifier = modifier,
    onClick = { showDialog = true }
  )

  if (showDialog) {
    val entries = preference.entries.orEmpty()
    val values = preference.entryValues.orEmpty()
    AlertDialog(
      onDismissRequest = { showDialog = false },
      title = { Text((preference.dialogTitle ?: preference.title)?.toString().orEmpty()) },
      text = {
        Column(
          modifier = Modifier
            .verticalScroll(rememberScrollState())
            .selectableGroup()
        ) {
          entries.forEachIndexed { i, entry ->
            val value = values.getOrNull(i)?.toString() ?: return@forEachIndexed
            val lines = entry.toString().lines()
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .selectable(
                  selected = value == preference.value,
                  role = Role.RadioButton,
                  onClick = {
                    showDialog = false
                    if (preference.callChangeListener(value)) preference.value = value
                    onChanged()
                  }
                )
                .padding(vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              RadioButton(selected = value == preference.value, onClick = null)
              Column {
                Text(lines.first(), style = MaterialTheme.typography.bodyLarge)
                if (lines.size > 1) {
                  Text(
                    text = lines.drop(1).joinToString("\n"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showDialog = false }) {
          Text(stringResource(android.R.string.cancel))
        }
      }
    )
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SeekBarItem(
  preference: SeekBarPreference,
  shapes: ListItemShapes,
  modifier: Modifier,
  onChanged: () -> Unit
) {
  var value by remember(preference) { mutableIntStateOf(preference.currentValue()) }
  val label = QuranUtils.getLocalizedNumber(value) + (preference.suffix ?: "")

  SettingsItem(
    title = preference.title,
    summary = preference.summary,
    enabled = preference.isEnabled,
    shapes = shapes,
    modifier = modifier,
    onClick = {},
    supporting = {
      Column {
        preference.summary?.takeIf { it.isNotEmpty() }?.let { Text(it.toString()) }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Slider(
            value = value.toFloat(),
            onValueChange = { value = it.roundToInt() },
            onValueChangeFinished = {
              preference.commitValue(value)
              onChanged()
            },
            enabled = preference.isEnabled,
            valueRange = 0f..preference.maxValue.toFloat(),
            modifier = Modifier.weight(1f)
          )
          Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 12.dp)
          )
        }
        SeekBarPreview(preference.preview, value)
      }
    }
  )
}

/** A sample under the slider, so you can see what the size or brightness will look like. */
@Composable
private fun SeekBarPreview(kind: SeekBarPreference.Preview, value: Int) {
  val sample = stringResource(R.string.prefs_preview)
  when (kind) {
    SeekBarPreference.Preview.NONE -> Unit
    SeekBarPreference.Preview.TEXT_SIZE ->
      Text(text = sample, fontSize = value.coerceAtLeast(1).sp, modifier = Modifier.padding(top = 4.dp))

  }
}

/** A choice among a few short options (system / light / dark, say), shown as segmented buttons. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SegmentedPreferenceItem(
  preference: SegmentedListPreference,
  shapes: ListItemShapes,
  modifier: Modifier,
  onChanged: () -> Unit
) {
  val entries = preference.entries.orEmpty()
  val values = preference.entryValues.orEmpty()

  SettingsItem(
    title = preference.title,
    summary = null,
    enabled = preference.isEnabled,
    shapes = shapes,
    modifier = modifier,
    onClick = {},
    supporting = {
      Column {
        preference.summary?.takeIf { it.isNotEmpty() }?.let { Text(it.toString()) }
        SingleChoiceSegmentedButtonRow(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp)
        ) {
          entries.forEachIndexed { index, entry ->
            val value = values.getOrNull(index)?.toString() ?: return@forEachIndexed
            SegmentedButton(
              selected = value == preference.value,
              onClick = {
                if (preference.callChangeListener(value)) preference.value = value
                onChanged()
              },
              shape = SegmentedButtonDefaults.itemShape(index, entries.size),
              enabled = preference.isEnabled,
              label = { Text(entry.toString(), maxLines = 1) }
            )
          }
        }
      }
    }
  )
}
