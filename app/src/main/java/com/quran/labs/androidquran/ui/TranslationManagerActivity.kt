package com.quran.labs.androidquran.ui

import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.quran.labs.androidquran.QuranApplication
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import com.quran.labs.androidquran.dao.translation.TranslationItem
import com.quran.labs.androidquran.database.DatabaseHandler.Companion.clearDatabaseHandlerIfExists
import com.quran.labs.androidquran.presenter.translation.TranslationManagerPresenter
import com.quran.labs.androidquran.service.QuranDownloadService
import com.quran.labs.androidquran.service.util.DefaultDownloadReceiver
import com.quran.labs.androidquran.service.util.DefaultDownloadReceiver.SimpleDownloadListener
import com.quran.labs.androidquran.service.util.QuranDownloadNotifier
import com.quran.labs.androidquran.service.util.ServiceIntentHelper.getDownloadIntent
import com.quran.labs.androidquran.ui.compose.TranslationManagerActions
import com.quran.labs.androidquran.ui.compose.TranslationManagerScreen
import com.quran.labs.androidquran.util.QuranFileUtils
import com.quran.labs.androidquran.util.QuranSettings
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

class TranslationManagerActivity : AppCompatActivity(), SimpleDownloadListener {
  private var allItems by mutableStateOf<List<TranslationItem>>(emptyList())
  private var refreshing by mutableStateOf(true)
  private var downloadingItem: TranslationItem? = null
  private var downloadingId by mutableStateOf<Int?>(null)
  private var databaseDirectory: File? = null
  private var downloadReceiver: DefaultDownloadReceiver? = null
  private val snackbarHostState = SnackbarHostState()

  @Inject
  lateinit var presenter: TranslationManagerPresenter

  @Inject
  lateinit var quranFileUtils: QuranFileUtils

  @Inject
  lateinit var quranSettings: QuranSettings

  private val scope = MainScope()

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    (application as QuranApplication).applicationComponent.inject(this)
    databaseDirectory = quranFileUtils.getQuranDatabaseDirectory()

