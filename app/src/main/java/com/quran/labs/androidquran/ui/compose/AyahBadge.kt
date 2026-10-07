package com.quran.labs.androidquran.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The surah and ayah of the selection ("2:282") at the start of the ayah window's row, on a
 * scalloped shape from the expressive shape library, like the surah numbers in the lists.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AyahBadge(label: String) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .padding(start = 10.dp)
      .heightIn(min = 40.dp)
      .widthIn(min = 52.dp)
      .background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Cookie12Sided.toShape())
      .padding(horizontal = 10.dp, vertical = 8.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onPrimaryContainer
    )
  }
}
