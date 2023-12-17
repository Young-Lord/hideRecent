package moe.lyniko.hiderecent.ui

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


@Composable
fun SettingsView() {
    val context = LocalContext.current
    val managerPref = PreferenceUtils(context).managerPref
    ProvidePreferenceLocals(
        flow = managerPref.getPreferenceFlow()
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                var switchMutableState by remember {
                    mutableStateOf(
                        managerPref.getBoolean(
                            ConfigKeys.ShowPackageForAllUser.key,
                            false
                        )
                    )
                }
                SwitchPreference(
                    value = switchMutableState,
                    onValueChange = {
                        switchMutableState = it; managerPref.edit()
                        .putBoolean(ConfigKeys.ShowPackageForAllUser.key, it)
                        .apply(); isShizukuAvailable()
                    },
                    title = { Text(context.getString(R.string.show_package_for_all_user)) },
                    summary = { Text(context.getString(R.string.show_package_for_all_user_summary)) },
                )
            }
        }
    }
}