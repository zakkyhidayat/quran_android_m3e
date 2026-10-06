package com.quran.labs.androidquran.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import android.widget.ImageView
import com.quran.labs.androidquran.ui.helpers.QuranRow
import com.quran.labs.androidquran.util.QuranUtils
import com.quran.labs.androidquran.view.JuzView

/**
 * The Material 3 replacement for the RecyclerView based `QuranListAdapter` on the surah and juz
 * tabs. rows come straight from the existing fragments, so data loading stays unchanged.
 */
@Composable
fun QuranRowList(
  rows: List<QuranRow>,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState(),
  contentPadding: PaddingValues = PaddingValues(0.dp),
  onRowClick: (QuranRow) -> Unit
) {
  LazyColumn(
    modifier = modifier,
    state = listState,
    contentPadding = contentPadding
  ) {
    itemsIndexed(rows) { _, row ->
      if (row.isHeader) {
        QuranSectionHeader(row)
      } else {
        QuranRowItem(row, onClick = { onRowClick(row) })
      }
    }
  }
}

@Composable
private fun QuranSectionHeader(row: QuranRow) {
  val trailing = row.itemCount ?: row.page
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .heightIn(min = 48.dp)
        .padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = row.text.orEmpty(),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.weight(1f)
      )
      if (trailing != 0) {
        Text(
          text = QuranUtils.getLocalizedNumber(trailing),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun QuranRowItem(row: QuranRow, onClick: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .heightIn(min = 72.dp)
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    QuranRowLeading(row)

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = row.text.orEmpty(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      if (!row.metadata.isNullOrEmpty()) {
        Text(
          text = row.metadata,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    if (row.page != 0) {
      Text(
        text = QuranUtils.getLocalizedNumber(row.page),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun QuranRowLeading(row: QuranRow) {
  val juzType = row.juzType
  val imageResource = row.imageResource
  when {
    juzType != null -> {
      val context = LocalContext.current
      AndroidView(
        modifier = Modifier.size(48.dp),
        factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER } },
        update = { it.setImageDrawable(JuzView(context, juzType, row.juzOverlayText)) }
      )
    }

    imageResource != null -> {
      val tint = row.imageFilterColorResource?.let { ColorFilter.tint(colorResource(it)) }
      Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Image(
          painter = painterResource(imageResource),
          contentDescription = row.imageContentDescription,
          colorFilter = tint
        )
      }
    }

    else -> {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.size(48.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = QuranUtils.getLocalizedNumber(row.sura),
            style = MaterialTheme.typography.titleMedium
          )
        }
      }
    }
  }
}
