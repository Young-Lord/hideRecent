package moe.lyniko.hiderecent.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import moe.lyniko.hiderecent.MyApplication
import moe.lyniko.hiderecent.R

@SuppressLint("WorldReadableFiles")
class PreferenceUtils( // init context on constructor
    context: Context
) {
    // ------ 1. get several SharedPreferences (funcPref is the only accessible during Xposed inject) ------
    private var funcPref: SharedPreferences = try {
        @Suppress("DEPRECATION")
        context.getSharedPreferences(functionalConfigName, Context.MODE_WORLD_READABLE)
    } catch (e: SecurityException) {
        throw e
        // Log.w("PreferenceUtil", "Fallback to Private SharedPref for error!!!: ${e.message}")
        // context.getSharedPreferences(functionalConfigName, Context.MODE_PRIVATE)
    }

    @Suppress("DEPRECATION")
    var managerPref: SharedPreferences =
        context.getSharedPreferences(managerConfigName, Context.MODE_WORLD_READABLE)

    @Suppress("DEPRECATION")
    private val legacyFuncPref = context.getSharedPreferences(legacyConfigName, Context.MODE_WORLD_READABLE)
    
    // ------ 2. init packages ------
    private fun initPackageFromLegacyAndNew(funcPref: SharedPreferences, legacyPref: SharedPreferences) {
        val legacyPackages = legacyPref.getString(legacyModeStringMode, "")?.removeSurrounding("#")?.split("##")?.toMutableSet()
        val newPackages = getPackageListFromPref(funcPref)
        if(newPackages.isEmpty() && !legacyPackages.isNullOrEmpty()) {
            // remove legacy data only if only legacy one has data.
            // Log.d("PreferenceUtil", "initPackageFromLegacyAndNew: $legacyPackages")
            legacyPref.edit().remove(legacyModeStringMode).apply()
            packages = legacyPackages
            commitPackageList()
        } else {
            packages = newPackages
        }
        packages.remove("") // have no idea why this occurs.
    }

    private lateinit var packages: MutableSet<String>

    init {
        initPackageFromLegacyAndNew(funcPref, legacyFuncPref)
    }
    companion object {

        @Volatile
        private var instance: PreferenceUtils? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: PreferenceUtils(context).also { instance = it }
            }

        private const val packagesTag = "packages"
        const val functionalConfigName = "functional_config"
        const val managerConfigName = "manager_config"
        private const val legacyConfigName = "config"
        private const val legacyModeStringMode = "Mode"

        enum class ConfigKeys(val key: String, val default: Boolean) {
            ShowPackageForAllUser("show_package_for_all_user", false),
            HideNoActivityPackages("hide_no_activity_packages", true)
        }

        fun getPackageListFromPref(pref: SharedPreferences): MutableSet<String> {
            val currentPackageSet = pref.getStringSet(packagesTag, HashSet<String>())
            return currentPackageSet!!.toMutableSet()
        }
    }


    private fun commitPackageList() {
        funcPref.edit().putStringSet(packagesTag, packages).apply()
    }

    fun addPackage(pkg: String): Int {
        if(pkg.isEmpty() || pkg == "*") return 0
        val ret = if (packages.add(pkg)) 1 else 0
        // Log.w("PreferenceUtil", "addPackage: $pkg -> $ret")
        commitPackageList()
        return ret
    }

    fun removePackage(pkg: String): Int {
        val ret: Int
        if (pkg == "*") {
            ret = packages.size
            packages.clear()
        } else {
            ret = if (packages.remove(pkg)) 1 else 0
        }
        // Log.w("PreferenceUtil", "removePackage: $pkg -> $ret")
        commitPackageList()
        return ret
    }

    fun isPackageInList(pkg: String): Boolean {
        // Log.d("PreferenceUtil", "isPackageInList: $pkg -> ${packages.contains(pkg)}")
        return packages.contains(pkg)
    }

    fun packagesToString(version: Int = 1): String {
        when (version) {
            1 -> {
                var result =
                    "# version=$version\n# -* # ${MyApplication.resourcesPublic.getString(R.string.export_uncomment_hint)}\n"
                packages.forEach { pkg ->
                    result += "+$pkg\n"
                }
                if (packages.isEmpty()){
                    result += "# +com.example.package  # ${MyApplication.resourcesPublic.getString(R.string.export_demo_hint)}\n"
                }
                return result
            }
            else -> throw NotImplementedError("Version $version is not implemented")
        }
    }
    private fun validatePackageNameOrAsterisk(pkg: String): Boolean {
        if (pkg == "*") return true
        // https://stackoverflow.com/a/40772073
        @Suppress("RegExpSimplifiable")
        return pkg.matches(Regex("^([A-Za-z]{1}[A-Za-z\\d_]*\\.)+[A-Za-z][A-Za-z\\d_]*\$"))
    }

    fun packagesFromString(str: String): Int {
        val lines = str.split("\n")
        // read first line for version
        val version: Int
        var changed = 0
        try {
            version = lines[0].split("=")[1].toInt()
        } catch (e: Exception) {
            e.printStackTrace()
            throw NotImplementedError("Version is not specified")
        }
        when (version) {
            1 -> {
                lines.forEach { line ->
                    // remove comments start with #
                    val lineWithoutComment = line.split("#")[0].trim()
                    // skip if empty
                    if (lineWithoutComment.isEmpty()) return@forEach
                    // get action & package name
                    val action = lineWithoutComment[0]
                    val currentPackage = lineWithoutComment.substring(1)
                    if (currentPackage.isEmpty()) return@forEach
                    if (!validatePackageNameOrAsterisk(currentPackage)) throw NotImplementedError("Invalid package name: $currentPackage")
                    @Suppress("LiftReturnOrAssignment")
                    when (action) {
                        '+' -> {
                            changed += addPackage(currentPackage)
                        }
                        '-' -> {
                            changed += removePackage(currentPackage)
                        }
                        else -> {
                            throw NotImplementedError("Action $action is not implemented")
                        }
                    }
                }
            }

            else -> throw NotImplementedError("Version $version is not implemented")
        }
        return changed
    }
}