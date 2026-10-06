package com.quran.labs.androidquran.ui.fragment

import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quran.data.model.bookmark.Tag
import com.quran.labs.androidquran.QuranApplication
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.CollectionNames
import com.quran.labs.androidquran.presenter.bookmark.TagBookmarkPresenter
import com.quran.labs.androidquran.ui.compose.DialogSurface
import com.quran.labs.androidquran.ui.compose.HomeIcons
import dev.zacsweers.metro.HasMemberInjections
import dev.zacsweers.metro.Inject
import com.quran.mobile.common.ui.core.R as UiCoreR

@HasMemberInjections
open class TagBookmarkDialog : ComposeDialogFragment() {
  private var tags by mutableStateOf<List<Tag>>(emptyList())
  private var checkedTags by mutableStateOf<Set<String>>(emptySet())

  @Inject
  lateinit var tagBookmarkPresenter: TagBookmarkPresenter

  override fun onAttach(context: Context) {
    super.onAttach(context)
    if (shouldInject()) {
      (context.applicationContext as QuranApplication).applicationComponent.inject(this)
    }
  }

  open fun shouldInject() = true

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val args = arguments
    if (args != null) {
      val bookmarkIds = args.getStringArray(EXTRA_BOOKMARK_IDS)
      if (bookmarkIds != null) {
        tagBookmarkPresenter.setBookmarksMode(bookmarkIds)
      }
    }
  }

  open fun showAddTagDialog() {
    val context: Context? = activity
    if (context is OnBookmarkTagsUpdateListener) {
      (context as OnBookmarkTagsUpdateListener).onAddTagSelected()
    }
  }

  fun setData(tags: List<Tag>?, checkedTags: HashSet<String>) {
    this.tags = tags ?: emptyList()
    this.checkedTags = checkedTags.toSet()
  }

  override fun onStart() {
    super.onStart()
    tagBookmarkPresenter.bind(this)
  }

  override fun onStop() {
    tagBookmarkPresenter.unbind(this)
    super.onStop()
  }

  @Composable
  override fun Content() {
    val context = LocalContext.current
    DialogSurface(
      title = stringResource(R.string.tag_bookmark),
      confirmLabel = stringResource(R.string.dialog_ok),
      onConfirm = {
        tagBookmarkPresenter.saveChanges()
        dismiss()
      },
      dismissLabel = stringResource(UiCoreR.string.cancel),
      onDismiss = ::dismiss
    ) {
      tags.forEach { tag ->
        val checked = tag.id in checkedTags
        val toggle = {
          val nowChecked = tagBookmarkPresenter.toggleTag(tag.id)
          checkedTags = if (nowChecked) checkedTags + tag.id else checkedTags - tag.id
        }
        TagRow(onClick = toggle) {
          Checkbox(checked = checked, onCheckedChange = null)
          Text(
            text = CollectionNames.displayName(context, tag),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp)
          )
        }
      }
      TagRow(onClick = { tagBookmarkPresenter.addTag() }) {
        Icon(HomeIcons.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
          text = stringResource(R.string.new_tag),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(start = 16.dp)
        )
      }
    }
  }

  @Composable
  private fun TagRow(onClick: () -> Unit, content: @Composable () -> Unit) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .clickable(onClick = onClick)
        .padding(horizontal = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      content()
    }
  }

  interface OnBookmarkTagsUpdateListener {
    fun onAddTagSelected()
  }

  companion object {
    const val TAG = "TagBookmarkDialog"
    private const val EXTRA_BOOKMARK_IDS = "bookmark_ids"
    fun newInstance(bookmarkId: String): TagBookmarkDialog {
      return newInstance(arrayOf(bookmarkId))
    }

    fun newInstance(bookmarkIds: Array<String>?): TagBookmarkDialog {
      val args = Bundle()
      args.putStringArray(EXTRA_BOOKMARK_IDS, bookmarkIds)
      val dialog = TagBookmarkDialog()
      dialog.arguments = args
      return dialog
    }
  }
}
