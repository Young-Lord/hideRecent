package moe.lyniko.hiderecent

import android.content.ComponentName
import android.content.Intent
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.XposedHelpers.getObjectField
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import moe.lyniko.hiderecent.utils.PreferenceUtils

class MainHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName == "android") onAppHooked(lpparam)
    }

    private fun onAppHooked(lpparam: LoadPackageParam) {
        val visibleRecentTaskHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (shouldRemoveTask(param.args[0])) {
                    param.result = false
                }
            }
        }

        installHook("RecentTasks.isVisibleRecentTask(Task)") {
            findAndHookMethod(
                "com.android.server.wm.RecentTasks",
                lpparam.classLoader,
                "isVisibleRecentTask",
                "com.android.server.wm.Task",
                visibleRecentTaskHook
            )
        }

        // Vivo and some OEM ROMs call the two-arg overload directly from getRecentTasksImpl.
        installHook("RecentTasks.isVisibleRecentTask(Task,boolean)") {
            findAndHookMethod(
                "com.android.server.wm.RecentTasks",
                lpparam.classLoader,
                "isVisibleRecentTask",
                "com.android.server.wm.Task",
                Boolean::class.javaPrimitiveType,
                visibleRecentTaskHook
            )
        }

        val snapshotModeHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (shouldHideTaskContent(param.args[0])) {
                    param.result = SNAPSHOT_MODE_APP_THEME
                }
            }
        }

        installHook("TaskSnapshotController.getSnapshotMode") {
            findAndHookMethod(
                "com.android.server.wm.TaskSnapshotController",
                lpparam.classLoader,
                "getSnapshotMode",
                "com.android.server.wm.Task",
                snapshotModeHook
            )
        }

        // getSnapshotMode may live on the parent class on some ROMs.
        installHook("AbsAppSnapshotController.getSnapshotMode") {
            findAndHookMethod(
                "com.android.server.wm.AbsAppSnapshotController",
                lpparam.classLoader,
                "getSnapshotMode",
                "com.android.server.wm.Task",
                snapshotModeHook
            )
        }

        installHook("ActivityRecord.shouldUseAppThemeSnapshot") {
            findAndHookMethod(
                "com.android.server.wm.ActivityRecord",
                lpparam.classLoader,
                "shouldUseAppThemeSnapshot",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (shouldHideActivityContent(param.thisObject)) {
                            param.result = true
                        }
                    }
                }
            )
        }

        installHook("Task.getSnapshot") {
            findAndHookMethod(
                "com.android.server.wm.Task",
                lpparam.classLoader,
                "getSnapshot",
                Boolean::class.javaPrimitiveType!!,
                Boolean::class.javaPrimitiveType!!,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val snapshot = param.result ?: return
                        if (shouldHideTaskContent(param.thisObject) && isRealSnapshot(snapshot)) {
                            param.result = null
                        }
                    }
                }
            )
        }
    }

    private fun installHook(name: String, hookInstaller: () -> Unit) {
        try {
            hookInstaller()
            XposedBridge.log("HideRecent: hook installed: $name")
        } catch (t: Throwable) {
            XposedBridge.log("HideRecent: hook skipped ($name): ${t.message}")
        }
    }

    private fun shouldRemoveTask(taskObject: Any?): Boolean {
        if (!loaded) tryLoadConfig()
        return isTaskHiddenPackage(packageNameFromTask(taskObject))
    }

    private fun shouldHideTaskContent(taskObject: Any?): Boolean {
        if (!loaded) tryLoadConfig()
        return isContentHiddenPackage(packageNameFromTask(taskObject))
    }

    private fun shouldHideActivityContent(activityObject: Any?): Boolean {
        if (!loaded) tryLoadConfig()
        return isContentHiddenPackage(packageNameFromActivityRecord(activityObject))
    }

    private fun isTaskHiddenPackage(packageName: String?): Boolean {
        return packageName != null && taskHiddenPackages.contains(packageName)
    }

    private fun isContentHiddenPackage(packageName: String?): Boolean {
        return packageName != null && contentHiddenPackages.contains(packageName)
    }

    private fun packageNameFromTask(taskObject: Any?): String? {
        if (taskObject == null) return null

        val baseIntent = tryOrNull {
            callMethod(taskObject, "getBaseIntent") as? Intent
        }
        packageNameFromIntent(baseIntent)?.let { return it }

        val taskIntent = tryOrNull {
            getObjectField(taskObject, "intent") as? Intent
        }
        packageNameFromIntent(taskIntent)?.let { return it }

        val realActivity = tryOrNull {
            getObjectField(taskObject, "realActivity") as? ComponentName
        }
        realActivity?.packageName?.let { return it }

        return tryOrNull {
            getObjectField(taskObject, "mCallingPackage") as? String
        }
    }

    private fun packageNameFromActivityRecord(activityObject: Any?): String? {
        if (activityObject == null) return null

        val packageName = tryOrNull {
            getObjectField(activityObject, "packageName") as? String
        }
        packageName?.let { return it }

        val activityComponent = tryOrNull {
            getObjectField(activityObject, "mActivityComponent") as? ComponentName
        }
        activityComponent?.packageName?.let { return it }

        val intent = tryOrNull {
            getObjectField(activityObject, "intent") as? Intent
        }
        return packageNameFromIntent(intent)
    }

    private fun packageNameFromIntent(intent: Intent?): String? {
        return intent?.component?.packageName ?: intent?.`package`
    }

    private fun isRealSnapshot(snapshot: Any): Boolean {
        return tryOrNull {
            callMethod(snapshot, "isRealSnapshot") as? Boolean
        } ?: true
    }

    private inline fun <T> tryOrNull(block: () -> T): T? {
        return try {
            block()
        } catch (_: Throwable) {
            null
        }
    }

    @Volatile
    private var taskHiddenPackages: Set<String> = emptySet()
    @Volatile
    private var contentHiddenPackages: Set<String> = emptySet()
    @Volatile
    private var loaded = false

    private fun tryLoadConfig() {
        val packagePref = XSharedPreferences(BuildConfig.APPLICATION_ID, PreferenceUtils.functionalConfigName)
        packagePref.makeWorldReadable()
        val hiddenTasks = PreferenceUtils.getPackageListFromPref(packagePref)
        val hiddenContents = PreferenceUtils.getContentHiddenPackageListFromPref(packagePref)
        hiddenContents.removeAll(hiddenTasks)
        taskHiddenPackages = hiddenTasks
        contentHiddenPackages = hiddenContents

        if (hiddenTasks.isNotEmpty() || hiddenContents.isNotEmpty()) {
            loaded = true
        }
    }

    companion object {
        private const val SNAPSHOT_MODE_APP_THEME = 1
    }

    init {
        tryLoadConfig()
    }
}
