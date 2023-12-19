package moe.lyniko.hiderecent

// https://stackoverflow.com/a/63877349
// https://stackoverflow.com/a/1109108
import android.os.Bundle
import android.os.Process.myUserHandle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import moe.lyniko.hiderecent.ui.AppNavHost
import moe.lyniko.hiderecent.ui.BottomNavigation
import moe.lyniko.hiderecent.ui.theme.MyApplicationTheme
import moe.lyniko.hiderecent.utils.PreferenceUtils
import moe.lyniko.hiderecent.utils.getIdByUserHandle
import moe.lyniko.hiderecent.utils.isShizukuAvailable
import moe.lyniko.hiderecent.utils.isShizukuNeeded
import kotlin.system.exitProcess


class MainActivity : ComponentActivity() {
    private var snackbarHostState = SnackbarHostState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            PreferenceUtils.getInstance(this)
        } catch (e: SecurityException) {
            Toast.makeText(this, getString(R.string.not_activated), Toast.LENGTH_LONG).show()
            finish()
            return
        }
        setContent {
            MyApplicationTheme {

                val scope = rememberCoroutineScope()
                val snackbarHostStateRemember = remember { snackbarHostState }
                val navController = rememberNavController()

                Scaffold(
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostStateRemember)
                    },
                    bottomBar = {
                        BottomNavigation(navController = navController)
                    }
                ) { innerPadding ->
                    AppNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                // check main user
                val userId = getUserId()
                if (userId != 0) {
                    LaunchedEffect(snackbarHostState) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                getString(R.string.main_user_only, userId),
                                actionLabel = getString(R.string.dismiss_notification),
                                duration = SnackbarDuration.Indefinite
                            )
                        }
                    }
                    Log.w(BuildConfig.APPLICATION_ID, "Wrong user id: $userId")
                }

                // check shizuku
                if (isShizukuNeeded(this) && !isShizukuAvailable()) {
                    LaunchedEffect(snackbarHostState) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                getString(R.string.shizuku_not_available_toast),
                                actionLabel = getString(R.string.dismiss_notification),
                                duration = SnackbarDuration.Indefinite
                            )
                        }
                    }
                    Log.w(BuildConfig.APPLICATION_ID, "Shizuku not running")
                }
            }
        }
    }

}

fun getUserId(): Int {
    val userHandle = myUserHandle()
    var userId = 0
    try {
        userId = getIdByUserHandle(userHandle)
    } catch (e: Exception) {
        Log.e(BuildConfig.APPLICATION_ID, "Error when getting user id: ${e.message}")
    }
    return userId
}