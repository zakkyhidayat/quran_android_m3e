package com.quran.labs.androidquran.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quran.labs.androidquran.ui.fragment.BookmarksEmptyState
import com.quran.mobile.feature.sync.BookmarksSignInCard
import com.quran.mobile.feature.sync.QuranSyncManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksTab(
  state: BookmarksState,
  actions: BookmarksActions,
  syncManager: QuranSyncManager,
  onSignIn: () -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(0.dp)
) {
  val canSync by syncManager.canTriggerSyncFlow.collectAsState(initial = syncManager.canTriggerSync)
  var refreshing by remember { mutableStateOf(false) }
  // a sync runs in the background, so the indicator only acknowledges the pull
  LaunchedEffect(refreshing) {
    if (refreshing) {
      delay(800)
      refreshing = false
    }
  }

  Column(modifier = modifier.fillMaxSize()) {
    BookmarksSignInCard(syncManager = syncManager, onSignIn = onSignIn)

    Box(modifier = Modifier.weight(1f)) {
      if (state.isLoaded && state.rows.isEmpty()) {
        BookmarksEmptyState()
      } else {
        val list: @Composable () -> Unit = {
          QuranRowList(
            rows = state.rows,
            isEditable = true,
            selectedIndices = state.selectedIndices,
            tagMap = state.tagMap,
            showTags = state.showTags,
            showDate = state.isDateShowing,
            contentPadding = contentPadding,
            onRowClick = { index, row -> state.onRowClick(index, row, actions) },
            onRowLongClick = state::onRowLongClick,
            onOpenClick = { index, row -> state.onOpenClick(index, row, actions) }
          )
        }

        if (canSync) {
          PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
              if (syncManager.canTriggerSync) syncManager.triggerSync()
              refreshing = true
            },
            modifier = Modifier.fillMaxSize()
          ) { list() }
        } else {
          list()
        }
      }
    }
  }
}
