package moe.lyniko.hiderecent.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class PreferenceUtils( // init context on constructor
    context: Context
) {
    @SuppressLint("WorldReadableFiles")
    private var funcPref: SharedPreferences = try {
        @Suppress("DEPRECATION")
        context.getSharedPreferences(functionalConfigName, Context.MODE_WORLD_READABLE)
    } catch (e: SecurityException) {
        if(true){ // TODO: remove this catch. This is for developing on Android Emulator.
            throw e
        }
        Log.w("PreferenceUtil", "Fallback to Private SharedPref for error!!!: ${e.message}")
        context.getSharedPreferences(functionalConfigName, Context.MODE_PRIVATE)
    }
    var managerPref: SharedPreferences = context.getSharedPreferences(managerConfigName, Context.MODE_PRIVATE)
    private val packages: MutableSet<String> = getPackageListFromPref(funcPref)

    companion object {
        private const val packagesTag = "packages"
        const val functionalConfigName = "functional_config"
        const val managerConfigName = "manager_config"
        enum class ConfigKeys(val key: String) {
            ShowPackageForAllUser("show_package_for_all_user")
        }
        fun getPackageListFromPref(pref: SharedPreferences): MutableSet<String> {
            val currentPackageSet = pref.getStringSet(packagesTag, HashSet<String>())
            return currentPackageSet!!.toMutableSet()
        }
    }


    private fun commitPackageList() {
        funcPref.edit().putStringSet(packagesTag, packages).apply()
    }

    fun addPackage(pkg: String) {
        packages.add(pkg)
        commitPackageList()
    }

    fun removePackage(pkg: String) {
        packages.remove(pkg)
        commitPackageList()
    }

    fun isPackageInList(pkg: String): Boolean {
        return packages.contains(pkg)
    }
}