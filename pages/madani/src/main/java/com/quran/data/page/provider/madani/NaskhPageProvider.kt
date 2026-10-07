package com.quran.data.page.provider.madani

import com.quran.data.model.audio.Qari
import com.quran.data.source.DisplaySize
import com.quran.data.source.PageProvider
import com.quran.data.source.PageSizeCalculator
import com.quran.labs.androidquran.pages.common.madani.size.NaskhPageSizeCalculator
import com.quran.labs.androidquran.pages.data.madani.NaskhDataSource
import com.quran.labs.androidquran.pages.madani.R

/** The Naskh (Indo-Pak) mushaf, with its own page layout, so its own data source. */
class NaskhPageProvider : PageProvider {
  private val madani = MadaniPageProvider()

  override fun getDataSource() = dataSource

  override fun getPageSizeCalculator(displaySize: DisplaySize): PageSizeCalculator =
    NaskhPageSizeCalculator(displaySize)

  override fun getImageVersion() = 3

  override fun getImagesBaseUrl() = "$baseUrl/"

  override fun getImagesZipBaseUrl() = "$baseUrl/zips/"

  override fun getPatchBaseUrl() = "$baseUrl/patches/v"

  override fun getAyahInfoBaseUrl() = "$baseUrl/databases/ayahinfo/"

  override fun getAudioDirectoryName() = madani.getAudioDirectoryName()

  override fun getDatabaseDirectoryName() = madani.getDatabaseDirectoryName()

  // kept beside its own pages, so the Madani ayah positions are not mixed with these
  override fun getAyahInfoDirectoryName() = "naskh/databases"

  override fun getDatabasesBaseUrl() = madani.getDatabasesBaseUrl()

  override fun getAudioDatabasesBaseUrl() = madani.getAudioDatabasesBaseUrl()

  // its own folder, so the Madani pages stay where they are
  override fun getImagesDirectoryName() = "naskh"

  override fun getPreviewTitle() = R.string.naskh_title

  override fun getPreviewDescription() = R.string.naskh_description

  override fun getDefaultQariId(): Int = madani.getDefaultQariId()

  override fun getQaris(): List<Qari> = madani.getQaris()

  override fun pageType(): String = "naskh"

  companion object {
    private val dataSource by lazy { NaskhDataSource() }
    private const val baseUrl = "https://files.quran.app/hafs/naskh"
  }
}
