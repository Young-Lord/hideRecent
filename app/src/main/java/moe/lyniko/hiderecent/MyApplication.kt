package moe.lyniko.hiderecent

import android.app.Application
import android.content.res.Resources

// https://stackoverflow.com/a/54686443/22911792
class MyApplication : Application() {
    companion object {
        lateinit var instance: Application
        lateinit var resourcesPublic: Resources
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        resourcesPublic = resources
    }
}