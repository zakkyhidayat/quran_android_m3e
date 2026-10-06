package com.quran.labs.androidquran

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import com.quran.labs.androidquran.ui.compose.PreferencesScreen
import com.quran.labs.androidquran.ui.fragment.AboutFragment
import com.quran.labs.androidquran.ui.fragment.QuranPreferenceFragment

class AboutUsActivity : AppCompatActivity() {

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val fm = supportFragmentManager
    val fragment = fm.findFragmentByTag(QuranPreferenceFragment.TAG) as? AboutFragment
      ?: AboutFragment().also {
        fm.beginTransaction().add(it, QuranPreferenceFragment.TAG).commitNow()
      }

    setContent {
      QuranTheme {
        PreferencesScreen(R.string.menu_about, fragment, onBack = ::finish)
      }
    }
  }
}
