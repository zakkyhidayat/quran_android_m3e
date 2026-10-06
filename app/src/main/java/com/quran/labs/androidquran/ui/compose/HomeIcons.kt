package com.quran.labs.androidquran.ui.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** The Material icons the home screen needs that [com.quran.labs.androidquran.common.ui.core.QuranIcons] lacks. */
internal object HomeIcons {
  val MoreVert: ImageVector by lazy {
    icon(
      "Filled.MoreVert",
      "M12,8c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2zM12,10c-1.1,0 -2,0.9 -2,2s0.9,2 " +
        "2,2 2,-0.9 2,-2 -0.9,-2 -2,-2zM12,16c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z"
    )
  }

  val Sort: ImageVector by lazy {
    icon("Filled.Sort", "M3,18h6v-2L3,16v2zM3,6v2h18L21,6L3,6zM3,13h12v-2L3,11v2z")
  }

  val Delete: ImageVector by lazy {
    icon(
      "Filled.Delete",
      "M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z"
    )
  }

  val Edit: ImageVector by lazy {
    icon(
      "Filled.Edit",
      "M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25zM20.71,7.04c0.39,-0.39 0.39,-1.02 0,-1.41l-2.34,-2.34c-0.39,-0.39 -1.02,-0.39 -1.41,0l-1.83,1.83 3.75,3.75 1.83,-1.83z"
    )
  }

  val Label: ImageVector by lazy {
    icon(
      "Filled.Label",
      "M17.63,5.84C17.27,5.33 16.67,5 16,5L5,5.01C3.9,5.01 3,5.9 3,7v10c0,1.1 0.9,1.99 2,1.99L16,19c0.67,0 1.27,-0.33 1.63,-0.84L22,12l-4.37,-6.16z"
    )
  }

  val Add: ImageVector by lazy {
    icon("Filled.Add", "M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z")
  }

  private fun icon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
      name = name,
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).addPath(
      pathData = PathParser().parsePathString(pathData).toNodes(),
      fill = SolidColor(Color.Black)
    ).build()
}
