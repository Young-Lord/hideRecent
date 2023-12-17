package moe.lyniko.hiderecent.utils

import android.content.Context
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper


// https://github.com/LibChecker/LibChecker/pull/821/files
val iPackageManager: IPackageManager by lazy {
    IPackageManager.Stub.asInterface(
        ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
    )
}

fun isShizukuAvailable(): Boolean {
    try {
        Shizuku.pingBinder()
        if (Shizuku.isPreV11()) {
            return false
        }
        Shizuku.checkSelfPermission()
    } catch (e: IllegalStateException) {
        return false
    }
    return if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
        true
    } else if (Shizuku.shouldShowRequestPermissionRationale()) {
        // Users choose "Deny and don't ask again"
        false
    } else {
        // Request the permission
        try {
            Shizuku.requestPermission((Int.MIN_VALUE..Int.MAX_VALUE).random())
        } catch (e: IllegalStateException) {
            // Shizuku not installed
            false
        }
        false
    }
}

fun isShizukuNeeded(context: Context): Boolean {
    return PreferenceUtils(context).managerPref.getBoolean(
        PreferenceUtils.Companion.ConfigKeys.ShowPackageForAllUser.key,
        false
    )
}