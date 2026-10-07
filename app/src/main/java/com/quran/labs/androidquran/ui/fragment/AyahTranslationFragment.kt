package com.quran.labs.androidquran.ui.fragment

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import android.widget.ProgressBar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.quran.data.core.QuranInfo
import com.quran.data.model.VerseRange
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.QuranAyahInfo
import com.quran.labs.androidquran.presenter.translation.InlineTranslationPresenter
import com.quran.labs.androidquran.presenter.translation.InlineTranslationPresenter.TranslationScreen
import com.quran.labs.androidquran.ui.PagerActivity
import com.quran.labs.androidquran.ui.helpers.SlidingPagerAdapter
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import com.quran.labs.androidquran.ui.compose.TranslationPickItem
import com.quran.labs.androidquran.ui.compose.TranslationPicker
import com.quran.labs.androidquran.util.QuranSettings
import com.quran.labs.androidquran.view.InlineTranslationView
import com.quran.mobile.di.AyahActionFragmentProvider
import com.quran.mobile.translation.model.LocalTranslation
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs

class AyahTranslationFragment : AyahActionFragment(), TranslationScreen {
  private lateinit var progressBar: ProgressBar
  private lateinit var translationView: InlineTranslationView
  private lateinit var emptyState: View
  private lateinit var translator: ComposeView

  private var currentTranslations: List<LocalTranslation> = emptyList()
  private val pickerItems = mutableStateOf<List<TranslationPickItem>>(emptyList())

  @Inject
  lateinit var quranInfo: QuranInfo

  @Inject
  lateinit var quranSettings: QuranSettings

  @Inject
  lateinit var translationPresenter: InlineTranslationPresenter

  private val scope = MainScope()

  object Provider : AyahActionFragmentProvider {
    override val order = SlidingPagerAdapter.TRANSLATION_PAGE
    override val iconResId = com.quran.labs.androidquran.common.toolbar.R.drawable.ic_translation
    override fun newAyahActionFragment() = AyahTranslationFragment()
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    (activity as? PagerActivity)?.pagerActivityComponent?.inject(this)
  }

  override fun onDestroyView() {
    // the picker belongs to the window's row, which outlives this view
    (translator.parent as? android.view.ViewGroup)?.removeView(translator)
    super.onDestroyView()
  }

  override fun onDetach() {
    scope.cancel()
    super.onDetach()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(
      R.layout.translation_panel, container, false
    )
    // the picker lives in the window's action row, not under it
    val header = (activity as PagerActivity).ayahToolbarHeader
    header.removeAllViews()
    translator = ComposeView(requireContext()).apply {
      setContent {
        QuranTheme {
          TranslationPicker(
            items = pickerItems.value,
            onToggle = ::onTranslationToggled,
            onMore = { (activity as? PagerActivity)?.startTranslationManager() }
          )
        }
      }
    }
    header.addView(
      translator,
      FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        .apply { gravity = Gravity.CENTER_VERTICAL }
    )
    translationView = view.findViewById(R.id.translation_view)
    progressBar = view.findViewById(R.id.progress)
    emptyState = view.findViewById(R.id.empty_state)
    val getTranslations = view.findViewById<Button>(R.id.get_translations_button)
    getTranslations.setOnClickListener(onClickListener)

    return view
  }

  override fun onResume() {
    // currently needs to be before we call super.onResume
    translationPresenter.bind(this)
    super.onResume()
  }

  override fun onPause() {
    translationPresenter.unbind(this)
    super.onPause()
  }

  private val onClickListener = View.OnClickListener { v: View ->
    val activity: Activity? = activity
    if (activity is PagerActivity) {
      when (v.id) {
        R.id.get_translations_button -> activity.startTranslationManager()
      }
    }
  }

  override fun onTranslationsUpdated(translations: List<LocalTranslation>) {
    if (translations.isEmpty()) {
      progressBar.visibility = View.GONE
      emptyState.visibility = View.VISIBLE
      translator.visibility = View.GONE
      translationView.visibility = View.GONE
    } else {
      currentTranslations = translations
      updatePickerItems()
      refreshView()
    }
  }

  private fun updatePickerItems() {
    val active = quranSettings.activeTranslations
    pickerItems.value = currentTranslations.map {
      TranslationPickItem(it.filename, it.resolveTranslatorName(), active.contains(it.filename))
    }
  }

  /** Turns a translation on or off; one always stays on, so the last one cannot be turned off. */
  private fun onTranslationToggled(filename: String) {
    val selected = HashSet(quranSettings.activeTranslations)
    if (!selected.remove(filename)) {
      selected.add(filename)
    }
    if (selected.isEmpty()) {
      return
    }
    quranSettings.activeTranslations = selected
    updatePickerItems()
    refreshView()
  }

  public override fun refreshView() {
    val start = start
    val end = end
    if (start == null || end == null) {
      return
    }

    val verses = 1 + abs(
      quranInfo.getAyahId(start.sura, start.ayah) - quranInfo.getAyahId(end.sura, end.ayah)
    )
    val verseRange = VerseRange(start.sura, start.ayah, end.sura, end.ayah, verses)
    scope.launch {
      translationPresenter.refresh(verseRange)
    }
  }

  override fun setVerses(translations: Array<LocalTranslation>, verses: List<QuranAyahInfo>, ayahHasBeenChanged: Boolean) {
    progressBar.visibility = View.GONE
    if (verses.isNotEmpty()) {
      emptyState.visibility = View.GONE
      translator.visibility = View.VISIBLE
      translationView.visibility = View.VISIBLE
      translationView.setAyahs(translations, verses)
      if (ayahHasBeenChanged) {
        translationView.resetScroll()
      }
    } else {
      emptyState.visibility = View.VISIBLE
    }
  }
}
