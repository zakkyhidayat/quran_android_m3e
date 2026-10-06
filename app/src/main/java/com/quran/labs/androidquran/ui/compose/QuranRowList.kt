package com.quran.labs.androidquran.ui.compose

import android.widget.ImageView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import com.quran.data.model.bookmark.Tag
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.CollectionNames
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.ui.helpers.QuranRow
import com.quran.labs.androidquran.util.QuranUtils
import com.quran.labs.androidquran.view.JuzView
import java.text.SimpleDateFormat
import java.util.Date

/**
 * The Material 3 replacement for the RecyclerView based `QuranListAdapter`. rows come straight
 * from the existing presenters, so data loading stays unchanged.
 *
 * the surah and juz tabs only need [onRowClick]. the bookmarks tab also passes [isEditable] (some
 * rows, like plain headers, aren't tappable there), [selectedIndices] and the long press / open
 * callbacks.
 */
@Composable
fun QuranRowList(
  rows: List<QuranRow>,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState(),
  contentPadding: PaddingValues = PaddingValues(0.dp),
  isEditable: Boolean = false,
  selectedIndices: Set<Int> = emptySet(),
  tagMap: Map<String, Tag> = emptyMap(),
  showTags: Boolean = false,
  showDate: Boolean = false,
  onRowLongClick: ((Int, QuranRow) -> Unit)? = null,
  onOpenClick: (Int, QuranRow) -> Unit = { _, _ -> },
  onRowClick: (Int, QuranRow) -> Unit
) {
  LazyColumn(
    modifier = modifier,
    state = listState,
    contentPadding = contentPadding
  ) {
    itemsIndexed(rows) { index, row ->
      val enabled = !isEditable || row.isTappableWhenEditable()
      val click: (() -> Unit)? = if (enabled) ({ onRowClick(index, row) }) else null
      val longClick: (() -> Unit)? =
        if (isEditable && enabled && onRowLongClick != null) ({ onRowLongClick(index, row) }) else null
      val selected = index in selectedIndices

      when {
        row.isBookmarkHeader || row.isHighlightsHeader ->
          CollectionHeader(row, selected, click, longClick) { onOpenClick(index, row) }

        row.isHeader -> SectionHeader(row, selected, click, longClick)
        row.isHighlightColor -> HighlightColorRow(row, selected, click, longClick) {
          onOpenClick(index, row)
        }

        else -> QuranRowItem(row, selected, tagMap, showTags, showDate, click, longClick)
      }
    }
  }
}

private fun QuranRow.isTappableWhenEditable(): Boolean =
  isBookmark || isReadingBookmark || rowType == QuranRow.NONE || isHighlightsHeader ||
    isHighlightColor || isHighlightedAyah || isBookmarkHeader

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectableRow(
  selected: Boolean,
  onClick: (() -> Unit)?,
  onLongClick: (() -> Unit)?,
  containerColor: Color,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  val color = if (selected) MaterialTheme.colorScheme.secondaryContainer else containerColor
  Surface(
    color = color,
    modifier = modifier
      .fillMaxWidth()
      .then(
        if (onClick != null) {
          Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
        } else {
          Modifier
        }
      )
  ) {
    content()
  }
}

