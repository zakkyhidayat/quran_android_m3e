package com.quran.labs.androidquran.ui.compose

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import com.quran.labs.androidquran.util.QuranUtils
import kotlin.math.min

/**
 * A Material 3 replacement for the deprecated `android.app.ProgressDialog`, with the parts of its
 * API the app uses: a title and message, a determinate or indeterminate progress bar, and an
 * optional cancel button.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class QuranProgressDialog(context: Context) : ComponentDialog(context) {
  private var titleText by mutableStateOf<CharSequence?>(null)
  private var messageText by mutableStateOf<CharSequence?>(null)
  private var progressValue by mutableIntStateOf(0)
  private var maxValue by mutableIntStateOf(100)
  private var indeterminateValue by mutableStateOf(false)
  private var negativeText by mutableStateOf<CharSequence?>(null)
  private var negativeListener: DialogInterface.OnClickListener? = null

  override fun setTitle(title: CharSequence?) {
    titleText = title
  }

  fun setMessage(message: CharSequence?) {
    messageText = message
  }

  fun setProgress(value: Int) {
    progressValue = value
  }

  fun setMax(value: Int) {
    maxValue = value
  }

  fun setIndeterminate(value: Boolean) {
    indeterminateValue = value
  }

  /** Kept for source compatibility with ProgressDialog; the bar is always horizontal. */
  @Suppress("UNUSED_PARAMETER")
  fun setProgressStyle(style: Int) = Unit

  /** Only [DialogInterface.BUTTON_NEGATIVE] (cancel) is supported. */
  fun setButton(which: Int, text: CharSequence, listener: DialogInterface.OnClickListener?) {
    if (which == DialogInterface.BUTTON_NEGATIVE) {
      negativeText = text
      negativeListener = listener
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val content = ComposeView(context).apply {
      setContent { QuranTheme { Body() } }
    }
    setContentView(content)
    window?.let { window ->
      window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
      val metrics = context.resources.displayMetrics
      val margin = (24 * metrics.density).toInt()
      val maxWidth = (560 * metrics.density).toInt()
      window.setLayout(
        min(metrics.widthPixels - 2 * margin, maxWidth),
        WindowManager.LayoutParams.WRAP_CONTENT
      )
    }
  }

  @androidx.compose.runtime.Composable
  private fun Body() {
    Surface(
      shape = MaterialTheme.shapes.extraLarge,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      tonalElevation = 6.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(24.dp)) {
        titleText?.let {
          Text(
            text = it.toString(),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 12.dp)
          )
        }
        messageText?.let {
          Text(
            text = it.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp)
          )
        }
        if (indeterminateValue || maxValue <= 0) {
          LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
          LinearWavyProgressIndicator(
            progress = { (progressValue.toFloat() / maxValue).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
          )
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = QuranUtils.getLocalizedNumber(progressValue * 100 / maxValue) + "%",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = QuranUtils.getLocalizedNumber(progressValue) + "/" + QuranUtils.getLocalizedNumber(maxValue),
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        negativeText?.let { label ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
          ) {
            TextButton(onClick = { negativeListener?.onClick(this@QuranProgressDialog, DialogInterface.BUTTON_NEGATIVE) }) {
              Text(label.toString())
            }
          }
        }
      }
    }
  }
}
