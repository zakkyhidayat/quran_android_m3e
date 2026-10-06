package com.quran.labs.androidquran

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.quran.labs.androidquran.common.ui.core.QuranThemeSettings
import com.quran.labs.androidquran.core.worker.QuranWorkerFactory
import com.quran.labs.androidquran.di.component.application.ApplicationComponent
import com.quran.labs.androidquran.util.QuranSettings
import com.quran.labs.androidquran.util.RecordingLogTree
import com.quran.labs.androidquran.util.ThemeUtil
import com.quran.labs.androidquran.widget.BookmarksWidgetSubscriber
import com.quran.mobile.di.QuranApplicationComponent
import com.quran.mobile.di.QuranApplicationComponentProvider
import dev.zacsweers.metro.HasMemberInjections
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.createGraphFactory
import timber.log.Timber
import java.util.Collections
import java.util.WeakHashMap

@HasMemberInjections
open class QuranApplication : Application(), QuranApplicationComponentProvider {
  lateinit var applicationComponent: ApplicationComponent

  @Inject lateinit var quranWorkerFactory: QuranWorkerFactory
  @Inject lateinit var bookmarksWidgetSubscriber: BookmarksWidgetSubscriber
  @Inject lateinit var quranSettings: QuranSettings

  // every activity that is currently alive, so a color scheme change can recreate all of them
  private val liveActivities: MutableSet<Activity> =
    Collections.newSetFromMap(WeakHashMap<Activity, Boolean>())

  /** The activities that are currently alive (created and not yet destroyed). */
  val activities: Collection<Activity>
    get() = liveActivities.toList()

  override fun provideQuranApplicationComponent(): QuranApplicationComponent {
    return applicationComponent
  }

  override fun onCreate() {
    super.onCreate()
    setupTimber()
    applicationComponent = initializeInjector()
    applicationComponent.inject(this)
    initializeWorkManager()
    bookmarksWidgetSubscriber.subscribeBookmarksWidgetIfNecessary()

    // theme setup
    val theme = quranSettings.currentTheme()
    ThemeUtil.setTheme(theme)
    setupColorScheme()
  }

  private fun setupColorScheme() {
    QuranThemeSettings.useDynamicColor = quranSettings.useDynamicColors()

    // the xml themes carry the original palette; when dynamic color is chosen (and available, on
    // Android 12+), layer the wallpaper based palette on top of each activity's theme instead.
    DynamicColors.applyToActivitiesIfAvailable(
      this,
      DynamicColorsOptions.Builder()
        .setPrecondition { _, _ -> QuranThemeSettings.useDynamicColor }
        .build()
    )

    registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
      override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        liveActivities.add(activity)
      }

      override fun onActivityDestroyed(activity: Activity) {
        liveActivities.remove(activity)
      }

      override fun onActivityStarted(activity: Activity) {}
      override fun onActivityResumed(activity: Activity) {}
      override fun onActivityPaused(activity: Activity) {}
      override fun onActivityStopped(activity: Activity) {}
      override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    })
  }

  open fun setupTimber() {
    Timber.plant(RecordingLogTree())
  }

  open fun initializeInjector(): ApplicationComponent {
    return createGraphFactory<ApplicationComponent.Factory>()
      .generate(this)
  }

  open fun initializeWorkManager() {
    WorkManager.initialize(
      this,
      Configuration.Builder()
        .setWorkerFactory(quranWorkerFactory)
        .build()
    )
  }
}
