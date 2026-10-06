package com.quran.labs.androidquran.ui.fragment

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.quran.data.core.QuranInfo
import com.quran.labs.androidquran.QuranApplication
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import com.quran.labs.androidquran.ui.compose.QuranRowList
import com.quran.labs.androidquran.data.Constants
import com.quran.labs.androidquran.data.QuranDisplayData
import com.quran.labs.androidquran.data.QuranFileConstants
import com.quran.labs.androidquran.presenter.data.JuzListPresenter
import com.quran.labs.androidquran.ui.QuranActivity
import com.quran.labs.androidquran.ui.helpers.QuranRow
import com.quran.labs.androidquran.ui.helpers.QuranRow.Builder
import com.quran.labs.androidquran.util.QuranUtils
import com.quran.labs.androidquran.view.JuzView
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Fragment that displays a list of all Juz (using [QuranRowList], each divided into
 * 8 parts (with headings for each Juz).
 * When a Juz part is selected (or a Juz heading), [QuranActivity.jumpTo] is called to
 * jump to that page.
 */
class JuzListFragment : Fragment() {
  private val listState = LazyListState()
  private var rows by mutableStateOf<List<QuranRow>>(emptyList())
  private val mainScope: CoroutineScope = MainScope()

  @Inject
  lateinit var quranInfo: QuranInfo

  @Inject
  lateinit var quranDisplayData: QuranDisplayData

  @Inject
  lateinit var juzListPresenter: JuzListPresenter

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        QuranTheme {
          QuranRowList(
            rows = rows,
            listState = listState,
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
            onRowClick = { _, row -> onRowClick(row) }
          )
        }
      }
    }
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    (context.applicationContext as QuranApplication).applicationComponent.inject(this)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    mainScope.launch {
      fetchJuz2List()
    }
  }

  override fun onResume() {
    val activity = requireActivity()
    if (activity is QuranActivity) {
      viewLifecycleOwner.lifecycleScope.launch {
        val recentPage = activity.latestPage()
        if (recentPage != Constants.NO_PAGE) {
          val juz = quranInfo.getJuzFromPage(recentPage)
          val position = (juz - 1) * 9
          listState.scrollToItem(position)
        }
      }
    }

    super.onResume()
  }

  private fun onRowClick(row: QuranRow) {
    val activity = activity as? QuranActivity
    if (activity != null && row.page != 0) {
      activity.jumpTo(row)
    }
  }

  private suspend fun fetchJuz2List() {
    val quarters = if (QuranFileConstants.FETCH_QUARTER_NAMES_FROM_DATABASE) {
      juzListPresenter.quarters().toTypedArray()
    } else {
      val context = context
      if (context != null) {
        val res = context.resources
        res.getStringArray(R.array.quarter_prefix_array)
      } else {
        emptyArray<String>()
      }
    }

    if (isAdded && quarters.isNotEmpty()) {
      updateJuz2List(quarters)
    }
  }

  private fun updateJuz2List(quarters: Array<String>) {
    val activity: Activity = activity ?: return

    val elements = arrayOfNulls<QuranRow>(Constants.JUZ2_COUNT * (8 + 1))
    var ctr = 0
    for (i in 0 until 8 * Constants.JUZ2_COUNT) {
      val pos = quranInfo.getQuarterByIndex(i)
      val page = quranInfo.getPageFromSuraAyah(pos.sura, pos.ayah)
      if (i % 8 == 0) {
        val juz = 1 + i / 8
        val juzTitle = activity.getString(
          R.string.juz2_description,
          QuranUtils.getLocalizedNumber(juz)
        )
        val builder = Builder()
          .withType(QuranRow.HEADER)
          .withText(juzTitle)
          .withPage(quranInfo.getStartingPageForJuz(juz))
        elements[ctr++] = builder.build()
      }
      val metadata = getString(
        R.string.sura_ayah_notification_str,
        quranDisplayData.getSuraName(activity, pos.sura, false), pos.ayah
      )
      val juzTextWithEllipsis = quarters[i] + "..."
      val builder = Builder()
        .withText(juzTextWithEllipsis)
        .withMetadata(metadata)
        .withPage(page)
        .withJuzType(ENTRY_TYPES[i % 4])
      if (i % 4 == 0) {
        val overlayText = QuranUtils.getLocalizedNumber(1 + i / 4)
        builder.withJuzOverlayText(overlayText)
      }
      elements[ctr++] = builder.build()
    }
    rows = elements.filterNotNull()
  }

  companion object {
    private val ENTRY_TYPES = intArrayOf(
      JuzView.TYPE_JUZ, JuzView.TYPE_QUARTER,
      JuzView.TYPE_HALF, JuzView.TYPE_THREE_QUARTERS
    )

    fun newInstance(): JuzListFragment {
      return JuzListFragment()
    }
  }
}
