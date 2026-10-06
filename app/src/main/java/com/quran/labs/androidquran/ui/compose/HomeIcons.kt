package com.quran.labs.androidquran.ui.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** The two Material icons the home screen needs that [com.quran.labs.androidquran.common.ui.core.QuranIcons] lacks. */
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
