package com.quran.labs.androidquran

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import com.quran.labs.androidquran.service.util.PermissionUtil
import com.quran.labs.androidquran.ui.compose.PreferencesScreen
import com.quran.labs.androidquran.ui.fragment.QuranPreferenceFragment
import com.quran.labs.androidquran.ui.fragment.QuranAdvancedSettingsFragment
import com.quran.labs.androidquran.ui.util.ToastCompat
import com.quran.labs.androidquran.util.QuranSettings

class QuranAdvancedPreferenceActivity : AppCompatActivity() {

  companion object {
    private const val SI_LOCATION_TO_WRITE = "SI_LOCATION_TO_WRITE"
    private const val REQUEST_WRITE_TO_SDCARD_PERMISSION = 1
  }

  private var locationToWrite: String? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()

    super.onCreate(savedInstanceState)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    if (savedInstanceState != null) {
      locationToWrite = savedInstanceState.getString(SI_LOCATION_TO_WRITE)
    }

    // the fragment has no view: it builds the preferences and handles what a change does, and
    // the Compose screen below draws them
    val fm = supportFragmentManager
    val fragment = fm.findFragmentByTag(QuranPreferenceFragment.TAG) as? QuranAdvancedSettingsFragment
      ?: QuranAdvancedSettingsFragment().also {
        fm.beginTransaction().add(it, QuranPreferenceFragment.TAG).commitNow()
      }

    setContent {
      QuranTheme {
        PreferencesScreen(R.string.prefs_category_advanced, fragment, onBack = ::finish)
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    if (locationToWrite != null) {
      outState.putString(SI_LOCATION_TO_WRITE, locationToWrite)
    }
    super.onSaveInstanceState(outState)
  }

  fun requestWriteExternalSdcardPermission(newLocation: String) {
    if (PermissionUtil.canRequestWriteExternalStoragePermission(this)) {
      QuranSettings.getInstance(this).setSdcardPermissionsDialogPresented()
      locationToWrite = newLocation
      ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
        REQUEST_WRITE_TO_SDCARD_PERMISSION
      )
    } else {
      // in the future, we should make this a direct link - perhaps using a Snackbar.
      ToastCompat.makeText(this, R.string.please_grant_permissions, Toast.LENGTH_SHORT).show()
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    if (requestCode == REQUEST_WRITE_TO_SDCARD_PERMISSION) {
      if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && locationToWrite != null) {
        val fragment = supportFragmentManager.findFragmentByTag(QuranPreferenceFragment.TAG)
        if (fragment is QuranAdvancedSettingsFragment) {
          val location = locationToWrite
          if (location != null) {
            fragment.moveFiles(location)
          }
        }
      }
      locationToWrite = null
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

}
