package moe.lyniko.hiderecent

import android.content.Intent
import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import moe.lyniko.hiderecent.utils.PreferenceUtils

class ModernMainHook : XposedModule() {
    companion object {
        private const val TAG = "HideRecent"
    }

    private lateinit var packages: MutableSet<String>

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        log(Log.INFO, TAG, "Module loaded in process: ${param.processName}")
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        val prefs = getRemotePreferences(PreferenceUtils.functionalConfigName)
        packages = PreferenceUtils.getPackageListFromPref(prefs)

        val classLoader = param.classLoader
        try {
            val taskClass = Class.forName(
                "com.android.server.wm.Task", false, classLoader
            )
            val recentTasksClass = Class.forName(
                "com.android.server.wm.RecentTasks", false, classLoader
            )
            val method = recentTasksClass.getDeclaredMethod(
                "isVisibleRecentTask", taskClass
            )

            hook(method).intercept { chain ->
                val task = chain.args[0]!!
                val intent = task.javaClass
                    .getMethod("getBaseIntent")
                    .invoke(task) as Intent

                if (BuildConfig.DEBUG) {
                    log(Log.DEBUG, TAG, "Current Intent: $intent")
                    log(Log.DEBUG, TAG, "Current component: ${intent.component}")
                    log(Log.DEBUG, TAG, "Current package: ${intent.component?.packageName}")
                }

                val packageName = intent.component?.packageName
                if (packageName != null && packages.contains(packageName)) {
                    false
                } else {
                    chain.proceed()
                }
            }
        } catch (e: Throwable) {
            log(Log.ERROR, TAG, "Failed to hook isVisibleRecentTask", e)
        }
    }
}
