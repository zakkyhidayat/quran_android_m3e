package com.quran.labs.androidquran.presenter.bookmark

import com.quran.labs.androidquran.dao.bookmark.BookmarkRawResult

/** What [BookmarkPresenter] needs from whatever shows the bookmarks tab. */
interface BookmarksView {
  fun onNewRawData(rawItems: BookmarkRawResult)
}
