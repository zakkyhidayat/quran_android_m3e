package com.quran.labs.androidquran.ui.compose

import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.aspectRatio
import androidx.activity.compose.BackHandler
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.QuranThemeSettings
import com.quran.labs.androidquran.common.ui.core.darkPrimary
import com.quran.labs.androidquran.common.ui.core.darkPrimaryContainer
import com.quran.labs.androidquran.common.ui.core.darkTertiary
import com.quran.labs.androidquran.common.ui.core.lightPrimary
import com.quran.labs.androidquran.common.ui.core.lightPrimaryContainer
import com.quran.labs.androidquran.common.ui.core.lightTertiary
import com.quran.labs.androidquran.dao.translation.TranslationItem
import com.quran.labs.androidquran.data.Constants
import com.quran.labs.androidquran.ui.TranslationDownloads

/** Where the download of the page images is. */
sealed interface PagesDownload {
  data object NotStarted : PagesDownload
  data object Waiting : PagesDownload
  data class Downloading(val progress: Int, val downloaded: String, val total: String) : PagesDownload
  data class Unpacking(val progress: Int) : PagesDownload
  data object Done : PagesDownload
  data class Failed(val message: String) : PagesDownload
}

/** One mushaf layout that can be downloaded and read with. */
class PageStyleItem(
  val key: String,
  @StringRes val title: Int,
  @StringRes val description: Int,
  val inUse: Boolean,
  val downloaded: Boolean
)

/** What the setup shows. The activity keeps it in step with the settings it changes. */
@Stable
class OnboardingState {
  var pages by mutableStateOf<PagesDownload>(PagesDownload.NotStarted)
  var theme by mutableStateOf(Constants.THEME_DEFAULT)
  var amoled by mutableStateOf(false)
  var dynamicColor by mutableStateOf(false)
  var dualPage by mutableStateOf(false)
  var splitTranslation by mutableStateOf(false)
  var dualPageAvailable by mutableStateOf(false)
  var arabic by mutableStateOf(false)
  var dyslexicFont by mutableStateOf(false)
  var arabicBeforeTranslation by mutableStateOf(true)
  var translationsFailed by mutableStateOf(false)
  var pageStyles by mutableStateOf<List<PageStyleItem>>(emptyList())
  var pagePreviews by mutableStateOf<Map<String, ImageBitmap>>(emptyMap())
}

/** What the setup can change. The activity owns what each of these does. */
class OnboardingActions(
  val onDownloadPages: () -> Unit,
  val onTheme: (String) -> Unit,
  val onAmoled: (Boolean) -> Unit,
  val onDynamicColor: (Boolean) -> Unit,
  val onDualPage: (Boolean) -> Unit,
  val onSplitTranslation: (Boolean) -> Unit,
  val onArabic: (Boolean) -> Unit,
  val onDyslexicFont: (Boolean) -> Unit,
  val onArabicBeforeTranslation: (Boolean) -> Unit,
  val onTranslationsShown: () -> Unit,
  val onUsePageStyle: (String) -> Unit,
  val onDownloadPageStyle: (String) -> Unit,
  val onRemovePageStyle: (String) -> Unit,
  val onFinish: () -> Unit
)

private enum class Step(@StringRes val title: Int, @StringRes val body: Int) {
  WELCOME(R.string.onboarding_welcome_title, R.string.onboarding_welcome_body),
  DATA(R.string.onboarding_data_title, R.string.onboarding_data_body),
  THEME(R.string.onboarding_theme_title, R.string.onboarding_theme_body),
  LANGUAGE(R.string.onboarding_language_title, R.string.onboarding_language_body),
  FONT(R.string.onboarding_font_title, R.string.onboarding_font_body),
  ORDER(R.string.onboarding_order_title, R.string.onboarding_order_body),
  TRANSLATION(R.string.onboarding_translation_title, R.string.onboarding_translation_body),
  DUAL(R.string.onboarding_dual_title, R.string.onboarding_dual_body),
  PAGES(R.string.onboarding_pages_title, R.string.onboarding_pages_body)
}

