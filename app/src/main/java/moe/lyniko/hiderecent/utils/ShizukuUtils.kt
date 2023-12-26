package moe.lyniko.hiderecent.utils

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import moe.lyniko.hiderecent.utils.PreferenceUtils.Companion.ConfigKeys



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
            @Suppress("UNUSED_EXPRESSION")
            false
        }
        false
    }
}

fun isShizukuNeeded(context: Context): Boolean {
    return PreferenceUtils.getInstance(context).managerPref.getBoolean(
        ConfigKeys.ShowPackageForAllUser.key,
        ConfigKeys.ShowPackageForAllUser.default,
    )
}