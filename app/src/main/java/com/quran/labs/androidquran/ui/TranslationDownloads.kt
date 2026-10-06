package com.quran.labs.androidquran.ui

import android.content.Context
import android.content.IntentFilter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.quran.labs.androidquran.dao.translation.TranslationItem
import com.quran.labs.androidquran.database.DatabaseHandler.Companion.clearDatabaseHandlerIfExists
import com.quran.labs.androidquran.presenter.translation.TranslationManagerPresenter
import com.quran.labs.androidquran.service.QuranDownloadService
import com.quran.labs.androidquran.service.util.DefaultDownloadReceiver
import com.quran.labs.androidquran.service.util.QuranDownloadNotifier
import com.quran.labs.androidquran.service.util.ServiceIntentHelper.getDownloadIntent
import com.quran.labs.androidquran.util.QuranFileUtils
import com.quran.labs.androidquran.util.QuranSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/**
 * The list of translations and the work of getting, ordering and removing them, kept as Compose
 * state. Shared by the translation manager and the first-run setup.
 */
class TranslationDownloads(
  private val context: Context,
  private val presenter: TranslationManagerPresenter,
  private val quranFileUtils: QuranFileUtils,
  private val quranSettings: QuranSettings,
  private val scope: CoroutineScope,
  private val onError: suspend () -> Unit = {}
) : DefaultDownloadReceiver.SimpleDownloadListener {

  var items by mutableStateOf<List<TranslationItem>>(emptyList())
    private set
  var refreshing by mutableStateOf(true)
    private set
  var downloadingId by mutableStateOf<Int?>(null)
    private set

  private var downloadingItem: TranslationItem? = null
  private var downloadReceiver: DefaultDownloadReceiver? = null
  private val databaseDirectory: File? = quranFileUtils.getQuranDatabaseDirectory()

  fun refresh(forceDownload: Boolean = false) {
    refreshing = true
    presenter.getTranslations(forceDownload)
      .onEach { updated ->
        refreshing = false
        items = updated
        updateUpgradeFlag()
      }
      .catch {
        refreshing = false
        onError()
      }
      .launchIn(scope)
  }

  /** Stop listening for download progress, as the screen goes away. */
  fun stop() {
    downloadReceiver?.let { receiver ->
      receiver.setListener(null)
      LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
    }
    downloadReceiver = null
  }

  fun download(selectedItem: TranslationItem) {
    if ((selectedItem.exists() && !selectedItem.needsUpgrade()) || downloadingItem != null) {
      return
    }
    downloadingItem = selectedItem
    downloadingId = selectedItem.translation.id
    val (_, _, _, _, _, fileName, url) = selectedItem.translation
    clearDatabaseHandlerIfExists(fileName)
    val receiver = downloadReceiver ?: DefaultDownloadReceiver(
      context,
      QuranDownloadService.DOWNLOAD_TYPE_TRANSLATION
    ).also {
      LocalBroadcastManager.getInstance(context).registerReceiver(
        it, IntentFilter(QuranDownloadNotifier.ProgressIntent.INTENT_NAME)
      )
      downloadReceiver = it
    }
    receiver.setListener(this)

    val destination = databaseDirectory
    Timber.d("downloading %s to %s", url, destination)
    if (selectedItem.exists()) {
      // keep the old copy until the new one arrives
      try {
        val f = File(destination, fileName)
        if (f.exists()) {
          val newPath = File(destination, fileName + UPGRADING_EXTENSION)
          if (newPath.exists()) {
            newPath.delete()
          }
          f.renameTo(newPath)
        }
      } catch (e: Exception) {
        Timber.d(e, "error backing database file up")
      }
    }

    val intent = getDownloadIntent(
      context, url,
      destination?.absolutePath ?: "", selectedItem.name(), TRANSLATION_DOWNLOAD_KEY,
      QuranDownloadService.DOWNLOAD_TYPE_TRANSLATION
    )
    var filename = selectedItem.translation.fileName
    if (url.endsWith("zip")) {
      filename += ".zip"
    }
    intent.putExtra(QuranDownloadService.EXTRA_OUTPUT_FILE_NAME, filename)
    context.startService(intent)
  }

  override fun handleDownloadSuccess() {
    val item = downloadingItem
    if (item != null) {
      if (item.exists()) {
        try {
          val f = File(databaseDirectory, item.translation.fileName + UPGRADING_EXTENSION)
          if (f.exists()) {
            f.delete()
          }
        } catch (e: Exception) {
          Timber.d(e, "error removing old database file")
        }
      }

      val lastDisplayOrder = items.filter { it.exists() }.maxOfOrNull { it.displayOrder } ?: 0
      updateItem(
        item.withLocalVersionAndDisplayOrder(item.translation.currentVersion, lastDisplayOrder + 1)
      )

      // a new translation is shown right away
      val activeTranslations = quranSettings.activeTranslations
      activeTranslations.add(item.translation.fileName)
      quranSettings.activeTranslations = activeTranslations
    }
    downloadingItem = null
    downloadingId = null
    updateUpgradeFlag()
  }

  override fun handleDownloadFailure(errId: Int) {
    val item = downloadingItem
    if (item != null && item.exists()) {
      try {
        val f = File(databaseDirectory, item.translation.fileName + UPGRADING_EXTENSION)
        val destFile = File(databaseDirectory, item.translation.fileName)
        if (f.exists() && !destFile.exists()) {
          f.renameTo(destFile)
        } else {
          f.delete()
        }
      } catch (e: Exception) {
        Timber.d(e, "error restoring translation after failed download")
      }
    }
    downloadingItem = null
    downloadingId = null
  }

  fun remove(selectedItem: TranslationItem) {
    val file = File(quranFileUtils.getQuranDatabaseDirectory(), selectedItem.translation.fileName)
    if (file.delete()) {
      updateItem(selectedItem.withTranslationRemoved())

      val activeTranslations = quranSettings.activeTranslations
      activeTranslations.remove(selectedItem.translation.fileName)
      quranSettings.activeTranslations = activeTranslations
      updateUpgradeFlag()
    }
  }

  /** Moves a downloaded translation up (-1) or down (+1) in the order they are shown in. */
  fun move(item: TranslationItem, delta: Int) {
    val sorted = items.filter { it.exists() }.sortedBy { it.displayOrder }.toMutableList()
    val index = sorted.indexOfFirst { it.translation.id == item.translation.id }
    val target = index + delta
    if (index < 0 || target !in sorted.indices) return

    sorted.add(target, sorted.removeAt(index))
    val normalized = sorted.mapIndexed { i, it -> it.withDisplayOrder(i + 1) }
    val byId = normalized.associateBy { it.translation.id }
    items = items.map { byId[it.translation.id] ?: it }
    scope.launch {
      presenter.updateItemOrdering(normalized)
    }
  }

  private fun updateItem(updated: TranslationItem) {
    items = items.map { if (it.translation.id == updated.translation.id) updated else it }
    scope.launch {
      presenter.updateItem(updated)
    }
  }

  private fun updateUpgradeFlag() {
    if (items.none { it.exists() && it.needsUpgrade() }) {
      quranSettings.setHaveUpdatedTranslations(false)
    }
  }

  companion object {
    const val TRANSLATION_DOWNLOAD_KEY = "TRANSLATION_DOWNLOAD_KEY"
    private const val UPGRADING_EXTENSION = ".old"
  }
}
