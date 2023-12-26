package moe.lyniko.hiderecent.ui

import moe.lyniko.hiderecent.BuildConfig
import moe.lyniko.hiderecent.R
import android.content.Context
import android.content.ContextWrapper
import android.os.Process.myUserHandle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.launch
import moe.lyniko.hiderecent.ui.theme.MyApplicationTheme
import moe.lyniko.hiderecent.utils.AppUtils
import moe.lyniko.hiderecent.utils.ParsedPackage
import moe.lyniko.hiderecent.utils.PreferenceUtils
import moe.lyniko.hiderecent.utils.PreferenceUtils.Companion.ConfigKeys
import moe.lyniko.hiderecent.utils.getIdByUserHandle

fun Context.getActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

private lateinit var appUtils: AppUtils
private lateinit var preferenceUtils: PreferenceUtils
private var searchContent: MutableState<String> = mutableStateOf("")
private var showUserAppInsteadOfSystem: MutableState<Boolean> = mutableStateOf(true)
private var snackbarHostState = SnackbarHostState()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeView() {
    val context = LocalContext.current
    appUtils = AppUtils.getInstance(context)
    preferenceUtils = PreferenceUtils.getInstance(context)
    MyApplicationTheme {
        var searchContentRemember by remember { searchContent }
        var showUserAppInsteadOfSystemRemember by remember { showUserAppInsteadOfSystem }
        val scope = rememberCoroutineScope()
        val snackbarHostStateRemember = remember { snackbarHostState }

        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostStateRemember)
            },
            topBar = {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchContentRemember,
                            onValueChange = { searchContentRemember = it },
                            placeholder = { Text(context.getString(R.string.search_text)) },
                            singleLine = true,
                            // action done
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    context.getActivity()?.currentFocus?.let { view ->
                                        val imm =
                                            context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                        imm?.hideSoftInputFromWindow(view.windowToken, 0)
                                    }
                                }
                            ),
                            // disable background
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    actions = {
                        if (searchContentRemember.isNotEmpty()) {
                            IconButton(onClick = {
                                // clear search
                                searchContentRemember = ""
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = context.getString(R.string.clear_search)
                                )
                            }
                        }
                        IconButton(onClick = {
                            showUserAppInsteadOfSystemRemember =
                                !showUserAppInsteadOfSystem.value
                        }) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.baseline_swap_horiz_24),
                                contentDescription = context.getString(R.string.switch_app_type)
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            AppListForPackages(getDisplayApps(), modifier = Modifier.padding(innerPadding))
        }

        // check main user
        val userHandle = myUserHandle()
        var userId = 0
        try {
            userId = getIdByUserHandle(userHandle)
        } catch (e: Exception) {
            Log.e(BuildConfig.APPLICATION_ID, "Error when getting user id: ${e.message}")
        }
        if (userId != 0) {
            LaunchedEffect(snackbarHostState) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.main_user_only, userId),
                        actionLabel = context.getString(R.string.dismiss_notification),
                        duration = SnackbarDuration.Indefinite
                    )
                }
            }
            Log.w(
                BuildConfig.APPLICATION_ID,
                "Wrong user id: ${getIdByUserHandle(userHandle)}"
            )
        }
        if (appUtils.parsedApps.isEmpty()) {
            LaunchedEffect(snackbarHostState) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.apps_not_fetched),
                        actionLabel = context.getString(R.string.dismiss_notification),
                        duration = SnackbarDuration.Indefinite
                    )
                }
            }
        } else if (!appUtils.isActivitiesFetched() && preferenceUtils.managerPref.getBoolean(
                ConfigKeys.HideNoActivityPackages.key, ConfigKeys.HideNoActivityPackages.default
            )
        ) {
            LaunchedEffect(snackbarHostState) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.activity_not_fetched),
                        actionLabel = context.getString(R.string.dismiss_notification),
                        duration = SnackbarDuration.Indefinite
                    )
                }
            }
        }
    }
}

private fun getDisplayApps(): List<ParsedPackage> {
    var appsFiltered =
        appUtils.parsedApps.filter { showUserAppInsteadOfSystem.value xor it.isSystemApp }
    val trimmedLowerCasedSearch = searchContent.value.trim().lowercase()
    appsFiltered = if (trimmedLowerCasedSearch.length <= 1) {
        appsFiltered
    } else appsFiltered.filter { it.isLowerCasedSearchMatch(trimmedLowerCasedSearch) }
    // sort by app name and package name
    appsFiltered = appsFiltered.sortedWith(compareByDescending<ParsedPackage> {
        preferenceUtils.isPackageInList(it.packageName)
    }.thenBy { it.appName }.thenBy { it.packageName })
    // appsFiltered = appsFiltered.sortedBy { it.packageName }.sortedBy { it.appName }.sortedBy{ !preferenceUtils.isPackageInList(it.packageName) }
    return appsFiltered
}

// https://jetpackcompose.cn/docs/tutorial/
@Composable
private fun SingleAppCardForPackage(app: ParsedPackage) {
    Row(
        modifier = Modifier.padding(all = 8.dp) // 在我们的 Card 周围添加 padding
    ) {
        Image(
            //image is app icon
            painter = rememberDrawablePainter(app.appIcon),
            contentDescription = null,
            modifier = Modifier
                .size(50.dp) // 改变 Image 元素的大小
        )
        Spacer(Modifier.padding(horizontal = 8.dp)) // 添加一个空的控件用来填充水平间距，设置 padding 为 8.dp
        Column(
            modifier = Modifier
                .weight(1f) // 设置 Column 的 weight 为 1，使其占据剩余空间
        ) {
            Text(
                text = app.appName,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.padding(vertical = 4.dp))
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Column {
            var checked by remember { mutableStateOf(preferenceUtils.isPackageInList(app.packageName)) }
            Switch(checked = checked, onCheckedChange = {
                if (it) {
                    preferenceUtils.addPackage(app.packageName)
                } else {
                    preferenceUtils.removePackage(app.packageName)
                }
                // update checked state
                checked = it
            })
        }
    }
}

@Composable
private fun AppListForPackages(apps: List<ParsedPackage>, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.medium, // 使用 MaterialTheme 自带的形状
        modifier = modifier
            .fillMaxWidth(),
    ) {
        LazyColumn {
            items(
                count = apps.size,
                key = { app_index -> apps[app_index].packageName }
            ) { app_index ->
                SingleAppCardForPackage(apps[app_index])
            }
        }
    }
}