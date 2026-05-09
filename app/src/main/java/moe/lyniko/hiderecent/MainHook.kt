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
                val intent = callMethod(param.args[0], "getBaseIntent") as Intent
                val packageName = intent.component?.packageName ?: return
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
