package moe.lyniko.hiderecent

import android.app.Application
import android.content.res.Resources
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

// https://stackoverflow.com/a/54686443/22911792
class MyApplication : Application(), XposedServiceHelper.OnServiceListener {
    companion object {
        lateinit var instance: Application
        lateinit var resourcesPublic: Resources

        @Volatile
        var xposedService: XposedService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        resourcesPublic = resources
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        xposedService = service
        syncPreferencesToRemote(service)
    }

    private fun syncPreferencesToRemote(service: XposedService) {
        try {
            val localPref = getSharedPreferences(
                moe.lyniko.hiderecent.utils.PreferenceUtils.functionalConfigName,
                MODE_PRIVATE
            )
            val packages = moe.lyniko.hiderecent.utils.PreferenceUtils.getPackageListFromPref(localPref)
            if (packages.isNotEmpty()) {
                service.getRemotePreferences(
                    moe.lyniko.hiderecent.utils.PreferenceUtils.functionalConfigName
                ).edit().putStringSet("packages", packages).apply()
            }
        } catch (_: Throwable) {
        }
    }

    override fun onServiceDied(service: XposedService) {
        xposedService = null
    }
}