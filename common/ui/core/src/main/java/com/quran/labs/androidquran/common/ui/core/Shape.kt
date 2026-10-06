package com.quran.labs.androidquran.common.ui.core

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The Material 3 Expressive shape scale - rounder than the baseline Material 3 scale for the
 * larger containers (cards, sheets and dialogs), which is most of what this app draws.
 */
val AppShapes = Shapes(
  extraSmall = RoundedCornerShape(4.dp),
  small = RoundedCornerShape(8.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(20.dp),
  extraLarge = RoundedCornerShape(32.dp),
)
