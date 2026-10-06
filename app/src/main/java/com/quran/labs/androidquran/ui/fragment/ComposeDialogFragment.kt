package com.quran.labs.androidquran.ui.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import kotlin.math.min

/**
 * A [DialogFragment] whose content is drawn with Compose. The dialog's own window is transparent
 * and sized like a Material 3 dialog (24dp from the screen edges, at most 560dp wide), so [Content]
 * supplies the whole visible dialog, normally through `DialogSurface`.
 */
abstract class ComposeDialogFragment : DialogFragment() {

  @Composable
  protected abstract fun Content()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setStyle(STYLE_NO_TITLE, 0)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent { QuranTheme { this@ComposeDialogFragment.Content() } }
    }
  }

  override fun onStart() {
    super.onStart()
    dialog?.window?.let { window ->
      window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
      val metrics = resources.displayMetrics
      val margin = (24 * metrics.density).toInt()
      val maxWidth = (560 * metrics.density).toInt()
      window.setLayout(
        min(metrics.widthPixels - 2 * margin, maxWidth),
        WindowManager.LayoutParams.WRAP_CONTENT
      )
    }
  }
}
