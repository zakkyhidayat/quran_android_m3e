package com.quran.labs.androidquran.ui.fragment

import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.quran.common.search.SearchTextUtil
import com.quran.data.core.QuranInfo
import com.quran.labs.androidquran.QuranApplication
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.ui.compose.DialogSurface
import com.quran.labs.androidquran.ui.helpers.JumpDestination
import com.quran.labs.androidquran.util.QuranUtils
import dev.zacsweers.metro.Inject
import timber.log.Timber
import com.quran.mobile.common.ui.core.R as UiCoreR

/**
 * Dialog for quickly selecting and jumping to a particular location in the Quran. A location can
 * be selected by page number or Surah/Ayah.
 */
class JumpFragment : ComposeDialogFragment() {

  @Inject
  lateinit var quranInfo: QuranInfo

  override fun onAttach(context: Context) {
    super.onAttach(context)
    (context.applicationContext as QuranApplication).applicationComponent
      .inject(this)
  }

  override fun onStart() {
    super.onStart()
    dialog?.window?.setSoftInputMode(
      WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
    )
  }

  @Composable
  override fun Content() {
    val suraNames = remember {
      resources.getStringArray(UiCoreR.array.sura_names)
        .mapIndexed { index, sura -> QuranUtils.getLocalizedNumber(index + 1) + ". " + sura }
    }
    JumpDialogContent(
      quranInfo = quranInfo,
      suraNames = suraNames,
      onJump = { page, sura, ayah ->
        dismiss()
        try {
          (activity as? JumpDestination)?.jumpToAndHighlight(page, sura, ayah)
        } catch (e: Exception) {
          Timber.d(e, "Could not jump, something went wrong...")
        }
      },
      onCancel = ::dismiss
    )
  }

  companion object {
    const val TAG = "JumpFragment"
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JumpDialogContent(
  quranInfo: QuranInfo,
  suraNames: List<String>,
  onJump: (page: Int, sura: Int, ayah: Int) -> Unit,
  onCancel: () -> Unit
) {
  var sura by remember { mutableIntStateOf(1) }
  var ayah by remember { mutableIntStateOf(1) }
  var ayahText by remember { mutableStateOf("1") }
  var pageText by remember { mutableStateOf("") }
  var suraQuery by remember { mutableStateOf(suraNames.first()) }
  // while the sura field is being typed in, the menu lists only the matches
  var filtering by remember { mutableStateOf(false) }
  var menuOpen by remember { mutableStateOf(false) }

  val isRtl = remember { SearchTextUtil.isRtl(suraNames.first()) }
  val searchable = remember { suraNames.map { SearchTextUtil.asSearchableString(it, isRtl) } }
  val shown = if (!filtering || suraQuery.isEmpty()) {
    suraNames.indices.toList()
  } else {
    val infix = if (SearchTextUtil.isRtl(suraQuery)) {
      SearchTextUtil.asSearchableString(suraQuery, true)
    } else {
      suraQuery.lowercase()
    }
    val number = infix.toIntOrNull()?.toString()
    suraNames.indices.filter { i ->
      searchable[i].contains(infix) ||
        // support English numbers in Arabic mode
        (number != null && (i + 1).toString().contains(number))
    }
  }

  val pageFocus = remember { FocusRequester() }
  LaunchedEffect(Unit) { pageFocus.requestFocus() }

  val pageHint = quranInfo.getPageFromSuraAyah(sura, ayah)
  fun typedPage(): Int? = pageText.toIntOrNull()?.coerceIn(1..quranInfo.numberOfPages)
  val submit = { onJump(typedPage() ?: pageHint, sura, ayah) }

  fun selectSura(newSura: Int) {
    sura = newSura
    suraQuery = suraNames[newSura - 1]
    filtering = false
    ayah = ayah.coerceIn(1..quranInfo.getNumberOfAyahs(newSura))
    ayahText = ayah.toString()
    pageText = ""
  }

  DialogSurface(
    title = stringResource(R.string.menu_jump),
    confirmLabel = stringResource(R.string.dialog_ok),
    onConfirm = submit,
    dismissLabel = stringResource(UiCoreR.string.cancel),
    onDismiss = onCancel
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      OutlinedTextField(
        value = pageText,
        onValueChange = { raw ->
          val digits = raw.filter { it.isDigit() }.take(4)
          pageText = digits
          digits.toIntOrNull()?.let { number ->
            val page = number.coerceIn(1..quranInfo.numberOfPages)
            sura = quranInfo.getSuraOnPage(page)
            ayah = quranInfo.getFirstAyahOnPage(page)
            ayahText = ayah.toString()
            suraQuery = suraNames[sura - 1]
            filtering = false
          }
        },
        label = { Text(stringResource(R.string.gotoPage)) },
        placeholder = { Text(QuranUtils.getLocalizedNumber(pageHint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { submit() }),
        modifier = Modifier
          .fillMaxWidth()
          .focusRequester(pageFocus)
      )

      Text(
        text = stringResource(R.string.sura_and_ayah),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
      )
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ExposedDropdownMenuBox(
          expanded = menuOpen,
          onExpandedChange = { menuOpen = it },
          modifier = Modifier.weight(2f)
        ) {
          OutlinedTextField(
            value = suraQuery,
            onValueChange = { text ->
              suraQuery = text
              filtering = true
              menuOpen = true
              suraNames.indexOf(text).takeIf { it >= 0 }?.let { sura = it + 1 }
            },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
          )
          ExposedDropdownMenu(
            expanded = menuOpen && shown.isNotEmpty(),
            onDismissRequest = {
              menuOpen = false
              // leaving the field with a half typed name puts the chosen sura back
              suraQuery = suraNames[sura - 1]
              filtering = false
            }
          ) {
            shown.forEach { i ->
              DropdownMenuItem(
                text = { Text(suraNames[i]) },
                onClick = {
                  selectSura(i + 1)
                  menuOpen = false
                }
              )
            }
          }
        }
        OutlinedTextField(
          value = ayahText,
          onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }.take(3)
            val value = (digits.toIntOrNull() ?: 1).coerceIn(1..quranInfo.getNumberOfAyahs(sura))
            ayah = value
            // an empty field is the user clearing it to type a new number, so don't fill it in
            ayahText = if (digits.isEmpty()) "" else value.toString()
            pageText = ""
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
          keyboardActions = KeyboardActions(onGo = { submit() }),
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}
