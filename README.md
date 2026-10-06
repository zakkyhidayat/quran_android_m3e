<div align="center">

<img src="https://raw.githubusercontent.com/quran/quran_android/main/app/src/madani/res/drawable-xxhdpi/icon.png" alt='Quran for Android logo'/>

# Quran for Android

[![Build Status](https://github.com/quran/quran_android/actions/workflows/build.yml/badge.svg)](https://github.com/quran/quran_android/actions/workflows/build.yml)
[![Version](https://img.shields.io/github/v/release/quran/quran_android?include_prereleases&sort=semver)](https://github.com/quran/quran_android/releases/latest)
[![Github Downloads](https://img.shields.io/github/downloads/quran/quran_android/total?logo=Github)](https://github.com/quran/quran_android/releases)

This is a simple (Madani based) Quran app for Android.

[<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png"
      alt='Get it on Google Play'
      height="80">](https://play.google.com/store/apps/details?id=com.quran.labs.androidquran)
[<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png"
      alt="Get it at IzzyOnDroid"
      height="80">](https://apt.izzysoft.de/packages/com.quran.labs.androidquran)
[<img src="https://user-images.githubusercontent.com/69304392/148696068-0cfea65d-b18f-4685-82b5-329a330b1c0d.png"
      alt='Get it on GitHub'
      height="80">](https://github.com/quran/quran_android/releases/latest)

<div align="left">

## About this fork (Material 3 Expressive)

This fork of Quran for Android changes three things:

* **Audio is disabled.** Recitation playback, the audio bar, the qari list, the audio manager, the audio download settings and Android Auto are all switched off by one build flag. Build with `./gradlew assembleMadaniDebug -PenableAudio` to bring all of it back.
* **Material 3 Expressive.** The view based screens use `Theme.Material3Expressive`, and the Compose screens use `MaterialExpressiveTheme` with the expressive motion scheme. To get the Compose expressive APIs, `androidx.compose.material3` is pinned to `1.5.0-alpha29`, because the Compose BOM still maps it to 1.4.0.
* **Own application id.** The madani build is `io.zakkyhidayat.quran.m3e` (debug builds add `.debug`), so it installs next to the official app instead of replacing it.
* **Donate link.** Settings opens with a *Support Quran.com* entry that links to the Quran Foundation donation page.
* **Dynamic or original colors.** Settings → Display → *Color scheme* picks between dynamic color (from the wallpaper, Android 12+) and the original Quran teal palette. The light, dark and automatic *Appearance* setting still applies on top of either one.

The `Fork Build` workflow (`.github/workflows/fork_build.yml`) builds a debug APK on every push and uploads it as an artifact, then runs lint and the unit tests.

## Credits

* madani images from [quran images project](https://github.com/quran/quran.com-images) on github.
* qaloon images used with permission of Nous Memes Editions Et Diffusion (Tunisia).
* naskh images used with permission of SHL Info Systems.
* translation, tafsir and Arabic data come from [quranenc](https://quranenc.com) and [King Saud University](https://quran.ksu.edu.sa). a small number of translations also come from [tanzil](http://tanzil.net).

## Contributing

If you'd like to contribute, please take a look at the [PRs Welcome](https://github.com/quran/quran_android/issues?q=is%3Aissue+is%3Aopen+label%3A%22PRs+Welcome%22) label on the issue tracker. For new features, please open an issue to discuss it before beginning implementation.

Use [`quran_android-code_style.xml`](https://github.com/quran/quran_android/blob/main/quran_android-code_style.xml) for Android Studio / IntelliJ code styles. Import it by copying it to the Android Studio/IntelliJ IDEA codestyles folder. For Android Studio, that folder is located at `~/.AndroidStudio[Version]/config/codestyles` (the root folder name may differ depending on the host machine and Android Studio version, but the rest of the path should be same). After copying the `quran_android-code_style.xml`, go to Code Style preferences screen and choose `quran_android-code_style` from Code Style Schemes.

Please set your Android studio kotlin code style based on [Kotlin Coding Conventions](https://kotlinlang.org/docs/reference/coding-conventions.html). You can configure it from menu Settings | Editor | Code Style | Kotlin, click on "Set from…" link in the upper right corner, and select "Predefined style / Kotlin style guide" from the menu.

May Allah reward all the awesome [Contributors and Translators](https://github.com/quran/quran_android/blob/main/CONTRIBUTORS.md).


## Setup

### Command Line

You can build Quran from the command line by running `./gradlew assembleMadaniDebug`.

### Android Studio / IntelliJ

Choose "Import Project," and choose the `build.gradle` file from the top level directory. Under "Build Variants" (a tab on the left side), choose "madaniDebug."

## Using Quran for Android code in other projects

The intention behind open sourcing Quran for Android is two fold - first, to allow developers to help contribute to the app, thus speeding up the development of new features and ideas. Second, to give back to the community and serve as a code reference.

Quran for Android costs money to run - all the data (pages, audio files, and translations) are hosted on servers that people volunteer their money to pay for every month. Moreover, the data itself is the work of various scholars, organizations, or reciters, many of whom provide this data free for usage for the benefit of the ummah.

Therefore, people planning on taking this project and profiting from it (by way of ads, in app purchases, etc) are in fact stealing from the work of the contributors of this project, and from the people who volunteer to pay for the servers (since they increase the bandwidth costs on them instead of covering them themselves).

Please keep use of this code for non-profit purposes only. Also, please note that the project is under the GPL 3 license, which requires that modifications to this code be open sourced as well. Please note that the data is licensed under the various licenses of the data's authors (typically, this is [CC BY-NC-ND](https://creativecommons.org/licenses/by-nc-nd/2.0/), but may differ depending on the source of the data).


## Open Source Projects Used

* [AndroidX](https://developer.android.com/jetpack/androidx/)
* [Kotlin](https://kotlinlang.org)
* [Material Design Components](https://github.com/material-components/material-components-android)
* [AndroidSlidingUpPanel](https://github.com/umano/AndroidSlidingUpPanel)
* [OkHttp](https://github.com/square/okhttp)
* [RxJava 2](https://github.com/ReactiveX/RxJava)
* [RxAndroid](https://github.com/ReactiveX/RxAndroid)
* [Moshi](https://github.com/square/moshi)
* [Dagger2](https://google.github.io/dagger/)
* [Timber](https://github.com/JakeWharton/timber)
* [dnsjava](http://dnsjava.org)
* [NumberPicker](https://github.com/ShawnLin013/NumberPicker)
