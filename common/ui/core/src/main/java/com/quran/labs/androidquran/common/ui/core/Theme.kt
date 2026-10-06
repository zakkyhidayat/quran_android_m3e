package com.quran.labs.androidquran.common.ui.core

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val LightColors = lightColorScheme(
  primary = lightPrimary,
  onPrimary = lightOnPrimary,
  primaryContainer = lightPrimaryContainer,
  onPrimaryContainer = lightOnPrimaryContainer,
  secondary = lightSecondary,
  onSecondary = lightOnSecondary,
  secondaryContainer = lightSecondaryContainer,
  onSecondaryContainer = lightOnSecondaryContainer,
  tertiary = lightTertiary,
  onTertiary = lightOnTertiary,
  tertiaryContainer = lightTertiaryContainer,
  onTertiaryContainer = lightOnTertiaryContainer,
  error = lightError,
  errorContainer = lightErrorContainer,
  onError = lightOnError,
  onErrorContainer = lightOnErrorContainer,
  background = lightBackground,
  onBackground = lightOnBackground,
  surface = lightSurface,
  onSurface = lightOnSurface,
  surfaceVariant = lightSurfaceVariant,
  onSurfaceVariant = lightOnSurfaceVariant,
  outline = lightOutline,
  outlineVariant = lightOutlineVariant,
  inverseOnSurface = lightInverseOnSurface,
  inverseSurface = lightInverseSurface,
  inversePrimary = lightInversePrimary,
  surfaceTint = lightSurfaceTint,
  scrim = lightShadow,
  surfaceDim = lightSurfaceDim,
  surfaceBright = lightSurfaceBright,
  surfaceContainerLowest = lightSurfaceContainerLowest,
  surfaceContainerLow = lightSurfaceContainerLow,
  surfaceContainer = lightSurfaceContainer,
  surfaceContainerHigh = lightSurfaceContainerHigh,
  surfaceContainerHighest = lightSurfaceColorHighest
)

private val DarkColors = darkColorScheme(
  primary = darkPrimary,
  onPrimary = darkOnPrimary,
  primaryContainer = darkPrimaryContainer,
  onPrimaryContainer = darkOnPrimaryContainer,
  secondary = darkSecondary,
  onSecondary = darkOnSecondary,
  secondaryContainer = darkSecondaryContainer,
  onSecondaryContainer = darkOnSecondaryContainer,
  tertiary = darkTertiary,
  onTertiary = darkOnTertiary,
  tertiaryContainer = darkTertiaryContainer,
  onTertiaryContainer = darkOnTertiaryContainer,
  error = darkError,
  errorContainer = darkErrorContainer,
  onError = darkOnError,
  onErrorContainer = darkOnErrorContainer,
  background = darkBackground,
  onBackground = darkOnBackground,
  surface = darkSurface,
  onSurface = darkOnSurface,
  surfaceVariant = darkSurfaceVariant,
  onSurfaceVariant = darkOnSurfaceVariant,
  outline = darkOutline,
  outlineVariant = darkOutlineVariant,
  inverseOnSurface = darkInverseOnSurface,
  inverseSurface = darkInverseSurface,
  inversePrimary = darkInversePrimary,
  surfaceTint = darkSurfaceTint,
  scrim = darkShadow,
  surfaceDim = darkSurfaceDim,
  surfaceBright = darkSurfaceBright,
  surfaceContainerLowest = darkSurfaceContainerLowest,
  surfaceContainerLow = darkSurfaceContainerLow,
  surfaceContainer = darkSurfaceContainer,
  surfaceContainerHigh = darkSurfaceContainerHigh,
  surfaceContainerHighest = darkSurfaceColorHighest
)

/**
 * The same scheme on true black: the surfaces step up from black in small increments, so cards
 * and containers still read as layers instead of vanishing into the background.
 */
private fun ColorScheme.toAmoled(): ColorScheme = copy(
  background = Color.Black,
  surface = Color.Black,
  surfaceDim = Color.Black,
  surfaceContainerLowest = Color.Black,
  surfaceContainerLow = Color(0xFF0A0A0A),
  surfaceContainer = Color(0xFF111111),
  surfaceContainerHigh = Color(0xFF181818),
  surfaceContainerHighest = Color(0xFF202020),
  surfaceBright = Color(0xFF262626)
)

private val forceLtr = listOf("huawei", "lenovo", "tecno")

object QuranIcons {
  val ArrowBack: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.ArrowBack
  val BookmarkBorder: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.BookmarkBorder
  val Chat: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Chat
  val Check: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Check
  val ChevronRight: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.ChevronRight
  val Close: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Close
  val ExpandMore: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.ExpandMore
  val FastForward: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.FastForward
  val FastRewind: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.FastRewind
  val Logout: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Logout
  val MenuBook: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.MenuBook
  val Mic: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Mic
  val Pause: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Pause
  val Person: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Person
  val PlayArrow: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.PlayArrow
  val Repeat: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Repeat
  val Search: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Search
  val Settings: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Settings
  val Speed: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Speed
  val Stop: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Stop
  val Sync: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Sync
}

/**
 * The app's Material 3 Expressive theme.
 *
 * @param useDarkTheme whether to use the dark color scheme
 * @param useDynamicColor whether to use the wallpaper based (Material You) color scheme instead of
 * the original Quran palette. this only has an effect on Android 12+, older versions always get the
 * original palette. defaults to the user's choice in [QuranThemeSettings].
 */
@Composable
fun QuranTheme(
  useDarkTheme: Boolean = isSystemInDarkTheme(),
  useDynamicColor: Boolean = QuranThemeSettings.useDynamicColor,
  content: @Composable () -> Unit
) {
  val context = LocalContext.current
  val baseColors: ColorScheme =
    if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (useDarkTheme) {
      DarkColors
    } else {
      LightColors
    }
  val colors = if (useDarkTheme && QuranThemeSettings.useAmoled) baseColors.toAmoled() else baseColors

  val quranColors = if (useDarkTheme) {
    darkQuranColors
  } else {
    lightQuranColors
  }

  // hack workaround for https://issuetracker.google.com/issues/266059178
  // crashes on Lollipop / MR1, mostly on Huawei and Lenovo devices due to
  // Compose in RTL. Force LTR for all Composables on Lollipop to attempt
  // to work around this crash for now.
  val locals =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M &&
      Build.MANUFACTURER.lowercase() in forceLtr
    ) {
      arrayOf(LocalLayoutDirection provides LayoutDirection.Ltr)
    } else {
      emptyArray()
    }

  CompositionLocalProvider(*locals) {
    CompositionLocalProvider(LocalQuranColors provides quranColors) {
      MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        shapes = AppShapes,
        typography = AppTypography,
        content = content
      )
    }
  }
}
