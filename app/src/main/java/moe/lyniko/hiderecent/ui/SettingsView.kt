package moe.lyniko.hiderecent.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.getPreferenceFlow
import me.zhanghai.compose.preference.SwitchPreference
import moe.lyniko.hiderecent.R
import moe.lyniko.hiderecent.utils.PreferenceUtils
import moe.lyniko.hiderecent.utils.PreferenceUtils.Companion.ConfigKeys
import moe.lyniko.hiderecent.utils.isShizukuAvailable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.switchPreference


@Composable
fun SettingsView() {
    val context = LocalContext.current
    val managerPref = PreferenceUtils.getInstance(context).managerPref
    ProvidePreferenceLocals(
        flow = managerPref.getPreferenceFlow()
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                var switchMutableState by remember {
                    mutableStateOf(
                        managerPref.getBoolean(
                            ConfigKeys.ShowPackageForAllUser.key,
                            ConfigKeys.ShowPackageForAllUser.default
                        )
                    )
                }
                SwitchPreference(
                    value = switchMutableState,
                    onValueChange = {
                        switchMutableState = it
                        managerPref.edit()
                        .putBoolean(ConfigKeys.ShowPackageForAllUser.key, it)
                        .apply()
                        if(it) isShizukuAvailable()
                    },
                    title = { Text(context.getString(R.string.show_package_for_all_user)) },
                    summary = { Text(context.getString(R.string.show_package_for_all_user_summary)) },
                )
            }
            switchPreference(
                key=ConfigKeys.HideNoActivityPackages.key,
                defaultValue = ConfigKeys.HideNoActivityPackages.default,
                title = { Text(context.getString(R.string.hide_no_activity_packages)) },
                summary = { Text(context.getString(R.string.hide_no_activity_packages_summary)) },
            )
            item {
                Preference(
                    title = { Text(context.getString(R.string.export_config)) },
                    onClick = {
                        // clipboard
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("config", PreferenceUtils.getInstance(context).packagesToString())
                        try {
                            clipboard.setPrimaryClip(clip)
                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                                Toast.makeText(context, context.getString(R.string.export_config_to_clipboard_success), Toast.LENGTH_SHORT).show()
                        }
                        catch (e: SecurityException) {
                            Toast.makeText(context, context.getString(R.string.export_config_to_clipboard_failed), Toast.LENGTH_SHORT).show()
                            e.printStackTrace()
                        }
                    }
                )
            }
            item {
                Preference(
                    title = { Text(context.getString(R.string.import_config)) },
                    onClick = {
                        // clipboard
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                        val clipboardData: CharSequence?
                        try {
                            clipboardData =
                                clipboard.primaryClip?.getItemAt(0)?.text
                        }
                        catch(e: SecurityException){
                            Toast.makeText(context, context.getString(R.string.import_config_failed_perm), Toast.LENGTH_SHORT).show()
                            return@Preference
                        }
                        if(clipboardData.isNullOrEmpty()){
                            Toast.makeText(context, context.getString(R.string.import_config_failed_empty), Toast.LENGTH_SHORT).show()
                            return@Preference
                        }
                        try{
                            val changed = PreferenceUtils.getInstance(context).packagesFromString(clipboardData.toString())
                            Toast.makeText(context, context.resources.getQuantityString(R.plurals.import_config_success_count, changed, changed), Toast.LENGTH_SHORT).show()
                        }
                        catch (e: NotImplementedError){
                            Toast.makeText(context, context.getString(R.string.import_config_failed_wrong), Toast.LENGTH_SHORT).show()
                            e.printStackTrace()
                        }
                    }
                )
            }
        }
    }
}