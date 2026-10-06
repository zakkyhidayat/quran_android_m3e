package com.quran.labs.androidquran.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.quran.labs.androidquran.BuildConfig
import com.quran.labs.androidquran.R

class AboutFragment : QuranPreferenceFragment() {

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    addPreferencesFromResource(R.xml.about)

    val flavor = BuildConfig.FLAVOR + "Images"
    val parent = findPreference("aboutDataSources") as PreferenceCategory?
    imagePrefKeys.filter { it != flavor }.map {
      val pref: Preference? = findPreference(it)
      if (pref != null) {
        parent?.removePreference(pref)
      }
    }
  }

  companion object {
    private val imagePrefKeys = arrayOf("madaniImages", "naskhImages", "qaloonImages")
  }
}