/**
 * The first-run setup: one decision per screen, each of which can be skipped, with everything
 * saved the moment it is chosen so that leaving early loses nothing.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen(
  state: OnboardingState,
  translations: TranslationDownloads,
  actions: OnboardingActions
) {
  // two pages side by side only makes sense on a screen wide enough to hold them
  val steps = remember(state.dualPageAvailable) {
    Step.entries.filter { it != Step.DUAL || state.dualPageAvailable }
  }
  // saveable, since changing the theme or the language recreates the activity
  var index by rememberSaveable { mutableIntStateOf(0) }
  val step = steps[index]
  val isLast = index == steps.lastIndex
  val next: () -> Unit = { if (isLast) actions.onFinish() else index++ }

  BackHandler(enabled = index > 0) { index-- }
  LaunchedEffect(step) {
    if (step == Step.TRANSLATION) actions.onTranslationsShown()
  }

  Scaffold(
    containerColor = arrivalBackground(MaterialTheme.colorScheme.surface),
    contentWindowInsets = WindowInsets(0),
    topBar = {
      if (step != Step.WELCOME) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .statusBarsPadding()
            .padding(start = 24.dp, end = 8.dp, top = 8.dp)
        ) {
          LinearWavyProgressIndicator(
            progress = { index / (steps.size - 1f) },
            modifier = Modifier.weight(1f)
          )
          TextButton(onClick = actions.onFinish, modifier = Modifier.padding(start = 8.dp)) {
            Text(stringResource(R.string.onboarding_skip_all))
          }
        }
      }
    },
    bottomBar = {
      OnboardingButtons(
        step = step,
        isLast = isLast,
        pages = state.pages,
        onBack = { index-- },
        onNext = next,
        onSkipAll = actions.onFinish
      )
    }
  ) { innerPadding ->
    AnimatedContent(
      targetState = index,
      transitionSpec = {
        val forward = targetState > initialState
        (slideInHorizontally(spring(dampingRatio = 0.75f, stiffness = 380f)) { if (forward) it / 3 else -it / 3 } + fadeIn())
          .togetherWith(slideOutHorizontally { if (forward) -it / 4 else it / 4 } + fadeOut())
      },
      label = "onboarding step",
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) { shown ->
      val shownStep = steps[shown]
      if (shownStep == Step.TRANSLATION) {
        TranslationStep(state, translations)
      } else {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
        ) {
          StepHeader(shownStep, centered = true)
          Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
              .widthIn(max = 560.dp)
              .fillMaxWidth()
              .padding(top = 28.dp, bottom = 24.dp)
          ) {
            when (shownStep) {
              Step.WELCOME -> Unit
              Step.DATA -> PagesDownloadCard(state.pages, actions.onDownloadPages)
              Step.THEME -> ThemeChoice(state, actions)
              Step.LANGUAGE -> SwitchCard(
                title = stringResource(R.string.prefs_use_arabic_title),
                summary = null,
                checked = state.arabic,
                onCheckedChange = actions.onArabic
              )

              Step.FONT -> FontChoice(state, actions)
              Step.ORDER -> OrderChoice(state, actions)
              Step.DUAL -> DualPageChoice(state, actions)
              Step.PAGES -> PageStyles(state, actions)
              Step.TRANSLATION -> Unit
            }
          }
        }
      }
    }
  }
}

private fun Step.icon(): ImageVector = when (this) {
  Step.WELCOME -> QuranIcons.MenuBook
  Step.DATA -> HomeIcons.Download
  Step.THEME -> HomeIcons.Palette
  Step.LANGUAGE -> HomeIcons.Language
  Step.FONT -> HomeIcons.TextFields
  Step.ORDER -> HomeIcons.SwapVert
  Step.TRANSLATION -> HomeIcons.Translate
  Step.DUAL -> HomeIcons.Columns
  Step.PAGES -> QuranIcons.MenuBook
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Step.shape(): Shape = when (this) {
  Step.WELCOME -> MaterialShapes.Cookie9Sided
  Step.DATA -> MaterialShapes.Sunny
  Step.THEME -> MaterialShapes.Flower
  Step.LANGUAGE -> MaterialShapes.Clover4Leaf
  Step.FONT -> MaterialShapes.Cookie6Sided
  Step.ORDER -> MaterialShapes.SoftBurst
  Step.TRANSLATION -> MaterialShapes.Cookie12Sided
  Step.DUAL -> MaterialShapes.Cookie4Sided
  Step.PAGES -> MaterialShapes.Gem
}.toShape()

/** The shape, the title and the explanation at the top of every step. */
@Composable
private fun StepHeader(step: Step, centered: Boolean) {
  val heroSize = if (step == Step.WELCOME) 168.dp else 104.dp
  Column(
    horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    modifier = Modifier
      .widthIn(max = 560.dp)
      .fillMaxWidth()
      .padding(top = if (step == Step.WELCOME) 96.dp else 32.dp)
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(heroSize)
        .background(MaterialTheme.colorScheme.primaryContainer, step.shape())
    ) {
      Icon(
        step.icon(),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(heroSize * 0.42f)
      )
    }
    Text(
      text = stringResource(step.title),
      style = if (step == Step.WELCOME) {
        MaterialTheme.typography.displaySmall
      } else {
        MaterialTheme.typography.headlineMedium
      },
      fontWeight = FontWeight.SemiBold,
      textAlign = if (centered) TextAlign.Center else TextAlign.Start,
      modifier = Modifier.padding(top = 28.dp)
    )
    Text(
      text = stringResource(step.body),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = if (centered) TextAlign.Center else TextAlign.Start,
      modifier = Modifier.padding(top = 12.dp)
    )
  }
}

