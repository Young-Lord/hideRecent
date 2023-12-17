package moe.lyniko.hiderecent.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build

class AppUtils(
    context: Context
) {
    // the package manager
    private val packageManager: PackageManager = context.packageManager

    // a list for all the apps, lazy init
    private val apps: List<PackageInfo> by lazy {
        // get all the apps
        if (
            PreferenceUtils(context).managerPref.getBoolean(
                PreferenceUtils.Companion.ConfigKeys.ShowPackageForAllUser.key,
                false
            )
        ) appForAllUser else appForSingleUser
    }

    private val appForSingleUser: List<PackageInfo> by lazy {
        packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
    }

    private val appForAllUser: List<PackageInfo> by lazy {
        val users = getUserProfiles(context)
        val apps = ArrayList<PackageInfo>()
        for (user in users) {
            apps.addAll(getInstalledPackagesAsUser(PackageManager.GET_META_DATA, getIdByUserHandle(user)))
        }
        apps
    }

    private fun atLeastT(): Boolean {
        return Build.VERSION.SDK_INT >= 33
    }

    private fun getInstalledPackagesAsUser(flags: Int, userId: Int): List<PackageInfo> {
        return if (atLeastT()) {
            iPackageManager.getInstalledPackages(flags.toLong(), userId)
        } else {
            iPackageManager.getInstalledPackages(flags, userId)
        }.list
    }

    val parsedApps: List<ParsedPackage> by lazy {
        apps.map { ParsedPackage(it, packageManager) }
    }
}

class ParsedPackage(
    private val pkg: PackageInfo,
    private val packageManager: PackageManager
){
    // lazy init
    val appName: String by lazy {
        packageManager.getApplicationLabel(pkg.applicationInfo).toString()
    }
    val appIcon: Drawable by lazy {
        pkg.applicationInfo.loadIcon(packageManager)
    }
    val packageName: String by lazy {
        pkg.packageName
    }
    @Suppress("unused")
    val versionName: String by lazy {
        pkg.versionName
    }
    @Suppress("unused")
    val versionCode: Long by lazy {
        pkg.longVersionCode
    }
    val isSystemApp: Boolean by lazy {
        pkg.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
    }
    private val packageNameLowerCase: String by lazy {
        packageName.lowercase()
    }
    private val appNameLowerCase: String by lazy {
        appName.lowercase()
    }
    fun isLowerCasedSearchMatch(searchContent: String): Boolean {
        return packageNameLowerCase.contains(searchContent) || appNameLowerCase.contains(searchContent)
    }
}