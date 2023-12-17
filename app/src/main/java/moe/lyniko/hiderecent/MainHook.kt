package moe.lyniko.hiderecent

import android.content.Intent
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import moe.lyniko.hiderecent.utils.PreferenceUtils


@Suppress("unused")
class MainHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName == "android") onAppHooked(lpparam)
    }

    private fun onAppHooked(lpparam: LoadPackageParam) {
        val visibleFilterHook: XC_MethodHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val intent = callMethod(param.args[0], "getBaseIntent") as Intent
                val packageName = intent.component?.packageName ?: return
                if (packages.contains(packageName)) {
                    param.result = false
                }
            }
        }
        try {
            findAndHookMethod(
                "com.android.server.wm.RecentTasks", lpparam.classLoader,
                "isVisibleRecentTask", "com.android.server.wm.Task",
                visibleFilterHook
            )
        } catch (ignored: Throwable) {
        }
    }

    private val packages: MutableSet<String>

    init {
        val xsp = XSharedPreferences(BuildConfig.APPLICATION_ID, PreferenceUtils.functionalConfigName)
        xsp.makeWorldReadable()
        packages = PreferenceUtils.getPackageListFromPref(xsp)
    }
}