@Composable
private fun OnboardingButtons(
  step: Step,
  isLast: Boolean,
  pages: PagesDownload,
  onBack: () -> Unit,
  onNext: () -> Unit,
  onSkipAll: () -> Unit
) {
  val bigButton = Modifier.heightIn(min = 56.dp)
  if (step == Step.WELCOME) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
      Button(
        onClick = onNext,
        modifier = bigButton
          .widthIn(max = 560.dp)
          .fillMaxWidth()
      ) {
        Text(stringResource(R.string.onboarding_get_started), style = MaterialTheme.typography.titleMedium)
      }
      TextButton(onClick = onSkipAll, modifier = Modifier.padding(top = 8.dp)) {
        Text(stringResource(R.string.onboarding_skip_all))
      }
    }
    return
  }

  // on the download step, moving on without downloading is a skip, and is called one
  val nextLabel = when {
    isLast -> R.string.onboarding_finish
    step == Step.DATA && pages == PagesDownload.NotStarted -> R.string.onboarding_skip
    else -> R.string.onboarding_next
  }
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 24.dp, vertical = 16.dp)
  ) {
    OutlinedButton(onClick = onBack, modifier = bigButton) {
      Text(stringResource(R.string.onboarding_back))
    }
    Spacer(Modifier.weight(1f))
    Button(
      onClick = onNext,
      contentPadding = PaddingValues(horizontal = 32.dp),
      modifier = bigButton
    ) {
      Text(stringResource(nextLabel), style = MaterialTheme.typography.titleSmall)
    }
  }
}

/** A rounded container, the building block of every choice in the setup. */
@Composable
private fun ChoiceCard(
  modifier: Modifier = Modifier,
  selected: Boolean = false,
  onClick: (() -> Unit)? = null,
  unselectedColor: Color = Color.Unspecified,
  content: @Composable ColumnScope.() -> Unit
) {
  // the card gives way under the finger and pops when it becomes the chosen one
  val source = remember { MutableInteractionSource() }
  val pressed by source.collectIsPressedAsState()
  val corner by animateDpAsState(
    targetValue = if (pressed) 16.dp else 24.dp,
    animationSpec = spring(dampingRatio = 0.5f, stiffness = 520f),
    label = "corner"
  )
  val pressScale by animateFloatAsState(
    targetValue = if (pressed) 0.97f else 1f,
    animationSpec = spring(dampingRatio = 0.5f, stiffness = 520f),
    label = "pressScale"
  )
  val pop = remember { Animatable(1f) }
  var firstSelection by remember { mutableStateOf(true) }
  LaunchedEffect(selected) {
    if (firstSelection) {
      firstSelection = false
    } else if (selected) {
      pop.snapTo(0.95f)
      pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 450f))
    }
  }
  val target = if (selected) {
    MaterialTheme.colorScheme.secondaryContainer
  } else if (unselectedColor != Color.Unspecified) {
    unselectedColor
  } else {
    MaterialTheme.colorScheme.surfaceContainerHigh
  }
  val color by animateColorAsState(target, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "cardColor")
  val borderColor by animateColorAsState(
    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
    MaterialTheme.motionScheme.defaultEffectsSpec(),
    label = "cardBorder"
  )
  val shape = RoundedCornerShape(corner)
  val border = BorderStroke(2.dp, borderColor)
  val scaleModifier = Modifier.graphicsLayer {
    scaleX = pressScale * pop.value
    scaleY = pressScale * pop.value
  }
  val inner: @Composable () -> Unit = {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), content = content)
  }
  if (onClick != null) {
    Surface(
      onClick = onClick,
      interactionSource = source,
      shape = shape,
      color = color,
      border = border,
      modifier = modifier.fillMaxWidth().then(scaleModifier)
    ) {
      inner()
    }
  } else {
    Surface(shape = shape, color = color, border = border, modifier = modifier.fillMaxWidth().then(scaleModifier)) {
      inner()
    }
  }
}

