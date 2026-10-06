package com.quran.labs.androidquran.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import com.quran.data.model.bookmark.Tag
import com.quran.labs.androidquran.QuranApplication
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.presenter.bookmark.AddTagDialogPresenter
import com.quran.labs.androidquran.ui.compose.DialogSurface
import dev.zacsweers.metro.Inject
import com.quran.mobile.common.ui.core.R as UiCoreR

class AddTagDialog : ComposeDialogFragment() {

  @Inject
  internal lateinit var addTagDialogPresenter: AddTagDialogPresenter

  private var nameError by mutableStateOf<String?>(null)

  override fun onAttach(context: Context) {
    super.onAttach(context)
    (context.applicationContext as QuranApplication).applicationComponent.inject(this)
  }

  override fun onStart() {
    super.onStart()
    addTagDialogPresenter.bind(this)
    dialog?.window?.setSoftInputMode(
      WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
    )
  }

  override fun onStop() {
    addTagDialogPresenter.unbind(this)
    super.onStop()
  }

  @Composable
  override fun Content() {
    val id = arguments?.getString(EXTRA_ID)
    val originalName = arguments?.getString(EXTRA_NAME, "") ?: ""
    var name by rememberSaveable(stateSaver = TextFieldValue.Saver) {
      mutableStateOf(
        TextFieldValue(if (id != null) originalName else "", TextRange(originalName.length))
      )
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val submit = {
      val text = name.text
      if (addTagDialogPresenter.validate(text, id)) {
        if (id != null) {
          addTagDialogPresenter.updateTag(Tag(id, text), ::dismissAfterPersistence)
        } else {
          addTagDialogPresenter.addTag(text, ::dismissAfterPersistence)
        }
      }
    }

    DialogSurface(
      title = stringResource(R.string.tag_dlg_title),
      confirmLabel = stringResource(R.string.dialog_ok),
      onConfirm = submit,
      dismissLabel = stringResource(UiCoreR.string.cancel),
      onDismiss = ::dismiss
    ) {
      OutlinedTextField(
        value = name,
        onValueChange = {
          name = it
          nameError = null
        },
        label = { Text(stringResource(R.string.tag_name)) },
        isError = nameError != null,
        supportingText = nameError?.let { error -> { Text(error) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        modifier = Modifier
          .fillMaxWidth()
          .focusRequester(focusRequester)
      )
    }
  }

  fun onBlankTagName() {
    nameError = getString(R.string.tag_blank_tag_error)
  }

  fun onDuplicateTagName() {
    nameError = getString(R.string.tag_duplicate_tag_error)
  }

  private fun dismissAfterPersistence() {
    if (isResumed && !parentFragmentManager.isStateSaved) {
      dismiss()
    }
  }

  companion object {
    const val TAG = "AddTagDialog"

    private const val EXTRA_ID = "id"
    private const val EXTRA_NAME = "name"

    fun newInstance(id: String, name: String): AddTagDialog {
      val args = Bundle()
      args.putString(EXTRA_ID, id)
      args.putString(EXTRA_NAME, name)
      val dialog = AddTagDialog()
      dialog.arguments = args
      return dialog
    }
  }
}
