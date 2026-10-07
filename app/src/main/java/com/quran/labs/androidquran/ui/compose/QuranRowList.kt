package com.quran.labs.androidquran.ui.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * The Material 3 Expressive replacement for the RecyclerView based `QuranListAdapter`.
 *
 * rows between two headers form one group: a run of segmented list items whose outer corners are
 * large and whose inner corners are small, so each group reads as a single rounded container.
 * the surah and juz tabs only need [onRowClick]; the bookmarks tab also passes [isEditable] (some
 * rows, like plain headers, aren't tappable there), [selectedIndices] and the long press / open
 * callbacks. With [separateCards] every row is its own fully rounded card instead.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QuranRowList(
  rows: List<QuranRow>,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState(),
  contentPadding: PaddingValues = PaddingValues(0.dp),
  isEditable: Boolean = false,
  separateCards: Boolean = false,
  highlightedSura: Int = 0,
  selectedIndices: Set<Int> = emptySet(),
  tagMap: Map<String, Tag> = emptyMap(),
  showTags: Boolean = false,
  showDate: Boolean = false,
  onRowLongClick: ((Int, QuranRow) -> Unit)? = null,
  onOpenClick: (Int, QuranRow) -> Unit = { _, _ -> },
  onRowClick: (Int, QuranRow) -> Unit
) {
  val groups = remember(rows) { segmentGroups(rows) }

  LazyColumn(
    modifier = modifier,
    state = listState,
    contentPadding = contentPadding,
    verticalArrangement = Arrangement.spacedBy(if (separateCards) 8.dp else ListItemDefaults.SegmentedGap)
  ) {
    itemsIndexed(rows) { index, row ->
      // each row arrives on a spring as it scrolls into view
      Box(Modifier.expressiveAppear()) {
      val enabled = !isEditable || row.isTappableWhenEditable()
      val click: () -> Unit = if (enabled) ({ onRowClick(index, row) }) else ({})
      val longClick: (() -> Unit)? =
        if (isEditable && enabled && onRowLongClick != null) ({ onRowLongClick(index, row) }) else null
      val selected = index in selectedIndices

      when {
        row.isBookmarkHeader || row.isHighlightsHeader ->
          CollectionHeader(row, selected, click, longClick) { onOpenClick(index, row) }

        row.isHeader -> SectionLabel(row)
        else -> {
          val shapes = if (separateCards) {
            ListItemDefaults.shapes(
              shape = RoundedCornerShape(24.dp),
              pressedShape = RoundedCornerShape(12.dp),
              selectedShape = RoundedCornerShape(24.dp)
            )
          } else {
            ListItemDefaults.segmentedShapes(
              index = groups.positionOf(index),
              count = groups.sizeOf(index)
            )
          }
          if (row.isHighlightColor) {
            HighlightColorRow(row, selected, shapes, click, longClick) { onOpenClick(index, row) }
          } else {
            val current = highlightedSura != 0 && row.sura == highlightedSura && row.isPlainSuraRow()
            QuranRowItem(row, selected, shapes, tagMap, showTags, showDate, current, click, longClick)
          }
        }
      }
      }
    }
  }
}

/** Where each row sits in its run of non-header rows, so the right corners can be rounded. */
private class SegmentGroups(private val position: IntArray, private val size: IntArray) {
  fun positionOf(index: Int) = position[index]
  fun sizeOf(index: Int) = size[index]
}

private fun segmentGroups(rows: List<QuranRow>): SegmentGroups {
  val position = IntArray(rows.size)
  val size = IntArray(rows.size)
  var start = 0
  while (start < rows.size) {
    if (rows[start].isHeader) {
      start++
      continue
    }
    var end = start
    while (end < rows.size && !rows[end].isHeader) end++
    for (i in start until end) {
      position[i] = i - start
      size[i] = end - start
    }
    start = end
  }
  return SegmentGroups(position, size)
}

/** A surah row of the surah tab, as opposed to a bookmark, a juz quarter or a header. */
private fun QuranRow.isPlainSuraRow(): Boolean =
  sura > 0 && juzType == null && imageResource == null && !isHeader

private fun QuranRow.isTappableWhenEditable(): Boolean =
  isBookmark || isReadingBookmark || rowType == QuranRow.NONE || isHighlightsHeader ||
    isHighlightColor || isHighlightedAyah || isBookmarkHeader

/** A juz heading: just a label above its group, rather than a band across the screen. */
@Composable
private fun SectionLabel(row: QuranRow) {
  val trailing = row.itemCount ?: row.page
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 40.dp)
      .padding(start = 28.dp, end = 28.dp, top = 12.dp, bottom = 0.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = row.text.orEmpty(),
      style = MaterialTheme.typography.titleMedium,
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

/** A bookmark collection (or the highlights group): collapsible, with a chevron to open it. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionHeader(
  row: QuranRow,
  selected: Boolean,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)?,
  onOpen: () -> Unit
) {
  // a section header like the others (flat, in the primary color), only with a chevron to fold it
  val source = remember { MutableInteractionSource() }
  Surface(
    shape = MaterialTheme.shapes.large,
    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp)
      .pressScale(source, 0.97f)
      .combinedClickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick, onLongClick = onLongClick)
  ) {
    Row(
      modifier = Modifier
        .heightIn(min = 48.dp)
        .padding(start = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (row.isCollapsible) {
        val rotation by animateFloatAsState(
          if (row.isCollapsed) 0f else 180f,
          animationSpec = spring(dampingRatio = 0.5f, stiffness = 420f),
          label = "expand"
        )
        Icon(
          imageVector = QuranIcons.ExpandMore,
          contentDescription = stringResource(
            if (row.isCollapsed) R.string.expand_collection else R.string.collapse_collection
          ),
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier
            .padding(end = 12.dp)
            .size(20.dp)
            .rotate(rotation)
        )
      }
      Text(
        text = row.text.orEmpty(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f)
      )
      row.itemCount?.let {
        Text(
          text = QuranUtils.getLocalizedNumber(it),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HighlightColorRow(
  row: QuranRow,
  selected: Boolean,
  shapes: ListItemShapes,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)?,
  onOpen: () -> Unit
) {
  val context = LocalContext.current
  val count = row.itemCount ?: 0
  // a color with nothing in it stays in the list, but dims rather than shouting
  val dim = if (count == 0) {
    ResourcesCompat.getFloat(context.resources, R.dimen.empty_highlight_alpha)
  } else {
    1f
  }
  SegmentedListItem(
    onClick = onClick,
    onLongClick = onLongClick,
    selected = selected,
    shapes = shapes,
    colors = ListItemDefaults.segmentedColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ),
    modifier = Modifier
      .padding(horizontal = 16.dp)
      .alpha(dim),
    leadingContent = {
      val swatch = row.imageFilterColorResource?.let { colorResource(it) }
        ?: MaterialTheme.colorScheme.primary
      Box(
        modifier = Modifier
          .size(32.dp)
          .background(swatch, MaterialShapes.Cookie6Sided.toShape())
      )
    },
    trailingContent = {
      Row(verticalAlignment = Alignment.CenterVertically) {
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
  ) {
    Text(
      text = row.text.orEmpty(),
      style = MaterialTheme.typography.titleMedium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
private fun QuranRowItem(
  row: QuranRow,
  selected: Boolean,
  shapes: ListItemShapes,
  tagMap: Map<String, Tag>,
  showTags: Boolean,
  showDate: Boolean,
  current: Boolean,
  onClick: () -> Unit,
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
  val trailing = row.itemCount ?: row.page

  // becoming (or ceasing to be) the surah last read fades the colors instead of switching them;
  // Material 3 Expressive moves color with its "effects" spring, not the spatial one
  val colorScheme = MaterialTheme.colorScheme
  val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
  val containerColor by animateColorAsState(
    if (current) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh,
    effects,
    label = "currentContainer"
  )
  val contentColor by animateColorAsState(
    if (current) colorScheme.onPrimaryContainer else colorScheme.onSurface,
    effects,
    label = "currentContent"
  )
  val supportingColor by animateColorAsState(
    if (current) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
    effects,
    label = "currentSupporting"
  )

  SegmentedListItem(
    onClick = onClick,
    onLongClick = onLongClick,
    selected = selected,
    shapes = shapes,
    colors = ListItemDefaults.segmentedColors(
      containerColor = containerColor,
      contentColor = contentColor,
      supportingContentColor = supportingColor
    ),
    modifier = Modifier.padding(horizontal = 16.dp),
    leadingContent = { QuranRowLeading(row, current) },
    supportingContent = if (metadata.isNullOrEmpty() && tags.isEmpty() && !current) {
      null
    } else {
      {
        Column {
          Crossfade(
            targetState = current,
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
            label = "lastReadLabel"
          ) { isCurrent ->
            if (isCurrent) {
              // on the metadata line itself, so the row keeps the height of the others
              Text(
                text = buildAnnotatedString {
                  withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                    append(stringResource(R.string.last_read_label))
                    append(" · ")
                  }
                  append(metadata.orEmpty())
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            } else if (!metadata.isNullOrEmpty()) {
              Text(text = metadata, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
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
      }
    },
    trailingContent = if (trailing != 0) {
      {
        Text(
          text = QuranUtils.getLocalizedNumber(trailing),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    } else {
      null
    }
  ) {
    // a long name scrolls sideways instead of being cut off
    Text(
      text = row.text.orEmpty(),
      style = MaterialTheme.typography.titleMedium,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Clip,
      modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 1500)
    )
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
private fun QuranRowLeading(row: QuranRow, current: Boolean = false) {
  val juzType = row.juzType
  val imageResource = row.imageResource
  when {
    juzType != null -> JuzProgressIcon(juzType, row.juzOverlayText)

    imageResource != null -> {
      val tint = row.imageFilterColorResource?.let { ColorFilter.tint(colorResource(it)) }
      Box(
        modifier = Modifier
          .size(48.dp)
          .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center
      ) {
        Image(
          painter = painterResource(imageResource),
          contentDescription = row.imageContentDescription,
          colorFilter = tint
        )
      }
    }

    else -> SuraNumberBadge(QuranUtils.getLocalizedNumber(row.sura), current)
  }
}

/** The surah number, in a scalloped "cookie" shape from the expressive shape library. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SuraNumberBadge(number: String, current: Boolean = false) {
  val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
  val badgeColor by animateColorAsState(
    if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
    effects,
    label = "badge"
  )
  val numberColor by animateColorAsState(
    if (current) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
    effects,
    label = "badgeNumber"
  )
  Box(
    modifier = Modifier
      .size(48.dp)
      .background(badgeColor, MaterialShapes.Cookie9Sided.toShape()),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = number,
      style = MaterialTheme.typography.titleMedium,
      color = numberColor
    )
  }
}

/**
 * The circle on the juz tab that fills up by quarters: a full juz, three quarters, a half or a
 * quarter. a plain Canvas, so scrolling the 240 rows doesn't allocate a drawable per bind.
 */
@Composable
private fun JuzProgressIcon(type: Int, overlayText: String?) {
  val fraction = when (type) {
    JuzView.TYPE_JUZ -> 1f
    JuzView.TYPE_THREE_QUARTERS -> 0.75f
    JuzView.TYPE_HALF -> 0.5f
    JuzView.TYPE_QUARTER -> 0.25f
    else -> 0f
  }
  val track = MaterialTheme.colorScheme.primaryContainer
  val fill = MaterialTheme.colorScheme.primary

  Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.size(40.dp)) {
      drawCircle(color = track)
      drawArc(
        color = fill,
        startAngle = -90f,
        sweepAngle = 360f * fraction,
        useCenter = true,
        topLeft = Offset.Zero,
        size = Size(size.width, size.height)
      )
    }
    if (!overlayText.isNullOrEmpty()) {
      Text(
        text = overlayText,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onPrimary
      )
    }
  }
}
