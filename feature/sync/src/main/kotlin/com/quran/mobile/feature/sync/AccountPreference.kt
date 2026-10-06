package com.quran.mobile.feature.sync

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.preference.Preference
import com.quran.labs.androidquran.common.ui.core.ComposePreferenceRow
import com.quran.labs.androidquran.common.ui.core.QuranTheme

/**
 * Preference row that renders [AccountSettingsRow] via Compose.
 *
 * Compose lets the row react to [QuranSyncManager.authState] directly, so the signed-in/signed-out
 * presentation (and each state's own click targets) stay current without a manual Preference
 * rebuild on fragment resume.
 */
internal class AccountPreference(
  context: Context,
  private val syncManager: QuranSyncManager
) : Preference(context), ComposePreferenceRow {

  init {
    isSelectable = false
    isIconSpaceReserved = false
  }

  @Composable
  override fun Content() {
    AccountSettingsRow(syncManager = syncManager)
  }
}
