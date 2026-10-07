package com.quran.page.common.toolbar

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnMenuItemClickListener
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import com.quran.data.model.selection.SelectionIndicator
import com.quran.data.model.selection.SelectionRectangle
import com.quran.labs.androidquran.common.toolbar.R
import com.quran.page.common.toolbar.dao.SelectedAyahPlacementType
import com.quran.page.common.toolbar.di.AyahToolBarInjector
import com.quran.page.common.toolbar.extension.toInternalPosition
import dev.zacsweers.metro.Inject
import kotlin.math.max
import kotlin.math.min

/**
 * The floating window that opens on the ayah you select: one row of actions (bookmark, share the
 * link, share the text, copy, play) and, under it, whatever [contentContainer] holds - the
 * translation. It sits above the ayah when there is room there and below it otherwise, so it
 * does not cover the ayah that was tapped, and a small arrow points back at the ayah.
 */
class AyahToolBar @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyle: Int = 0
) : ViewGroup(context, attrs, defStyle), OnClickListener, OnLongClickListener, AyahSelectionReactor {

  private var menu: Menu
  private val pipWidth: Int
  private val pipHeight: Int
  private val ayahMenu = R.menu.ayah_menu
  private val card: LinearLayout
  private val menuLayout: LinearLayout
  private val divider: View
  private val toolBarPip: AyahToolBarPip
  private val toolBarHeight: Int
  private val itemWidth: Int
  private val cardMaxWidth: Int
  private val sideMargin: Int
  private val gap: Int
  private val versePillHeight: Float
  private val cornerRadius: Float

  /** Where the translation goes, under the actions. */
  val contentContainer: FrameLayout

  /** The start of the action row, where the translation picker sits. */
  val headerContainer: FrameLayout

  private var containerColor = ContextCompat.getColor(context, R.color.toolbar_background)
  private var contentColor = ContextCompat.getColor(context, R.color.toolbar_icon)
  private var accentColor = contentColor
  private var bookmarked = false

  private var pipOffset = 0f
  private var pipPosition: SelectedAyahPlacementType
  private var currentMenu: Menu? = null
  private var itemSelectedListener: OnMenuItemClickListener? = null
  private var lastIndicator: SelectionIndicator? = null
  private var positionedHeight = 0

  var isShowing = false
    private set

  var flavor: String = ""
  var longPressLambda: ((CharSequence) -> Unit) = {}
  var isRecitationEnabled = false
  var lastMeasuredWidth = 0
  var lastSelectionShouldPadForCutout = false

  /** Whether the translation shows in the window; off while the page itself is the translation. */
  var contentEnabled: () -> Boolean = { true }

  var insets: Insets = Insets.NONE

  @Inject
  lateinit var ayahToolBarPresenter: AyahToolBarPresenter

  init {
    val resources = context.resources
    toolBarHeight = resources.getDimensionPixelSize(R.dimen.toolbar_height)
    itemWidth = resources.getDimensionPixelSize(R.dimen.toolbar_item_width)
    pipHeight = resources.getDimensionPixelSize(R.dimen.toolbar_pip_height)
    pipWidth = resources.getDimensionPixelSize(R.dimen.toolbar_pip_width)
    cardMaxWidth = resources.getDimensionPixelSize(R.dimen.toolbar_card_max_width)
    sideMargin = resources.getDimensionPixelSize(R.dimen.toolbar_side_margin)
    gap = resources.getDimensionPixelSize(R.dimen.toolbar_gap)
    versePillHeight = 28 * resources.displayMetrics.density
    cornerRadius = resources.getDimension(R.dimen.toolbar_corner_radius)

    card = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      // rounded, with the ripples and the translation kept inside the corners
      clipToOutline = true
      layoutDirection = LAYOUT_DIRECTION_LTR
    }
    menuLayout = LinearLayout(context).apply {
      layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, toolBarHeight)
      layoutDirection = LAYOUT_DIRECTION_LTR
    }
    headerContainer = FrameLayout(context).apply {
      layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
    }
    menuLayout.addView(headerContainer)
    divider = View(context).apply {
      layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 1)
      visibility = GONE
    }
    contentContainer = FrameLayout(context).apply {
      layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
      id = R.id.ayah_toolbar_content
    }
    card.addView(menuLayout)
    card.addView(divider)
    card.addView(contentContainer)
    addView(card)

    pipPosition = SelectedAyahPlacementType.BOTTOM
    toolBarPip = AyahToolBarPip(context)
    toolBarPip.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, pipHeight)
    addView(toolBarPip)

    // used to use MenuBuilder, but now it has @RestrictTo, so using this clever trick from
    // StackOverflow - PopupMenu generates a new MenuBuilder internally, so this just lets us
    // get that menu and do whatever we want with it.
    menu = PopupMenu(this.context, this).menu
    val inflater = MenuInflater(this.context)
    inflater.inflate(ayahMenu, menu)
    applyColors(containerColor, contentColor, accentColor)
    showMenu(menu)
  }

  /** The window's colors: its surface, the icons and text on it, and the accent (a bookmark). */
  fun applyColors(container: Int, content: Int, accent: Int) {
    containerColor = container
    contentColor = content
    accentColor = accent
    card.background = GradientDrawable().apply {
      cornerRadius = this@AyahToolBar.cornerRadius
      setColor(container)
    }
    divider.setBackgroundColor((content and 0x00FFFFFF) or 0x1F000000)
    toolBarPip.setColor(container)
    showMenu(menu, force = true)
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    // inject the present and bind
    (context as AyahToolBarInjector).injectToolBar(this)
    ayahToolBarPresenter.bind(this)
  }

  override fun onDetachedFromWindow() {
    ayahToolBarPresenter.unbind(this)
    super.onDetachedFromWindow()
  }

  private fun cardWidth(parentWidth: Int): Int =
    max(itemWidth * 4, min(cardMaxWidth, parentWidth - 2 * sideMargin))

  /** How tall the window may get, from the room above or below the selected ayah. */
  private fun maxCardHeight(parentHeight: Int): Int {
    val indicator = lastIndicator
    val room = if (indicator is SelectionIndicator.SelectedItemPosition) {
      max(indicator.firstItem.top, parentHeight - indicator.lastItem.bottom) - pipHeight - gap
    } else {
      parentHeight / 2
    }.toInt()
    return room.coerceIn(toolBarHeight + 3 * gap, (parentHeight * 0.6f).toInt())
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val parentView = parent as? View
    val parentWidth = parentView?.width ?: MeasureSpec.getSize(widthMeasureSpec)
    val parentHeight = parentView?.height ?: MeasureSpec.getSize(heightMeasureSpec)
    val width = cardWidth(parentWidth)
    card.measure(
      MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(maxCardHeight(parentHeight), MeasureSpec.AT_MOST)
    )
    measureChild(
      toolBarPip,
      MeasureSpec.makeMeasureSpec(pipWidth, MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(pipHeight, MeasureSpec.EXACTLY)
    )
    setMeasuredDimension(width, card.measuredHeight + toolBarPip.measuredHeight)
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    val totalWidth = measuredWidth
    val pipWidth = toolBarPip.measuredWidth
    val pipHeight = toolBarPip.measuredHeight
    val menuWidth = card.measuredWidth
    val menuHeight = card.measuredHeight
    var pipLeft = pipOffset.toInt()
    if (pipLeft + pipWidth > totalWidth) {
      pipLeft = totalWidth / 2 - pipWidth / 2
    }

    // overlap the pip and the window by 1px to avoid occasional gap
    if (pipPosition == SelectedAyahPlacementType.TOP) {
      toolBarPip.layout(pipLeft, 0, pipLeft + pipWidth, pipHeight + 1)
      card.layout(0, pipHeight, menuWidth, pipHeight + menuHeight)
    } else {
      toolBarPip.layout(pipLeft, menuHeight - 1, pipLeft + pipWidth, menuHeight + pipHeight)
      card.layout(0, 0, menuWidth, menuHeight)
    }

    // handle first layout of toolbar
    val parentWidth = (parent as View).width
    if (lastMeasuredWidth != totalWidth && lastMeasuredWidth == 0) {
      // whenever we're RTL, we need to adjust the translationX
      if (layoutDirection == LAYOUT_DIRECTION_RTL) {
        val insetToAdd = if (lastSelectionShouldPadForCutout) max(insets.left, insets.right) else 0
        translationX = translationX - (parentWidth - measuredWidth) + insetToAdd
      }
      lastMeasuredWidth = totalWidth
    }

    // the translation arrives after the window is first placed, which changes its height, and
    // with it which side of the ayah it fits on
    val indicator = lastIndicator
    if (isShowing && indicator != null && positionedHeight != measuredHeight) {
      positionedHeight = measuredHeight
      post { updatePosition(indicator) }
    }
  }

  private fun showMenu(menu: Menu, force: Boolean = false) {
    if (currentMenu === menu && !force) {
      // no need to re-draw
      return
    }

    // If recitation is enabled, show it in the menu
    if (isRecitationEnabled) {
      menu.findItem(R.id.cab_recite_from_here)?.apply { isVisible = true }
    }

    // the first child is the header (the translation picker); the buttons come after it
    while (menuLayout.childCount > 1) menuLayout.removeViewAt(1)
    val count = menu.size()
    for (i in 0 until count) {
      val item = menu.getItem(i)
      if (item.isVisible) {
        val view = getMenuItemView(item)
        menuLayout.addView(view)
      }
    }
    currentMenu = menu
    updateBookmarkIcon()
  }

  private fun getMenuItemView(item: MenuItem): View {
    return ImageButton(context).apply {
      setImageDrawable(item.icon)
      imageTintList = ColorStateList.valueOf(
        if (item.itemId == R.id.cab_bookmark_ayah && bookmarked) accentColor else contentColor
      )
      background = RippleDrawable(
        ColorStateList.valueOf((contentColor and 0x00FFFFFF) or 0x33000000), null, null
      )
      contentDescription = item.title
      id = item.itemId
      layoutParams = LinearLayout.LayoutParams(itemWidth, LayoutParams.MATCH_PARENT)
      setOnClickListener(this@AyahToolBar)
      setOnLongClickListener(this@AyahToolBar)
    }
  }

  fun setBookmarked(bookmarked: Boolean) {
    this.bookmarked = bookmarked
    val bookmarkItem = menu.findItem(R.id.cab_bookmark_ayah)
    bookmarkItem.setIcon(if (bookmarked) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border)
    updateBookmarkIcon()
  }

  private fun updateBookmarkIcon() {
    val bookmarkItem = menu.findItem(R.id.cab_bookmark_ayah) ?: return
    val bookmarkButton = findViewById<ImageButton>(R.id.cab_bookmark_ayah) ?: return
    bookmarkButton.setImageDrawable(bookmarkItem.icon)
    bookmarkButton.imageTintList =
      ColorStateList.valueOf(if (bookmarked) accentColor else contentColor)
  }

  override fun onSelectionChanged(selectionIndicator: SelectionIndicator, reset: Boolean) {
    if (reset) {
      resetMenu()
    }

    if (selectionIndicator is SelectionIndicator.None ||
        selectionIndicator is SelectionIndicator.ScrollOnly) {
      lastIndicator = null
      hideMenu()
    } else {
      val showContent = contentEnabled()
      contentContainer.visibility = if (showContent) VISIBLE else GONE
      divider.visibility = if (showContent) VISIBLE else GONE
      lastIndicator = selectionIndicator
      requestLayout()
      updatePosition(selectionIndicator)
      showMenu()
    }
  }

  override fun updateBookmarkStatus(isBookmarked: Boolean) {
    setBookmarked(isBookmarked)
  }

  private fun updatePosition(position: SelectionIndicator) {
    val parentView = parent as View
    val width = card.measuredWidth.takeIf { it > 0 } ?: cardWidth(parentView.width)
    val height = measuredHeight.takeIf { it > 0 } ?: (toolBarHeight + pipHeight)
    val internalPosition = when (position) {
      is SelectionIndicator.SelectedItemPosition ->
        place(position, parentView.width, parentView.height, width, height)
      // the translation list reports the bottom of the verse number pill: place the window
      // around it just like around an ayah on the page, so it always stays on the screen
      is SelectionIndicator.SelectedPointPosition -> {
        val pill = SelectionRectangle(
          position.x - 1f, position.y - versePillHeight, position.x + 1f, position.y
        )
        place(
          SelectionIndicator.SelectedItemPosition(pill, pill, position.xScroll, position.yScroll),
          parentView.width, parentView.height, width, height
        )
      }
      else -> position.toInternalPosition(parentView.width, parentView.height, width, height)
    }

    if (internalPosition != null) {
      val needsLayout =
        internalPosition.pipPosition != pipPosition || pipOffset != internalPosition.pipOffset
      ensurePipPosition(internalPosition.pipPosition)
      pipOffset = internalPosition.pipOffset
      val x = internalPosition.x
      val y = internalPosition.y

      // hack to help fix RTL when measuredWidth is not yet set. if this is set,
      // we adjust the translationX _after_ onLayout
      lastMeasuredWidth = measuredWidth
      val leftInset = if (position is SelectionIndicator.SelectedPointPosition) {
        lastSelectionShouldPadForCutout = true
        max(insets.left, insets.right)
      } else {
        lastSelectionShouldPadForCutout = false
        0
      }

      val actualX = if (layoutDirection == LAYOUT_DIRECTION_RTL) {
        if (measuredWidth > 0) {
          // in RTL, x=0 means the end of the toolbar is touching the very right of the screen, with
          // the toolbar itself appearing before the very right of the screen. translationX is still
          // to the right, however (i.e. translationX of 100 is 100 off the screen to the right).
          // consequently, we need to subtract the width of the view to get the actual x,
          // which is some negative value between -measuredWidth and 0 to properly render
          // when RTL.
          x - (parentView.width - measuredWidth) + leftInset
        } else {
          // if measuredWidth is not set, we do this step in onLayout instead
          x
        }
      } else {
        x + leftInset
      }

      setPosition(actualX, y)
      if (needsLayout) {
        requestLayout()
      }
    }
  }

  /**
   * Below the ayah when it fits there, otherwise above it, otherwise on whichever side has more
   * room - never on top of the ayah when one of the sides can hold it.
   */
  private fun place(
    position: SelectionIndicator.SelectedItemPosition,
    parentWidth: Int,
    parentHeight: Int,
    width: Int,
    height: Int
  ): com.quran.page.common.toolbar.dao.SelectionIndicatorPosition {
    val first = position.firstItem
    val last = position.lastItem
    val roomAbove = first.top
    val roomBelow = parentHeight - last.bottom
    // below first: the lines under the ayah are the ones still to be read
    val above = when {
      roomBelow >= height + gap -> false
      roomAbove >= height + gap -> true
      else -> roomAbove > roomBelow
    }
    val chosen = if (above) first else last
    var y = if (above) first.top - height - gap / 2 else last.bottom + gap / 2
    y = y.coerceIn(0f, max(0f, parentHeight - height.toFloat()))
    y += position.yScroll

    val midpoint = chosen.centerX()
    var x = midpoint - (width / 2)
    if (x < sideMargin) {
      x = sideMargin.toFloat()
    }
    if (x + width > parentWidth - sideMargin) {
      x = (parentWidth - sideMargin - width).toFloat()
    }
    val placement =
      if (above) SelectedAyahPlacementType.BOTTOM else SelectedAyahPlacementType.TOP
    // the arrow stays inside the rounded corners
    val pip = (midpoint - x).coerceIn(cornerRadius, width - cornerRadius - pipWidth)
    return com.quran.page.common.toolbar.dao.SelectionIndicatorPosition(
      x + position.xScroll, y, pip, placement
    )
  }

  private fun setPosition(x: Float, y: Float) {
    translationX = x
    translationY = y
  }

  private fun ensurePipPosition(position: SelectedAyahPlacementType) {
    pipPosition = position
    toolBarPip.ensurePosition(position)
  }

  private fun resetMenu(force: Boolean = false) {
    showMenu(menu, force)
  }

  private fun showMenu() {
    showMenu(menu)
    visibility = VISIBLE
    isShowing = true
  }

  private fun hideMenu() {
    isShowing = false
    visibility = GONE
  }

  fun setOnItemSelectedListener(listener: OnMenuItemClickListener?) {
    itemSelectedListener = listener
  }

  override fun onClick(v: View) {
    val item = menu.findItem(v.id) ?: return
    val subMenu = if (item.hasSubMenu()) item.subMenu else null
    if (subMenu != null) {
      showMenu(subMenu)
    } else {
      itemSelectedListener?.onMenuItemClick(item)
    }
  }

  override fun onLongClick(v: View): Boolean {
    val item = menu.findItem(v.id)
    val title = item?.title
    if (title != null) {
      longPressLambda(title)
      return true
    }
    return false
  }

  fun setMenuItemVisibility(itemId: Int, isVisible: Boolean) {
    val item = menu.findItem(itemId) ?: return
    if (item.isVisible != isVisible) {
      item.isVisible = isVisible
      resetMenu(true)
    }
  }
}
