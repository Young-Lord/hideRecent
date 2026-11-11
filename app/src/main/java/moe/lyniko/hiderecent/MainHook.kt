package moe.lyniko.hiderecent

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import moe.lyniko.hiderecent.utils.PreferenceUtils
import java.lang.reflect.Method

class MainHook : XposedModule() {

    override fun onPackageReady(param: PackageReadyParam) {
        log(Log.INFO, TAG, "onPackageReady: ${param.packageName}")
        if (param.packageName == BuildConfig.APPLICATION_ID) {
            hookSelf()
        }
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        log(Log.INFO, TAG, "onSystemServerStarting")
        hookSystemServer(param.classLoader)
    }

    private fun hookSelf() {
        installHook("ModuleStatus.isActive") {
            val method = ModuleStatus::class.java.getDeclaredMethod("isActive")
            hook(method).intercept { true }
        }
    }

    private fun hookSystemServer(classLoader: ClassLoader) {
        val taskClass = findClass(classLoader, "com.android.server.wm.Task")
        if (taskClass != null) {
            installHook("RecentTasks.isVisibleRecentTask(Task)") {
                hookMethod(
                    classLoader,
                    "com.android.server.wm.RecentTasks",
                    "isVisibleRecentTask",
                    arrayOf(taskClass)
                ) { chain ->
                    if (shouldRemoveTask(chain.args[0])) false else chain.proceed()
                }
            }

            // Vivo and some OEM ROMs call the two-arg overload directly from getRecentTasksImpl.
            installHook("RecentTasks.isVisibleRecentTask(Task,boolean)") {
                hookMethod(
                    classLoader,
                    "com.android.server.wm.RecentTasks",
                    "isVisibleRecentTask",
                    arrayOf(taskClass, Boolean::class.javaPrimitiveType!!)
                ) { chain ->
                    if (shouldRemoveTask(chain.args[0])) false else chain.proceed()
                }
            }

            installHook("TaskSnapshotController.getSnapshotMode") {
                hookMethod(
                    classLoader,
                    "com.android.server.wm.TaskSnapshotController",
                    "getSnapshotMode",
                    arrayOf(taskClass)
                ) { chain ->
                    if (shouldHideTaskContent(chain.args[0])) SNAPSHOT_MODE_APP_THEME else chain.proceed()
                }
            }

            // getSnapshotMode may live on the parent class on some ROMs.
            installHook("AbsAppSnapshotController.getSnapshotMode") {
                hookMethod(
                    classLoader,
                    "com.android.server.wm.AbsAppSnapshotController",
                    "getSnapshotMode",
                    arrayOf(taskClass)
                ) { chain ->
                    if (shouldHideTaskContent(chain.args[0])) SNAPSHOT_MODE_APP_THEME else chain.proceed()
                }
            }

            installHook("Task.getSnapshot") {
                hookMethod(
                    classLoader,
                    "com.android.server.wm.Task",
                    "getSnapshot",
                    arrayOf(Boolean::class.javaPrimitiveType!!, Boolean::class.javaPrimitiveType!!)
                ) { chain ->
                    val snapshot = chain.proceed()
                    if (snapshot != null && shouldHideTaskContent(chain.thisObject) && isRealSnapshot(snapshot)) {
                        null
                    } else {
                        snapshot
                    }
                }
            }
        }

        installHook("ActivityRecord.shouldUseAppThemeSnapshot") {
            hookMethod(
                classLoader,
                "com.android.server.wm.ActivityRecord",
                "shouldUseAppThemeSnapshot",
                arrayOf()
            ) { chain ->
                if (shouldHideActivityContent(chain.thisObject)) true else chain.proceed()
            }
        }
    }

    private fun hookMethod(
        classLoader: ClassLoader,
        className: String,
        methodName: String,
        paramTypes: Array<Class<*>>,
        hooker: XposedInterface.Hooker
    ) {
        val method: Method = Class.forName(className, false, classLoader)
            .getDeclaredMethod(methodName, *paramTypes)
        hook(method).intercept(hooker)
    }

