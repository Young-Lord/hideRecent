package moe.lyniko.hiderecent.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import moe.lyniko.hiderecent.R

sealed class NavigationItem(val route: String, val icon: ImageVector, val title: Int) {
    object Home : NavigationItem("Home", Icons.Filled.Home, title = R.string.title_home)
    object Settings :
        NavigationItem("Settings", Icons.Filled.Settings, title = R.string.title_settings)

    object About : NavigationItem("About", Icons.Filled.Info, title = R.string.title_about)
}

@Composable
fun BottomNavigation(
    navController: NavHostController
) {
    val items = listOf(
        NavigationItem.Home,
        NavigationItem.Settings,
        NavigationItem.About,
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        items.forEach { item ->
            // Log.w("BottomNavigation", "item: $item; currentDestination: $currentDestination")
            NavigationBarItem(
                // Text that shows bellow the icon
                label = {
                    Text(text = LocalContext.current.getString(item.title))
                },
                // The icon resource
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = null
                    )
                },
                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                onClick = {
                    navController.navigate(item.route) {
                        // https://medium.com/@KaushalVasava/navigation-in-jetpack-compose-full-guide-beginner-to-advanced-950c1133740
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // re-selecting the same item
                        launchSingleTop = true
                        // Restore state when re-selecting a previously selected item
                        restoreState = true
                    }
                },
            )
        }
    }
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: String = NavigationItem.Home.route,
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavigationItem.Home.route) {
            HomeView()
        }
        composable(NavigationItem.Settings.route) {
            SettingsView()
        }
        composable(NavigationItem.About.route) {
            AboutView()
        }
    }
}