@Composable
private fun SectionHeader(
  row: QuranRow,
  selected: Boolean,
  onClick: (() -> Unit)?,
  onLongClick: (() -> Unit)?
) {
  val trailing = row.itemCount ?: row.page
  SelectableRow(selected, onClick, onLongClick, MaterialTheme.colorScheme.surfaceContainerLow) {
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

/** A bookmark collection (or the highlights group): collapsible, with a chevron to open it. */
@Composable
private fun CollectionHeader(
  row: QuranRow,
  selected: Boolean,
  onClick: (() -> Unit)?,
  onLongClick: (() -> Unit)?,
  onOpen: () -> Unit
) {
  SelectableRow(selected, onClick, onLongClick, MaterialTheme.colorScheme.surfaceContainerLow) {
    Row(
      modifier = Modifier
        .heightIn(min = 48.dp)
        .padding(start = 16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (row.isCollapsible) {
        val rotation by animateFloatAsState(if (row.isCollapsed) 0f else 180f, label = "expand")
        Icon(
          imageVector = QuranIcons.ExpandMore,
          contentDescription = stringResource(
            if (row.isCollapsed) R.string.expand_collection else R.string.collapse_collection
          ),
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier
            .padding(end = 8.dp)
            .size(20.dp)
            .rotate(rotation)
        )
      }
      Text(
        text = row.text.orEmpty(),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f)
      )
      row.itemCount?.let {
        Text(
          text = QuranUtils.getLocalizedNumber(it),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(start = 8.dp)
        )
      }
      if (row.tagId != null) {
        IconButton(onClick = onOpen) {
          Icon(
            QuranIcons.ChevronRight,
            contentDescription = stringResource(R.string.open_collection),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      } else {
        Box(modifier = Modifier.size(16.dp))
      }
    }
  }
}

@Composable
private fun HighlightColorRow(
  row: QuranRow,
  selected: Boolean,
  onClick: (() -> Unit)?,
  onLongClick: (() -> Unit)?,
  onOpen: () -> Unit
) {
  val context = LocalContext.current
  val count = row.itemCount ?: 0
  // a color with nothing in it stays in the list, but dims rather than shouting
  val dim = if (count == 0) ResourcesCompat.getFloat(context.resources, R.dimen.empty_highlight_alpha) else 1f
  SelectableRow(selected, onClick, onLongClick, MaterialTheme.colorScheme.surface) {
    Row(
      modifier = Modifier
        .heightIn(min = 56.dp)
        .padding(start = 16.dp, end = 4.dp)
        .alpha(dim),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      val swatch = row.imageFilterColorResource?.let { colorResource(it) }
        ?: MaterialTheme.colorScheme.primary
      Box(
        modifier = Modifier
          .size(24.dp)
          .background(swatch, CircleShape)
      )
      Text(
        text = row.text.orEmpty(),
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f)
      )
      Text(
        text = QuranUtils.getLocalizedNumber(count),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      IconButton(onClick = onOpen) {
        Icon(QuranIcons.ChevronRight, contentDescription = null)
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuranRowItem(
  row: QuranRow,
  selected: Boolean,
  tagMap: Map<String, Tag>,
  showTags: Boolean,
  showDate: Boolean,
  onClick: (() -> Unit)?,
  onLongClick: (() -> Unit)?
) {
  val context = LocalContext.current
  // a bookmark row (as opposed to a surah / juz row) is the one with an icon of its own
  val isBookmarkRow = row.imageResource != null && row.juzType == null
  val metadata = if (showDate && isBookmarkRow && !row.isReadingBookmark) {
    val date = SimpleDateFormat("MMM dd, HH:mm", QuranUtils.getCurrentLocale())
      .format(Date(row.dateAddedInMillis))
    "${row.metadata} - $date"
  } else {
    row.metadata
  }
  val tags = if (showTags && isBookmarkRow) {
    row.bookmark?.tags.orEmpty().mapNotNull { tagMap[it] }
      .map { CollectionNames.displayName(context, it) }
  } else {
    emptyList()
  }

  SelectableRow(selected, onClick, onLongClick, MaterialTheme.colorScheme.surface) {
    Row(
      modifier = Modifier
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
        if (!metadata.isNullOrEmpty()) {
          Text(
            text = metadata,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }
        if (tags.isNotEmpty()) {
          FlowRow(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            tags.forEach { TagChip(it) }
          }
        }
      }

      val trailing = row.itemCount ?: row.page
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
private fun TagChip(name: String) {
  Text(
    text = name,
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.primary,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    modifier = Modifier
      .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
      .padding(horizontal = 8.dp, vertical = 2.dp)
  )
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