@Composable
private fun SwitchCard(
  title: String,
  summary: String?,
  checked: Boolean,
  enabled: Boolean = true,
  onCheckedChange: (Boolean) -> Unit
) {
  ChoiceCard(onClick = { if (enabled) onCheckedChange(!checked) }) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text(
          title,
          style = MaterialTheme.typography.titleMedium,
          color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
        )
        if (summary != null) {
          Text(
            summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
          )
        }
      }
      Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        thumbContent = if (checked) {
          { Icon(QuranIcons.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
        } else {
          null
        },
        modifier = Modifier.padding(start = 16.dp)
      )
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PagesDownloadCard(pages: PagesDownload, onDownload: () -> Unit) {
  ChoiceCard {
    when (pages) {
      PagesDownload.NotStarted -> FilledTonalButton(
        onClick = onDownload,
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 56.dp)
      ) {
        Icon(HomeIcons.Download, contentDescription = null)
        Text(stringResource(R.string.onboarding_data_download), modifier = Modifier.padding(start = 8.dp))
      }

      PagesDownload.Waiting -> ProgressRow(null, stringResource(R.string.onboarding_data_waiting))
      is PagesDownload.Downloading -> ProgressRow(
        pages.progress.takeIf { it >= 0 },
        stringResource(R.string.onboarding_data_progress, pages.downloaded, pages.total)
      )

      is PagesDownload.Unpacking -> ProgressRow(
        pages.progress.takeIf { it >= 0 },
        stringResource(R.string.onboarding_data_unpacking)
      )

      PagesDownload.Done -> Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(HomeIcons.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
          stringResource(R.string.onboarding_data_done),
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(start = 12.dp)
        )
      }

      is PagesDownload.Failed -> {
        Text(pages.message, color = MaterialTheme.colorScheme.error)
        FilledTonalButton(onClick = onDownload, modifier = Modifier.padding(top = 12.dp)) {
          Text(stringResource(R.string.onboarding_retry))
        }
      }
    }
  }
  if (pages is PagesDownload.Downloading || pages is PagesDownload.Unpacking || pages == PagesDownload.Waiting) {
    Text(
      stringResource(R.string.onboarding_data_background),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth()
    )
  }
}

/**
 * Asking for the pages outside of the first-run setup (they went missing, or were skipped before):
 * a page of its own, with the progress right in it, instead of a dialog over a dimmed screen.
 */
@Composable
fun PagesDownloadScreen(state: OnboardingState, onDownload: () -> Unit, onSkip: () -> Unit) {
  Scaffold(
    containerColor = arrivalBackground(MaterialTheme.colorScheme.surface),
    contentWindowInsets = WindowInsets(0)
  ) { padding ->
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 24.dp)
    ) {
      StepHeader(Step.DATA, centered = true)
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
          .widthIn(max = 560.dp)
          .fillMaxWidth()
          .padding(top = 28.dp, bottom = 24.dp)
      ) {
        PagesDownloadCard(state.pages, onDownload)
        if (state.pages == PagesDownload.NotStarted || state.pages is PagesDownload.Failed) {
          TextButton(onClick = onSkip, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.onboarding_skip))
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProgressRow(progress: Int?, label: String) {
  if (progress == null) {
    LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
  } else {
    LinearWavyProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
  }
  Text(
    label,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(top = 12.dp)
  )
}

@Composable
private fun ThemeChoice(state: OnboardingState, actions: OnboardingActions) {
  val options = listOf(
    Constants.THEME_DEFAULT to R.string.onboarding_theme_system,
    Constants.THEME_LIGHT to R.string.onboarding_theme_light,
    Constants.THEME_DARK to R.string.onboarding_theme_dark
  )
  SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
    options.forEachIndexed { index, (value, label) ->
      SegmentedButton(
        selected = state.theme == value,
        onClick = { actions.onTheme(value) },
        shape = SegmentedButtonDefaults.itemShape(index, options.size),
        label = { Text(stringResource(label), maxLines = 1) },
        modifier = Modifier.heightIn(min = 52.dp)
      )
    }
  }
  if (QuranThemeSettings.isDynamicColorAvailable) {
    ColorSchemeChoice(state, actions)
  }
  SwitchCard(
    title = stringResource(R.string.prefs_amoled_title),
    summary = stringResource(R.string.onboarding_amoled_summary),
    checked = state.amoled,
    onCheckedChange = actions.onAmoled
  )
}

