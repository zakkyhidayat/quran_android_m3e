package com.quran.labs.androidquran.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The body of a Material 3 dialog, for dialogs that live in their own window (a DialogFragment):
 * a large rounded container with a title, the content and a row of text buttons. The fragment's
 * window is transparent, so this is what you see.
 */
@Composable
fun DialogSurface(
  title: String?,
  confirmLabel: String,
  onConfirm: () -> Unit,
  dismissLabel: String?,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
  confirmEnabled: Boolean = true,
  content: @Composable () -> Unit
) {
  Surface(
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation = 6.dp,
    modifier = modifier.fillMaxWidth().expressiveAppear(rise = 40f, from = 0.9f)
  ) {
    Column(modifier = Modifier.padding(24.dp)) {
      if (title != null) {
        Text(
          text = title,
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(bottom = 16.dp)
        )
      }
      Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
        content()
      }
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
      ) {
        if (dismissLabel != null) {
          TextButton(onClick = onDismiss) { Text(dismissLabel) }
        }
        TextButton(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmLabel) }
      }
    }
  }
}
