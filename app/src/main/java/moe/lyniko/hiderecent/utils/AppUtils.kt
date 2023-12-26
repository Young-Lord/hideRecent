package moe.lyniko.hiderecent.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.IBinder
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import moe.lyniko.hiderecent.utils.PreferenceUtils.Companion.ConfigKeys

class AppUtils(
    context: Context
) {
    companion object {

        @Volatile
        private var instance: AppUtils? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: AppUtils(context).also { instance = it }
            }
    }

    // the package manager
    private val packageManager: PackageManager = context.packageManager
    private val baseGetInstalledPackagesFlags: Int =
        PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA
    private var getInstalledPackagesFlags: Int = baseGetInstalledPackagesFlags
    private val preferenceUtils = PreferenceUtils.getInstance(context)

    private fun removeActivitiesFetch() {
        getInstalledPackagesFlags =
            baseGetInstalledPackagesFlags and (baseGetInstalledPackagesFlags xor PackageManager.GET_ACTIVITIES)
    }
    private val allApps: List<PackageInfo> by lazy {
        // catch random error caused by MIUI or something else
        if(!preferenceUtils.managerPref.getBoolean(ConfigKeys.HideNoActivityPackages.key, ConfigKeys.HideNoActivityPackages.default)){
            removeActivitiesFetch()
        }
        try {
            allAppsNoCatch
        } catch (e: Exception) {
            e.printStackTrace()
            if(!isActivitiesFetched()){
                // skip another try if already failed
                return@lazy listOf()
            }

            // remove GET_ACTIVITIES flag
            removeActivitiesFetch()
            try{
                allAppsNoCatch
            }
            catch(e: Exception) {
                e.printStackTrace()
                listOf()
            }
        }
    }

    fun isActivitiesFetched(): Boolean {
        return (PackageManager.GET_ACTIVITIES and getInstalledPackagesFlags) != 0
    }

    // a list for all the apps, lazy init
    private val allAppsNoCatch: List<PackageInfo> by lazy {
        // get all the apps
        if (
            preferenceUtils.managerPref.getBoolean(
                ConfigKeys.ShowPackageForAllUser.key,
                ConfigKeys.ShowPackageForAllUser.default
            ) && isShizukuAvailable()
        ) appForAllUser else appForSingleUser
    }

    private val appForSingleUser: List<PackageInfo> by lazy {
        packageManager.getInstalledPackages(getInstalledPackagesFlags)
    }

    private val appForAllUser: List<PackageInfo> by lazy {
        val users = getUserProfiles(context)
        val apps = ArrayList<PackageInfo>()
        for (user in users) {
            apps.addAll(
                getInstalledPackagesAsUser(
                    getInstalledPackagesFlags,
                    getIdByUserHandle(user)
                )
            )
        }
        apps
    }

    private fun atLeastT(): Boolean {
        return Build.VERSION.SDK_INT >= 33
    }

    @SuppressLint("PrivateApi")
    private fun getInstalledPackagesAsUser(
        @Suppress("SameParameterValue") flags: Int,
        userId: Int
    ): List<PackageInfo> {
        // fuck android.
        // https://www.xda-developers.com/implementing-shizuku/
        // Previous version: https://github.com/Young-Lord/hideRecent/commit/8f956002e1edbb95e2e3e945c28ec1a716596347
        // val iPmClass = Class.forName("android.content.pm.IPackageManager")
        val iPmStub = Class.forName("android.content.pm.IPackageManager\$Stub")
        val asInterfaceMethod = iPmStub.getMethod("asInterface", IBinder::class.java)
        val iPmInstance = asInterfaceMethod.invoke(
            null,
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
        )
        val iParceledListSliceClass = Class.forName("android.content.pm.ParceledListSlice")
        val retAsInner: Any
        if (atLeastT()) {
            retAsInner = HiddenApiBypass.invoke(
                iPmInstance::class.java,
                iPmInstance,
                "getInstalledPackages",
                flags.toLong(),
                userId
            )
        } else {
            retAsInner = HiddenApiBypass.invoke(
                iPmInstance::class.java,
                iPmInstance,
                "getInstalledPackages",
                flags,
                userId
            )
        }
        @Suppress("UNCHECKED_CAST")
        return HiddenApiBypass.invoke(
            iParceledListSliceClass,
            retAsInner,
            "getList"
        ) as List<PackageInfo>
    }

    private val appsFiltered: List<PackageInfo> by lazy {
        // get all the apps
        val result = ArrayList<PackageInfo>()
        allApps.forEach {
            if (result.find { pkg -> pkg.packageName == it.packageName } == null && (!isActivitiesFetched() || !it.activities.isNullOrEmpty())) {
                // filter for multi-user and filter those without activities
                result.add(it)
            }
        }
        result
    }

    val parsedApps: List<ParsedPackage> by lazy {
        appsFiltered.map { ParsedPackage(it, packageManager) }
    }
}

class ParsedPackage(
    private val pkg: PackageInfo,
    private val packageManager: PackageManager
) {
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
        (pkg.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
    }
    private val packageNameLowerCase: String by lazy {
        packageName.lowercase()
    }
    private val appNameLowerCase: String by lazy {
        appName.lowercase()
    }

    fun isLowerCasedSearchMatch(searchContent: String): Boolean {
        return packageNameLowerCase.contains(searchContent) || appNameLowerCase.contains(
            searchContent
        )
    }
}