/** Wallpaper colors or the original emerald, each shown by the swatches it would paint the app with. */
@Composable
private fun ColorSchemeChoice(state: OnboardingState, actions: OnboardingActions) {
  ColorSchemeOptions(state.dynamicColor, showTitle = true, onSelect = actions.onDynamicColor)
}

@Composable
internal fun ColorSchemeOptions(
  dynamic: Boolean,
  showTitle: Boolean,
  unselectedColor: Color = Color.Unspecified,
  onSelect: (Boolean) -> Unit
) {
  val context = LocalContext.current
  val dark = isSystemInDarkTheme()
  val original = if (dark) darkSwatches else lightSwatches
  val dynamicSwatches = remember(dark) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val scheme = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      listOf(scheme.primary, scheme.primaryContainer, scheme.tertiary)
    } else {
      original
    }
  }
  if (showTitle) {
    Text(
      stringResource(R.string.onboarding_colors_title),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(top = 8.dp, start = 4.dp)
    )
  }
  ColorOption(
    selected = !dynamic,
    title = R.string.onboarding_colors_original,
    summary = R.string.onboarding_colors_original_summary,
    swatches = original,
    unselectedColor = unselectedColor
  ) { onSelect(false) }
  ColorOption(
    selected = dynamic,
    title = R.string.onboarding_colors_dynamic,
    summary = R.string.onboarding_colors_dynamic_summary,
    swatches = dynamicSwatches,
    unselectedColor = unselectedColor
  ) { onSelect(true) }
}

private val lightSwatches = listOf(lightPrimary, lightPrimaryContainer, lightTertiary)
private val darkSwatches = listOf(darkPrimary, darkPrimaryContainer, darkTertiary)

@Composable
private fun ColorOption(
  selected: Boolean,
  @StringRes title: Int,
  @StringRes summary: Int,
  swatches: List<Color>,
  unselectedColor: Color,
  onClick: () -> Unit
) {
  ChoiceCard(selected = selected, onClick = onClick, unselectedColor = unselectedColor) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(
          stringResource(summary),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 2.dp)
        )
      }
      Row(
        horizontalArrangement = Arrangement.spacedBy((-8).dp),
        modifier = Modifier.padding(start = 12.dp)
      ) {
        swatches.forEach { color ->
          Box(
            Modifier
              .size(28.dp)
              .background(color, CircleShape)
              .border(2.dp, MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
          )
        }
      }
    }
  }
}

/** Two pages side by side when the phone is held sideways, drawn so the difference is visible. */
@Composable
private fun DualPageChoice(state: OnboardingState, actions: OnboardingActions) {
  val basmalah = state.pagePreviews.values.firstOrNull()
  Surface(
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(2.1f)
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
        .fillMaxSize()
        .padding(14.dp)
    ) {
      if (!state.dualPage) Spacer(Modifier.weight(0.5f))
      PaperPage(basmalah, Modifier.weight(1f))
      when {
        state.dualPage && state.splitTranslation -> TranslationPaper(Modifier.weight(1f))
        state.dualPage -> PaperPage(null, Modifier.weight(1f))
        else -> Spacer(Modifier.weight(0.5f))
      }
    }
  }
  SwitchCard(
    title = stringResource(R.string.prefs_dual_page_mode_title),
    summary = stringResource(R.string.onboarding_dual_summary),
    checked = state.dualPage,
    onCheckedChange = actions.onDualPage
  )
  SwitchCard(
    title = stringResource(R.string.prefs_split_page_and_translation_title),
    summary = stringResource(R.string.onboarding_split_summary),
    checked = state.splitTranslation && state.dualPage,
    enabled = state.dualPage,
    onCheckedChange = actions.onSplitTranslation
  )
}

