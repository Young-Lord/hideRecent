# Hide App from Recent Task List

Simple module to hide apps from the recent task list, or keep them visible while hiding their preview snapshots.

Designed with Kotlin, Jetpack Compose and Material Design 3. Can also serve as a template for any Xposed module with an application selection list.

![UI Screenshot](https://github.com/Young-Lord/hideRecent/raw/master/assets/image/preview.jpg)

## How to use

> Tested on: Android 10 ~ 16.

1. Select `System framework` (package name may be `android` or `system` or empty, [see this](https://github.com/LSPosed/LSPosed/releases/tag/v1.9.1)) in module scope and activate the module
2. Force stop the module
3. Set a mode for each app in module settings: `No action`, `Hide task`, or `Hide preview` (if package list not shown, you can manually import / export settings to edit config)
4. Reboot (you MUST reboot when you modify the list or the hide mode, or changes will not be applied until next reboot)
5. If you need multi-user support, install this module only in main user, and use [Shizuku](https://shizuku.rikka.app/download/) to get app info from other users.

## Module Scope

- android

## Project URL

Home URL: <https://github.com/Young-Lord/hideRecent>

Xposed Modules Repo URL: <https://github.com/Xposed-Modules-Repo/moe.lyniko.hiderecent>

## Technical Details

UI: Material Design 3 + Jetpack Compose + Kotlin.

Hook: Hook `com.android.server.wm.RecentTasks.isVisibleRecentTask(com.android.server.wm.Task)` for apps set to `Hide task`.

For apps set to `Hide preview`, hook `com.android.server.wm.TaskSnapshotController.getSnapshotMode(com.android.server.wm.Task)` and `com.android.server.wm.ActivityRecord.shouldUseAppThemeSnapshot()` so selected apps use an app-theme task snapshot instead of a real screenshot. `com.android.server.wm.Task.getSnapshot(boolean, boolean)` is also hooked to avoid returning previously cached real snapshots.

## HELP ME IT DOESNT WORK!!!

Please open a issue [here](https://github.com/Young-Lord/hideRecent/issues). Provide your Android version, `/system/framework/framework.jar`, `/system/framework/services.jar` and all `/system/framework/framework{a number here}.jar` if they exist.

PR for refactoring is also appreciated.

## License

Apache-2.0 License or MIT License are fine.

## Thanks

<https://stackoverflow.com/questions/57266451/get-list-of-apps-of-all-users>

<https://github.com/LibChecker/LibChecker/pull/821/files> (Apache-2.0 license)

~~Original code from: <https://github.com/cokkeijigen/setAppFull>~~ refactored.

[rootAVD](https://gitlab.com/newbit/rootAVD)

## Why?

出于隐私或便捷原因，有些时候我们总是想隐藏一些应用。

CrDroid 内置了这个功能，这是好的，然而并不是所有人都在用 CrDroid。

而且，国内的 ROM 的“最近任务列表”里划掉一个卡片，就等于杀死这个应用，这太蠢了！你也不想你的 Clash For Android 编辑完配置就挂了吧？

Thanox 等一些应用也有这个功能，但只为了这个功能氪金并装一个闭源应用，怎么看都很怪。于是我买了 Thanox 订阅，然后写完这个模块后又卖了。
