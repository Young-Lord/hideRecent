package moe.lyniko.hiderecent

import android.app.Application
import android.content.res.Resources
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import moe.lyniko.hiderecent.utils.PreferenceUtils

// https://stackoverflow.com/a/54686443/22911792
class MyApplication : Application(), XposedServiceHelper.OnServiceListener {
    companion object {
        lateinit var instance: Application
        lateinit var resourcesPublic: Resources
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        resourcesPublic = resources
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        PreferenceUtils.getInstance(this).attachRemoteService(service)
    }

    override fun onServiceDied(service: XposedService) {
        PreferenceUtils.getInstance(this).attachRemoteService(null)
    }
}