    private fun findClass(classLoader: ClassLoader, className: String): Class<*>? {
        return try {
            Class.forName(className, false, classLoader)
        } catch (_: Throwable) {
            null
        }
    }

    private fun installHook(name: String, hookInstaller: () -> Unit) {
        try {
            hookInstaller()
            log(Log.INFO, TAG, "hook installed: $name")
        } catch (t: Throwable) {
            log(Log.WARN, TAG, "hook skipped ($name): ${t.message}")
        }
    }

    private fun shouldRemoveTask(taskObject: Any?): Boolean {
        ensureConfigLoaded()
        return isTaskHiddenPackage(packageNameFromTask(taskObject))
    }

    private fun shouldHideTaskContent(taskObject: Any?): Boolean {
        ensureConfigLoaded()
        return isContentHiddenPackage(packageNameFromTask(taskObject))
    }

    private fun shouldHideActivityContent(activityObject: Any?): Boolean {
        ensureConfigLoaded()
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

        packageNameFromIntent(callMethod(taskObject, "getBaseIntent") as? Intent)?.let { return it }
        packageNameFromIntent(getField(taskObject, "intent") as? Intent)?.let { return it }

        val realActivity = getField(taskObject, "realActivity") as? ComponentName
        realActivity?.packageName?.let { return it }

        return getField(taskObject, "mCallingPackage") as? String
    }

    private fun packageNameFromActivityRecord(activityObject: Any?): String? {
        if (activityObject == null) return null

        (getField(activityObject, "packageName") as? String)?.let { return it }

        val activityComponent = getField(activityObject, "mActivityComponent") as? ComponentName
        activityComponent?.packageName?.let { return it }

        return packageNameFromIntent(getField(activityObject, "intent") as? Intent)
    }

    private fun packageNameFromIntent(intent: Intent?): String? {
        return intent?.component?.packageName ?: intent?.`package`
    }

    private fun isRealSnapshot(snapshot: Any): Boolean {
        return callMethod(snapshot, "isRealSnapshot") as? Boolean ?: true
    }

    private fun callMethod(target: Any, methodName: String): Any? {
        var clazz: Class<*>? = target.javaClass
        while (clazz != null) {
            try {
                val method = clazz.getDeclaredMethod(methodName)
                method.isAccessible = true
                return method.invoke(target)
            } catch (_: Throwable) {
                clazz = clazz.superclass
            }
        }
        return null
    }

    private fun getField(target: Any, fieldName: String): Any? {
        var clazz: Class<*>? = target.javaClass
        while (clazz != null) {
            try {
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                return field.get(target)
            } catch (_: Throwable) {
                clazz = clazz.superclass
            }
        }
        return null
    }

    @Volatile
    private var taskHiddenPackages: Set<String> = emptySet()

    @Volatile
    private var contentHiddenPackages: Set<String> = emptySet()

    @Volatile
    private var loaded = false

    private var remotePrefs: SharedPreferences? = null

    private fun ensureConfigLoaded() {
        if (!loaded) tryLoadConfig()
    }

    private fun tryLoadConfig() {
        try {
            val pref = remotePrefs ?: getRemotePreferences(PreferenceUtils.functionalConfigName).also {
                remotePrefs = it
                it.registerOnSharedPreferenceChangeListener { _, _ -> loaded = false }
            }
            val hiddenTasks = PreferenceUtils.getPackageListFromPref(pref)
            val hiddenContents = PreferenceUtils.getContentHiddenPackageListFromPref(pref)
            hiddenContents.removeAll(hiddenTasks)
            taskHiddenPackages = hiddenTasks
            contentHiddenPackages = hiddenContents
            loaded = true
        } catch (t: Throwable) {
            log(Log.WARN, TAG, "failed to load config: ${t.message}")
        }
    }

    companion object {
        private const val TAG = "HideRecent"
        private const val SNAPSHOT_MODE_APP_THEME = 1
    }
}
