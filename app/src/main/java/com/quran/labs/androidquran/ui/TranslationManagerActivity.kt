package com.quran.labs.androidquran.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHostState
import com.quran.labs.androidquran.QuranApplication
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import com.quran.labs.androidquran.presenter.translation.TranslationManagerPresenter
import com.quran.labs.androidquran.ui.compose.TranslationManagerActions
import com.quran.labs.androidquran.ui.compose.TranslationManagerScreen
import com.quran.labs.androidquran.util.QuranFileUtils
import com.quran.labs.androidquran.util.QuranSettings
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

class TranslationManagerActivity : AppCompatActivity() {

  @Inject
  lateinit var presenter: TranslationManagerPresenter

  @Inject
  lateinit var quranFileUtils: QuranFileUtils

  @Inject
  lateinit var quranSettings: QuranSettings

  private val scope = MainScope()
  private val snackbarHostState = SnackbarHostState()
  private lateinit var downloads: TranslationDownloads

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    (application as QuranApplication).applicationComponent.inject(this)

    downloads = TranslationDownloads(this, presenter, quranFileUtils, quranSettings, scope) {
      snackbarHostState.showSnackbar(getString(R.string.error_getting_translation_list))
    }
    val actions = TranslationManagerActions(
      onBack = ::finish,
      onRefresh = { downloads.refresh(forceDownload = true) },
      onDownload = downloads::download,
      onMove = downloads::move,
      onRemove = downloads::remove
    )
    setContent {
      QuranTheme {
        TranslationManagerScreen(
          items = downloads.items,
          downloadingId = downloads.downloadingId,
          refreshing = downloads.refreshing,
          snackbarHostState = snackbarHostState,
          actions = actions
        )
      }
    }
    downloads.refresh()
  }

  public override fun onStop() {
    downloads.stop()
    super.onStop()
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }
}
