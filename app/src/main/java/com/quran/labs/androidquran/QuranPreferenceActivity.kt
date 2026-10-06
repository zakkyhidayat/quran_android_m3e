package com.quran.labs.androidquran

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import com.quran.labs.androidquran.ui.compose.PreferencesScreen
import com.quran.labs.androidquran.ui.fragment.QuranPreferenceFragment
import com.quran.labs.androidquran.ui.fragment.QuranSettingsFragment

class QuranPreferenceActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    // the fragment has no view: it builds the preferences and handles what a change does, and
    // the Compose screen below draws them
    val fm = supportFragmentManager
    val fragment = fm.findFragmentByTag(QuranPreferenceFragment.TAG) as? QuranSettingsFragment
      ?: QuranSettingsFragment().also {
        fm.beginTransaction().add(it, QuranPreferenceFragment.TAG).commitNow()
      }

    setContent {
      QuranTheme {
        PreferencesScreen(R.string.menu_settings, fragment, onBack = ::finish)
      }
    }
  }

  fun restartActivity() {
    val intent = intent
    finish()
    startActivity(intent)
  }
}
