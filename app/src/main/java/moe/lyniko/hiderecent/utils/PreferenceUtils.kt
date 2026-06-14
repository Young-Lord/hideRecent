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
    // ------ 1. get several SharedPreferences ------
    // Vector/LSPosed hooks checkMode to allow MODE_WORLD_READABLE when the module
    // is in its own scope. Fall back to MODE_PRIVATE if the hook isn't active.
    private var funcPref: SharedPreferences = openWorldReadable(context, functionalConfigName)

    var managerPref: SharedPreferences = openWorldReadable(context, managerConfigName)

    private val legacyFuncPref = openWorldReadable(context, legacyConfigName)
    
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
    private var contentHiddenPackages: MutableSet<String> = getContentHiddenPackageListFromPref(funcPref)

    init {
        initPackageFromLegacyAndNew(funcPref, legacyFuncPref)
        contentHiddenPackages.remove("")
        if (contentHiddenPackages.removeAll(packages)) {
            commitPackageList()
        }
    }

    enum class PackageHideMode(val configValue: String) {
        NONE("none"),
        HIDE_TASK("hide_task"),
        HIDE_CONTENT("hide_content");

        companion object {
            fun fromConfigValue(value: String): PackageHideMode? {
                return entries.firstOrNull { it.configValue == value }
            }
        }
    }

    companion object {

        @Volatile
        private var instance: PreferenceUtils? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: PreferenceUtils(context).also { instance = it }
            }

        @Suppress("DEPRECATION")
        private fun openWorldReadable(context: Context, name: String): SharedPreferences {
            return try {
                context.getSharedPreferences(name, Context.MODE_WORLD_READABLE)
            } catch (_: SecurityException) {
                context.getSharedPreferences(name, Context.MODE_PRIVATE)
            }
        }

        private const val packagesTag = "packages"
        private const val contentHiddenPackagesTag = "content_hidden_packages"
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

        fun getContentHiddenPackageListFromPref(pref: SharedPreferences): MutableSet<String> {
            val currentPackageSet = pref.getStringSet(contentHiddenPackagesTag, HashSet<String>())
            return currentPackageSet!!.toMutableSet()
        }
    }


    private fun commitPackageList() {
        funcPref.edit()
            .putStringSet(packagesTag, packages)
            .putStringSet(contentHiddenPackagesTag, contentHiddenPackages)
            .apply()
    }

    fun addPackage(pkg: String): Int {
        val ret = setPackageMode(pkg, PackageHideMode.HIDE_TASK)
        // Log.w("PreferenceUtil", "addPackage: $pkg -> $ret")
        return ret
    }

    fun removePackage(pkg: String): Int {
        val ret = if (pkg == "*") clearPackageModes() else setPackageMode(pkg, PackageHideMode.NONE)
        // Log.w("PreferenceUtil", "removePackage: $pkg -> $ret")
        return ret
    }

    fun isPackageInList(pkg: String): Boolean {
        // Log.d("PreferenceUtil", "isPackageInList: $pkg -> ${packages.contains(pkg)}")
        return packages.contains(pkg)
    }

    fun isPackageContentHidden(pkg: String): Boolean {
        return contentHiddenPackages.contains(pkg)
    }

    fun getPackageMode(pkg: String): PackageHideMode {
        return when {
            packages.contains(pkg) -> PackageHideMode.HIDE_TASK
            contentHiddenPackages.contains(pkg) -> PackageHideMode.HIDE_CONTENT
            else -> PackageHideMode.NONE
        }
    }

    fun setPackageMode(pkg: String, mode: PackageHideMode): Int {
        if(pkg.isEmpty() || pkg == "*") return 0
        val oldMode = getPackageMode(pkg)
        when (mode) {
            PackageHideMode.NONE -> {
                packages.remove(pkg)
                contentHiddenPackages.remove(pkg)
            }
            PackageHideMode.HIDE_TASK -> {
                packages.add(pkg)
                contentHiddenPackages.remove(pkg)
            }
            PackageHideMode.HIDE_CONTENT -> {
                packages.remove(pkg)
                contentHiddenPackages.add(pkg)
            }
        }
        commitPackageList()
        return if (oldMode == mode) 0 else 1
    }

    private fun clearPackageModes(): Int {
        val changed = (packages + contentHiddenPackages).size
        packages.clear()
        contentHiddenPackages.clear()
        commitPackageList()
        return changed
    }

    fun packagesToString(version: Int = 2): String {
        when (version) {
            1 -> {
                var result =
                    "# version=$version\n# -* # ${MyApplication.resourcesPublic.getString(R.string.export_uncomment_hint)}\n"
                packages.sorted().forEach { pkg ->
                    result += "+$pkg\n"
                }
                if (packages.isEmpty()){
                    result += "# +com.example.package  # ${MyApplication.resourcesPublic.getString(R.string.export_demo_hint)}\n"
                }
                return result
            }
            2 -> {
                var result =
                    "# version=$version\n# -* # ${MyApplication.resourcesPublic.getString(R.string.export_uncomment_hint)}\n"
                packages.sorted().forEach { pkg ->
                    result += "$pkg=${PackageHideMode.HIDE_TASK.configValue}\n"
                }
                contentHiddenPackages.sorted().forEach { pkg ->
                    result += "$pkg=${PackageHideMode.HIDE_CONTENT.configValue}\n"
                }
                if (packages.isEmpty() && contentHiddenPackages.isEmpty()){
                    result += "# com.example.package=${PackageHideMode.HIDE_TASK.configValue}  # ${MyApplication.resourcesPublic.getString(R.string.export_demo_hint)}\n"
                    result += "# com.example.private=${PackageHideMode.HIDE_CONTENT.configValue}\n"
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
            2 -> {
                lines.forEach { line ->
                    val lineWithoutComment = line.split("#")[0].trim()
                    if (lineWithoutComment.isEmpty()) return@forEach

                    if (lineWithoutComment[0] == '-') {
                        val currentPackage = lineWithoutComment.substring(1)
                        if (currentPackage.isEmpty()) return@forEach
                        if (!validatePackageNameOrAsterisk(currentPackage)) throw NotImplementedError("Invalid package name: $currentPackage")
                        changed += removePackage(currentPackage)
                        return@forEach
                    }

                    val entry = lineWithoutComment.split("=", limit = 2)
                    if (entry.size != 2) throw NotImplementedError("Invalid config entry: $lineWithoutComment")
                    val currentPackage = entry[0].trim()
                    val mode = PackageHideMode.fromConfigValue(entry[1].trim())
                        ?: throw NotImplementedError("Invalid hide mode: ${entry[1].trim()}")
                    if (currentPackage.isEmpty() || !validatePackageNameOrAsterisk(currentPackage)) throw NotImplementedError("Invalid package name: $currentPackage")
                    changed += setPackageMode(currentPackage, mode)
                }
            }

            else -> throw NotImplementedError("Version $version is not implemented")
        }
        return changed
    }
}
