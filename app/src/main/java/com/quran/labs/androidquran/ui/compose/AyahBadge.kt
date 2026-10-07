package com.quran.labs.androidquran.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import com.quran.labs.androidquran.ui.helpers.ReadingPalette
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The surah and ayah of the selection ("2:282") at the start of the ayah window's row, in a pill.
 */
@Composable
fun AyahBadge(label: String, palette: ReadingPalette) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .padding(start = 10.dp)
      .height(36.dp)
      .background(Color(palette.accent).copy(alpha = 0.18f), CircleShape)
      .padding(horizontal = 16.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = Color(palette.accent)
    )
  }
}