/** The second half of the spread when the translation takes the place of the second page. */
@Composable
private fun TranslationPaper(modifier: Modifier) {
  val ink = MaterialTheme.colorScheme.onSurface
  Column(
    verticalArrangement = Arrangement.spacedBy(6.dp),
    modifier = modifier
      .fillMaxHeight()
      .clipToBounds()
      .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp)
  ) {
    Box(
      Modifier
        .width(44.dp)
        .height(14.dp)
        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(7.dp))
    )
    listOf(1f, 0.94f, 0.98f, 0.5f, 1f, 0.92f, 0.96f).forEachIndexed { i, fraction ->
      Box(
        Modifier
          .fillMaxWidth(fraction)
          .height(5.dp)
          .padding(top = if (i == 4) 4.dp else 0.dp)
          .background(ink.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
      )
    }
  }
}

/** One page of paper: the Basmalah when there is one on the phone, then lines of text. */
@Composable
private fun PaperPage(preview: ImageBitmap?, modifier: Modifier) {
  val ink = Color(0xFF1B1B1B)
  Column(
    verticalArrangement = Arrangement.spacedBy(6.dp),
    modifier = modifier
      .fillMaxHeight()
      .clipToBounds()
      .background(Color(0xFFFDFBEF), RoundedCornerShape(10.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp)
  ) {
    if (preview != null) {
      Image(
        bitmap = preview,
        contentDescription = null,
        contentScale = ContentScale.FillWidth,
        colorFilter = ColorFilter.tint(ink),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp)
      )
    }
    listOf(1f, 0.96f, 1f, 0.92f, 1f, 0.98f, 0.9f, 1f, 0.6f).forEach { fraction ->
      Box(
        Modifier
          .align(Alignment.End)
          .fillMaxWidth(fraction)
          .height(5.dp)
          .background(ink.copy(alpha = 0.55f), RoundedCornerShape(3.dp))
      )
    }
  }
}

@Composable
private fun FontChoice(state: OnboardingState, actions: OnboardingActions) {
  val context = LocalContext.current
  val dyslexic = remember { FontFamily(Font("OpenDyslexic.otf", context.assets)) }
  SwitchCard(
    title = stringResource(R.string.prefs_use_dyslexic_font_title),
    summary = null,
    checked = state.dyslexicFont,
    onCheckedChange = actions.onDyslexicFont
  )
  ChoiceCard {
    Text(
      stringResource(R.string.prefs_preview),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.primary
    )
    Text(
      stringResource(R.string.onboarding_font_sample),
      style = MaterialTheme.typography.bodyLarge,
      fontFamily = if (state.dyslexicFont) dyslexic else null,
      modifier = Modifier.padding(top = 8.dp)
    )
  }
}

@Composable
private fun OrderChoice(state: OnboardingState, actions: OnboardingActions) {
  OrderOption(
    selected = state.arabicBeforeTranslation,
    title = R.string.onboarding_order_arabic_first,
    summary = R.string.onboarding_order_arabic_first_summary
  ) { actions.onArabicBeforeTranslation(true) }
  OrderOption(
    selected = !state.arabicBeforeTranslation,
    title = R.string.onboarding_order_translation_only,
    summary = R.string.onboarding_order_translation_only_summary
  ) { actions.onArabicBeforeTranslation(false) }
}

