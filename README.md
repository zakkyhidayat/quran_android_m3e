<div align="center">

<img src="app/src/madani/res/drawable-xxhdpi/icon.png" alt='Quran M3E logo'/>

# Quran M3E

[![Fork Build](https://github.com/zakkyhidayat/quran_android_m3e/actions/workflows/fork_build.yml/badge.svg)](https://github.com/zakkyhidayat/quran_android_m3e/actions/workflows/fork_build.yml)
[![Release](https://img.shields.io/github/v/release/zakkyhidayat/quran_android_m3e?include_prereleases&sort=semver)](https://github.com/zakkyhidayat/quran_android_m3e/releases/latest)

A Material 3 Expressive take on [Quran for Android](https://github.com/quran/quran_android), the Madani based Quran reader from Quran.com.

<div align="left">

> [!NOTE]
> **This M3E version was built entirely with AI.** Every change in this fork, from the code and theming to the build setup and this README, was written by an AI coding agent ([Claude Code](https://claude.ai/code)) under the direction of the repository owner. The app it builds on is the human work of the Quran.com team and contributors. Test it yourself before you rely on it, and report problems in this repository's [issues](https://github.com/zakkyhidayat/quran_android_m3e/issues), not upstream.

## Features

Everything from Quran for Android for reading the Quran: Madani mushaf pages, translations and tafsir, search, bookmarks and tags, highlights, reading bookmarks, night mode, and widgets. On top of that, this version changes the following:

* **Material 3 Expressive design.** The view based screens use the `Theme.Material3Expressive` theme, and the Compose screens use `MaterialExpressiveTheme` with expressive motion and shapes. Toolbars are flat and tonal. Lists, tabs, headers and dialogs take their colors from the theme.
* **Dynamic or original colors.** Settings → Display → *Color scheme*:
  * *Dynamic*, the default, takes its colors from your wallpaper. It needs Android 12 or newer.
  * *Original* keeps the classic Quran.com teal palette.

  The light, dark and automatic *Appearance* setting works with either one.
* **No audio, for now.** Recitation playback, the audio bar, the qari list, the audio manager, audio download settings and Android Auto are switched off.
* **No analytics.** Firebase Analytics and Crashlytics are off, so the app sends no usage or crash data.
* **Installs next to the official app.** The application id is `io.zakkyhidayat.quran.m3e`, so it does not replace Quran for Android if you already have it.
* **Optional donation link.** The last entry in Settings, *Support Quran.com*, opens the Quran Foundation donation page. The app never asks you to donate anywhere else.

Requires Android 7.0 (API 24) or newer.

## Download

Signed APKs are published on this repository's [Releases](https://github.com/zakkyhidayat/quran_android_m3e/releases) page, each with a SHA-256 checksum. Quran M3E is not on Google Play or F-Droid.

Every push also builds a debug APK, which you can download from the artifacts of a [Fork Build](https://github.com/zakkyhidayat/quran_android_m3e/actions/workflows/fork_build.yml) run. Each CI run can sign its debug build with a different throwaway key, so a debug build may refuse to install over another one. If that happens, uninstall the old one first.

## Building

```sh
./gradlew assembleMadaniDebug
```

In Android Studio, open the project and pick the `madaniDebug` build variant.

| Gradle property | Effect |
|---|---|
| `-PenableAudio` | Brings back audio playback, the qari list, the audio manager and Android Auto. |
| `-PenableFirebase` | Turns Firebase Analytics and Crashlytics back on. It needs your own `google-services.json`. |

`androidx.compose.material3` is pinned to `1.5.0-alpha29` in `gradle/libs.versions.toml`, because the Compose BOM still maps it to 1.4.0, which has no Material 3 Expressive APIs. Expect source changes whenever this pin is raised.

## Releasing

`.github/workflows/release.yml` builds a signed release APK and publishes it to GitHub Releases whenever a `v*` tag is pushed. You can also run it from the Actions tab.

It needs four repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | the release keystore, base64 encoded (`base64 -w0 quran-m3e-release.jks`) |
| `RELEASE_KEYSTORE_PASSWORD` | the keystore password |
| `RELEASE_KEY_ALIAS` | the key alias (`quran-m3e`) |
| `RELEASE_KEY_PASSWORD` | the key password |

```sh
git tag v3.6.4-m3e.1 && git push origin v3.6.4-m3e.1
```

Keep the keystore backed up. If it is lost, nobody who installed a release can update to a new one without uninstalling first.

## Credits

* The app is based on [Quran for Android](https://github.com/quran/quran_android) by Quran.com and its [contributors and translators](CONTRIBUTORS.md). May Allah reward them.
* madani images from [quran images project](https://github.com/quran/quran.com-images) on github.
* qaloon images used with permission of Nous Memes Editions Et Diffusion (Tunisia).
* naskh images used with permission of SHL Info Systems.
* translation, tafsir and Arabic data come from [quranenc](https://quranenc.com) and [King Saud University](https://quran.ksu.edu.sa). a small number of translations also come from [tanzil](http://tanzil.net).

## License and use

This project is under the GPL 3 [license](LICENSE), like the app it is based on. Any modifications must be open sourced as well.

Upstream asks that the code be used for **non-profit purposes only**, and this fork follows that. All the data (pages and translations) is hosted on servers that people volunteer their money to pay for every month. The data itself is the work of scholars and organizations who share it for the benefit of the ummah. Profiting from this project, by way of ads, in app purchases or similar, takes from their work and adds to their bandwidth costs without covering them. If you use this app, consider supporting Quran.com through the donation link in Settings.

The data is licensed under the various licenses of the data's authors. This is typically [CC BY-NC-ND](https://creativecommons.org/licenses/by-nc-nd/2.0/), but it may differ depending on the source.

## Contributing

Bug reports and pull requests for the Material 3 Expressive changes are welcome here. Report problems with the underlying app, its data or its features to [upstream](https://github.com/quran/quran_android/issues) instead, but only after checking that they also happen in the official app.

Use [`quran_android-code_style.xml`](quran_android-code_style.xml) for the Android Studio / IntelliJ code style, and the [Kotlin Coding Conventions](https://kotlinlang.org/docs/reference/coding-conventions.html) for Kotlin code.

## Open Source Projects Used

* [AndroidX](https://developer.android.com/jetpack/androidx/)
* [Kotlin](https://kotlinlang.org)
* [Material Design Components](https://github.com/material-components/material-components-android)
* [AndroidSlidingUpPanel](https://github.com/umano/AndroidSlidingUpPanel)
* [OkHttp](https://github.com/square/okhttp)
* [RxJava](https://github.com/ReactiveX/RxJava)
* [RxAndroid](https://github.com/ReactiveX/RxAndroid)
* [Moshi](https://github.com/square/moshi)
* [Metro](https://github.com/ZacSweers/metro)
* [Timber](https://github.com/JakeWharton/timber)
* [dnsjava](http://dnsjava.org)
* [NumberPicker](https://github.com/ShawnLin013/NumberPicker)