    val actions = TranslationManagerActions(
      onBack = ::finish,
      onRefresh = {
        refreshing = true
        refreshTranslations(forceDownload = true)
      },
      onDownload = ::downloadItem,
      onMove = ::moveItem,
      onRemove = ::removeItem
    )
    setContent {
      QuranTheme {
        TranslationManagerScreen(
          items = allItems,
          downloadingId = downloadingId,
          refreshing = refreshing,
          snackbarHostState = snackbarHostState,
          actions = actions
        )
      }
    }
    refreshTranslations()
  }

  public override fun onStop() {
    val receiver = downloadReceiver
    if (receiver != null) {
      receiver.setListener(null)
      LocalBroadcastManager.getInstance(this)
        .unregisterReceiver(receiver)
      downloadReceiver = null
    }
    super.onStop()
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }

  override fun handleDownloadSuccess() {
    val downloadingItem = downloadingItem
    if (downloadingItem != null) {
      if (downloadingItem.exists()) {
        try {
          val f = File(
            databaseDirectory,
            downloadingItem.translation.fileName + UPGRADING_EXTENSION
          )
          if (f.exists()) {
            f.delete()
          }
        } catch (e: Exception) {
          Timber.d(e, "error removing old database file")
        }
      }

      val sortedItems = allItems.filter { it.exists() }.sortedBy { it.displayOrder }
      val lastDisplayOrder =
        if (sortedItems.isEmpty()) 0 else sortedItems[sortedItems.size - 1].displayOrder
      val (_, _, currentVersion) = downloadingItem.translation
      updateTranslationItem(
        downloadingItem.withLocalVersionAndDisplayOrder(currentVersion, lastDisplayOrder + 1)
      )

      // update active translations and add this item to it
      val activeTranslations = quranSettings.activeTranslations
      activeTranslations.add(downloadingItem.translation.fileName)
      quranSettings.activeTranslations = activeTranslations
    }
    this.downloadingItem = null
    downloadingId = null
    updateUpgradeFlag()
  }

  override fun handleDownloadFailure(errId: Int) {
    val downloadingItem = downloadingItem
    if (downloadingItem != null && downloadingItem.exists()) {
      try {
        val f = File(
          databaseDirectory,
          downloadingItem.translation.fileName + UPGRADING_EXTENSION
        )
        val destFile = File(databaseDirectory, downloadingItem.translation.fileName)
        if (f.exists() && !destFile.exists()) {
          f.renameTo(destFile)
        } else {
          f.delete()
        }
      } catch (e: Exception) {
        Timber.d(e, "error restoring translation after failed download")
      }
    }
    this.downloadingItem = null
    downloadingId = null
  }

  private fun refreshTranslations(forceDownload: Boolean = false) {
    presenter.getTranslations(forceDownload)
      .onEach { items ->
        refreshing = false
        allItems = items
        updateUpgradeFlag()
      }
      .catch {
        refreshing = false
        snackbarHostState.showSnackbar(getString(R.string.error_getting_translation_list))
      }
      .launchIn(scope)
  }

  private fun updateUpgradeFlag() {
    if (allItems.none { it.exists() && it.needsUpgrade() }) {
      quranSettings.setHaveUpdatedTranslations(false)
    }
  }

  private fun updateTranslationItem(updated: TranslationItem) {
    allItems = allItems.map { if (it.translation.id == updated.translation.id) updated else it }
    scope.launch {
      presenter.updateItem(updated)
    }
  }

  private fun downloadItem(selectedItem: TranslationItem) {
    if ((selectedItem.exists() && !selectedItem.needsUpgrade()) || downloadingItem != null) {
      return
    }
    downloadingItem = selectedItem
    downloadingId = selectedItem.translation.id
    val (_, _, _, _, _, fileName, url) = selectedItem.translation
    clearDatabaseHandlerIfExists(fileName)
    if (downloadReceiver == null) {
      val downloadReceiver = DefaultDownloadReceiver(
        this,
        QuranDownloadService.DOWNLOAD_TYPE_TRANSLATION
      )
      LocalBroadcastManager.getInstance(this).registerReceiver(
        downloadReceiver, IntentFilter(
          QuranDownloadNotifier.ProgressIntent.INTENT_NAME
        )
      )
      this.downloadReceiver = downloadReceiver
    }
    downloadReceiver!!.setListener(this)

    // actually start the download
    val destination = databaseDirectory
    Timber.d("downloading %s to %s", url, destination)
    if (selectedItem.exists()) {
      try {
        val f = File(destination, fileName)
        if (f.exists()) {
          val newPath = File(
            destination,
            fileName + UPGRADING_EXTENSION
          )
          if (newPath.exists()) {
            newPath.delete()
          }
          f.renameTo(newPath)
        }
      } catch (e: Exception) {
        Timber.d(e, "error backing database file up")
      }
    }

    // start the download
    val notificationTitle = selectedItem.name()
    val intent = getDownloadIntent(
      this, url,
      destination?.absolutePath ?: "", notificationTitle, TRANSLATION_DOWNLOAD_KEY,
      QuranDownloadService.DOWNLOAD_TYPE_TRANSLATION
    )
    var filename = selectedItem.translation.fileName
    if (url.endsWith("zip")) {
      filename += ".zip"
    }
    intent.putExtra(QuranDownloadService.EXTRA_OUTPUT_FILE_NAME, filename)
    startService(intent)
  }

  private fun removeItem(selectedItem: TranslationItem) {
    if (removeTranslation(selectedItem.translation.fileName)) {
      updateTranslationItem(selectedItem.withTranslationRemoved())

      // remove from active translations
      val activeTranslations = quranSettings.activeTranslations
      activeTranslations.remove(selectedItem.translation.fileName)
      quranSettings.activeTranslations = activeTranslations
      updateUpgradeFlag()
    }
  }

  /** Moves a downloaded translation up (-1) or down (+1) in the order they are shown in. */
  private fun moveItem(item: TranslationItem, delta: Int) {
    val sorted = allItems.filter { it.exists() }.sortedBy { it.displayOrder }.toMutableList()
    val index = sorted.indexOfFirst { it.translation.id == item.translation.id }
    val target = index + delta
    if (index < 0 || target !in sorted.indices) return

    sorted.add(target, sorted.removeAt(index))
    val normalized = sorted.mapIndexed { i, it -> it.withDisplayOrder(i + 1) }
    val byId = normalized.associateBy { it.translation.id }
    allItems = allItems.map { byId[it.translation.id] ?: it }
    scope.launch {
      presenter.updateItemOrdering(normalized)
    }
  }

  private fun removeTranslation(fileName: String): Boolean {
    val path = quranFileUtils.getQuranDatabaseDirectory()
    val f = File(path, fileName)
    return f.delete()
  }

  companion object {
    const val TRANSLATION_DOWNLOAD_KEY = "TRANSLATION_DOWNLOAD_KEY"
    private const val UPGRADING_EXTENSION = ".old"
  }
}