@Composable
private fun OrderOption(selected: Boolean, @StringRes title: Int, @StringRes summary: Int, onClick: () -> Unit) {
  ChoiceCard(selected = selected, onClick = onClick) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(
          stringResource(summary),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 2.dp)
        )
      }
      if (selected) {
        Icon(HomeIcons.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TranslationStep(state: OnboardingState, translations: TranslationDownloads) {
  var query by rememberSaveable { mutableStateOf("") }
  var confirmRemoval by remember { mutableStateOf<TranslationItem?>(null) }
  val items = translations.items
  val downloaded = remember(items) { items.filter { it.exists() }.sortedBy { it.displayOrder } }
  val available = remember(items, query) { items.filter { !it.exists() && it.matches(query) } }

  val header: LazyListScope.() -> Unit = {
    item(key = "header") {
      Column(Modifier.padding(horizontal = 24.dp)) {
        StepHeader(Step.TRANSLATION, centered = false)
      }
    }
    item(key = "filter") {
      TranslationFilterField(
        query = query,
        onQueryChange = { query = it },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
      )
    }
  }

  when {
    items.isEmpty() && translations.refreshing -> LazyColumn(Modifier.fillMaxSize()) {
      header()
      item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { LoadingIndicator() } }
    }

    items.isEmpty() && state.translationsFailed -> LazyColumn(Modifier.fillMaxSize()) {
      header()
      item {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
        ) {
          Text(
            stringResource(R.string.onboarding_translation_error),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          FilledTonalButton(onClick = { translations.refresh() }, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.onboarding_retry))
          }
        }
      }
    }

    else -> TranslationList(
      downloaded = downloaded,
      available = available,
      downloadingId = translations.downloadingId,
      contentPadding = PaddingValues(bottom = 16.dp),
      onDownload = translations::download,
      onMove = translations::move,
      onRemove = { confirmRemoval = it },
      header = header
    )
  }

  confirmRemoval?.let { item ->
    AlertDialog(
      onDismissRequest = { confirmRemoval = null },
      title = { Text(stringResource(R.string.remove_dlg_title)) },
      text = { Text(stringResource(R.string.remove_dlg_msg, item.name())) },
      confirmButton = {
        TextButton(onClick = {
          confirmRemoval = null
          translations.remove(item)
        }) { Text(stringResource(com.quran.mobile.common.ui.core.R.string.remove_button)) }
      },
      dismissButton = {
        TextButton(onClick = { confirmRemoval = null }) {
          Text(stringResource(com.quran.mobile.common.ui.core.R.string.cancel))
        }
      }
    )
  }
}

@Composable
private fun PageStyles(state: OnboardingState, actions: OnboardingActions) {
  val downloadedCount = state.pageStyles.count { it.downloaded }
  state.pageStyles.forEach { style ->
    ChoiceCard(selected = style.inUse) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Text(stringResource(style.title), style = MaterialTheme.typography.titleMedium)
          Text(
            stringResource(style.description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
          )
        }
        if (style.inUse) {
          Icon(HomeIcons.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
      }
      PageStylePreview(state.pagePreviews[style.key])

      val status = buildList {
        if (style.inUse) add(stringResource(R.string.page_style_in_use))
        add(
          stringResource(
            if (style.downloaded) R.string.page_style_downloaded else R.string.page_style_not_downloaded
          )
        )
      }.joinToString(" · ")
      Text(
        status,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
      )

      // the style in use is downloaded on the previous step, so show its progress here too
      if (style.inUse && !style.downloaded && state.pages != PagesDownload.NotStarted) {
        Column(Modifier.padding(top = 12.dp)) { PagesDownloadCard(state.pages, actions.onDownloadPages) }
      }

      val canRemove = style.downloaded && downloadedCount > 1
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 12.dp)
      ) {
        when {
          !style.downloaded && (!style.inUse || state.pages == PagesDownload.NotStarted) ->
            FilledTonalButton(onClick = {
              if (style.inUse) actions.onDownloadPages() else actions.onDownloadPageStyle(style.key)
            }) {
              Icon(HomeIcons.Download, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
              Text(stringResource(R.string.page_style_download), modifier = Modifier.padding(start = 8.dp))
            }

          style.downloaded && !style.inUse ->
            FilledTonalButton(onClick = { actions.onUsePageStyle(style.key) }) {
              Text(stringResource(R.string.page_style_use))
            }
        }
        if (style.downloaded) {
          OutlinedButton(onClick = { actions.onRemovePageStyle(style.key) }, enabled = canRemove) {
            Text(stringResource(R.string.page_style_remove))
          }
        }
      }
      if (style.downloaded && !canRemove) {
        Text(
          stringResource(R.string.page_style_last_one),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 8.dp)
        )
      }
    }
  }
}

/**
 * How the style writes the Basmalah, on paper whatever the theme, since that is
 * how the pages themselves are shown.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PageStylePreview(preview: ImageBitmap?) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = Color(0xFFFFFDF7),
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 12.dp)
  ) {
    Column(Modifier.padding(12.dp)) {
      if (preview != null) {
        Image(
          bitmap = preview,
          contentDescription = stringResource(R.string.page_style_preview),
          contentScale = ContentScale.FillWidth,
          colorFilter = ColorFilter.tint(Color(0xFF1B1B1B)),
          modifier = Modifier.fillMaxWidth()
        )
      } else {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
        ) {
          LoadingIndicator()
        }
      }
    }
  }
}
