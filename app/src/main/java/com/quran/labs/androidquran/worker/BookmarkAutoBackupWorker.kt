package com.quran.labs.androidquran.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.quran.labs.androidquran.core.worker.WorkerTaskFactory
import com.quran.labs.androidquran.model.bookmark.BookmarkImportExportModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Saves the bookmarks to the backup file once a day. The export always writes the same file, so
 * there is only ever one: each run replaces the one before it.
 */
class BookmarkAutoBackupWorker(
  context: Context,
  params: WorkerParameters,
  private val bookmarkImportExportModel: BookmarkImportExportModel
) : CoroutineWorker(context, params) {

  override suspend fun doWork(): Result {
    return try {
      withContext(Dispatchers.IO) { bookmarkImportExportModel.exportBookmarksObservable().blockingGet() }
      Result.success()
    } catch (e: Exception) {
      Timber.e(e, "unable to back up the bookmarks")
      Result.retry()
    }
  }

  class Factory @Inject constructor(
    private val bookmarkImportExportModel: BookmarkImportExportModel
  ) : WorkerTaskFactory {
    override fun makeWorker(
      appContext: Context,
      workerParameters: WorkerParameters
    ): ListenableWorker = BookmarkAutoBackupWorker(appContext, workerParameters, bookmarkImportExportModel)
  }

  companion object {
    private const val UNIQUE_WORK = "bookmark_auto_backup"

    /** Starts the daily backup, or stops it. Safe to call whenever; an existing schedule is kept. */
    fun schedule(context: Context, enabled: Boolean) {
      val workManager = WorkManager.getInstance(context)
      if (enabled) {
        val request = PeriodicWorkRequestBuilder<BookmarkAutoBackupWorker>(1, TimeUnit.DAYS).build()
        workManager.enqueueUniquePeriodicWork(UNIQUE_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
      } else {
        workManager.cancelUniqueWork(UNIQUE_WORK)
      }
    }
  }
}
