# Hide App from Recent Task List

## How to use

> Only tested on Android 13, MIUI 14; may work on [Android >= 10](http://aospxref.com/android-10.0.0_r47/xref/frameworks/base/services/core/java/com/android/server/wm/RecentTasks.java#1272)

1. Select `android` in module scope and activate the module
2. Select the apps you want to hide from recent app list
3. Reboot (you MUST reboot when you modify the list, or changes will not be applied until next reboot)

## Module Scope

- android

## Project URL

Home URL: <https://github.com/Young-Lord/hideRecent>

Xposed Modules Repo URL: <https://github.com/Xposed-Modules-Repo/moe.lyniko.hiderecent>

## Credits

original code from: <https://github.com/cokkeijigen/setAppFull>

## License

[My modification](https://github.com/Young-Lord/hideRecent/blob/master/app/src/main/java/moe/lyniko/hiderecent/MainHook.java#L34-L49) are licensed under WTFPL. You can use them freely.
