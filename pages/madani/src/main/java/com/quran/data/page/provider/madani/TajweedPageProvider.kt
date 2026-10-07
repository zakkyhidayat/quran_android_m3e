package com.quran.data.page.provider.madani

import com.quran.data.model.audio.Qari
import com.quran.data.source.DisplaySize
import com.quran.data.source.PageProvider
import com.quran.data.source.PageSizeCalculator
import com.quran.labs.androidquran.pages.common.madani.size.TajweedPageSizeCalculator
import com.quran.labs.androidquran.pages.madani.R

/**
 * The Tajweed mushaf: the same 604 pages and the same ayah on each page as the Madani mushaf,
 * with the tajweed rules in color, drawn at a single size.
 */
class TajweedPageProvider : PageProvider {
  private val madani = MadaniPageProvider()

  override fun getDataSource() = madani.getDataSource()

  override fun getPageSizeCalculator(displaySize: DisplaySize): PageSizeCalculator =
    TajweedPageSizeCalculator()

  override fun getImageVersion() = 7

  override fun getImagesBaseUrl() = "$baseUrl/"

  override fun getImagesZipBaseUrl() = "$baseUrl/zips/"

  override fun getPatchBaseUrl() = "$baseUrl/patches/v"

  override fun getAyahInfoBaseUrl() = "$baseUrl/databases/ayahinfo/"

  override fun getAudioDirectoryName() = madani.getAudioDirectoryName()

  override fun getDatabaseDirectoryName() = madani.getDatabaseDirectoryName()

  // kept beside its own pages, so the Madani ayah positions are not mixed with these
  override fun getAyahInfoDirectoryName() = "tajweed/databases"

  override fun getDatabasesBaseUrl() = madani.getDatabasesBaseUrl()

  override fun getAudioDatabasesBaseUrl() = madani.getAudioDatabasesBaseUrl()

  // its own folder, so the Madani pages stay where they are
  override fun getImagesDirectoryName() = "tajweed"

  override fun getPreviewTitle() = R.string.tajweed_title

  override fun getPreviewDescription() = R.string.tajweed_description

  override fun getDefaultQariId(): Int = madani.getDefaultQariId()

  override fun getQaris(): List<Qari> = madani.getQaris()

  override fun pageType(): String = "tajweed"

  companion object {
    private const val baseUrl = "https://files.quran.app/hafs/tajweed"
  }
}
