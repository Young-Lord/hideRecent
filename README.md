# Hide App from Recent Task List

## How to use

> Only tested on Android 13 (MIUI 14) and Android 11 (MIUI 12); may work on [10 <= Android <= 14](http://aospxref.com/android-10.0.0_r47/xref/frameworks/base/services/core/java/com/android/server/wm/RecentTasks.java#1272)

1. Select `android` in module scope and activate the module
2. Select the apps you want to hide from recent app list
3. Reboot (you MUST reboot when you modify the list, or changes will not be applied until next reboot)
4. If you need multi-user support, install this module only in main user, and use [Shizuku](https://shizuku.rikka.app/download/) to get app info from other users.

## Module Scope

- android

## Project URL

Home URL: <https://github.com/Young-Lord/hideRecent>

Xposed Modules Repo URL: <https://github.com/Xposed-Modules-Repo/moe.lyniko.hiderecent>

## Technical Details

Material Design 3 + Jetpack Compose + Kotlin.

## HELP ME IT DOESNT WORK!!!

Please open a issue [here](https://github.com/Young-Lord/hideRecent/issues). Provide your Android version, `/system/framework/framework.jar` and all `/system/framework/framework{a number here}.jar` if exist.

I am not intended to support Android < 10, but anyone is free to [send a PR](https://github.com/Young-Lord/hideRecent/pulls) for Android < 10 support.

PR for refactoring is also appreciated.

## License

Apache-2.0 License or MIT License are all OK.

## Thanks

<https://stackoverflow.com/questions/57266451/get-list-of-apps-of-all-users>

<https://github.com/LibChecker/LibChecker/pull/821/files> (Apache-2.0 license)

~~Original code from: <https://github.com/cokkeijigen/setAppFull>~~ refactored.
