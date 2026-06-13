package moe.lyniko.hiderecent

import android.content.Intent
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import de.robv.android.xposed.XposedBridge
import moe.lyniko.hiderecent.utils.PreferenceUtils

class MainHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName == "android") onAppHooked(lpparam)
    }

    private fun onAppHooked(lpparam: LoadPackageParam) {
        val visibleFilterHook: XC_MethodHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (!loaded) tryLoadPackages()

                val taskObject = param.args[0]

                // 1. Safely call getBaseIntent
                val intent = try {
                    callMethod(taskObject, "getBaseIntent") as? Intent
                } catch (t: Throwable) {
                    XposedBridge.log("HideRecent: Failed to call getBaseIntent(). Error: ${t.message}")
                    null
                }

                if (intent == null) {
                    XposedBridge.log("HideRecent: getBaseIntent() returned null for task: $taskObject")
                    return
                }

                // 2. Try to get package name from component or package attribute
                var packageName = intent.component?.packageName ?: intent.`package`

                // 3. Fallback: Try to reflect the internal intent field directly if packageName is still null
                if (packageName == null) {
                    try {
                        val realIntent = callMethod(taskObject, "intent") as? Intent
                        if (realIntent != null) {
                            packageName = realIntent.component?.packageName ?: realIntent.`package`
                        } else {
                            XposedBridge.log("HideRecent: Internal intent field is also null")
                        }
                    } catch (t: Throwable) {
                        XposedBridge.log("HideRecent: Failed to reflect internal intent field. Error: ${t.message}")
                    }
                }

                // 4. Final check for package name
                if (packageName == null) {
                    XposedBridge.log("HideRecent: Cannot resolve packageName for intent: $intent")
                    return
                }

                // 5. Match the blocklist and intercept
                if (packages.contains(packageName)) {
                    param.result = false
                }
            }
        }
        try {
            findAndHookMethod(
                "com.android.server.wm.RecentTasks",
                lpparam.classLoader,
                "isVisibleRecentTask",
                "com.android.server.wm.Task",
                visibleFilterHook
            )
            XposedBridge.log("HideRecent: hook installed, packages=$packages")
        } catch (t: Throwable) {
            XposedBridge.log("HideRecent: hook FAILED")
            XposedBridge.log(t)
        }
    }

    @Volatile
    private var packages: Set<String> = emptySet()
    @Volatile
    private var loaded = false

    private fun tryLoadPackages() {
        val xsp = XSharedPreferences(BuildConfig.APPLICATION_ID, PreferenceUtils.functionalConfigName)
        xsp.makeWorldReadable()
        val pkgs = PreferenceUtils.getPackageListFromPref(xsp)
        if (pkgs.isNotEmpty()) {
            packages = pkgs
            loaded = true
        }
    }

    init {
        tryLoadPackages()
    }
}
