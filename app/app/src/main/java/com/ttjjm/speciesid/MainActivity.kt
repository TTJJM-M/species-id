package com.ttjjm.speciesid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ttjjm.speciesid.ui.camera.CameraScreen
import com.ttjjm.speciesid.ui.guide.GuideDetailScreen
import com.ttjjm.speciesid.ui.guide.GuideScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SpeciesIdRoot() }
    }
}

private sealed class Tab(val route: String, val label: String) {
    data object Camera : Tab("camera", "拍照")
    data object Guide : Tab("guide", "图鉴")
}

@Composable
private fun SpeciesIdRoot() {
    val navController = rememberNavController()
    val tabs = listOf(Tab.Camera, Tab.Guide)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    // 详情页全屏,不显示底部导航
    val showBottomBar = currentDestination?.route?.startsWith("guide_detail") != true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (tab is Tab.Camera) Icons.Default.CameraAlt else Icons.Default.MenuBook,
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Camera.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.Camera.route) { CameraScreen(onOpenSettings = {}) }
            composable(Tab.Guide.route) {
                GuideScreen(onOpenDetail = { id -> navController.navigate("guide_detail/$id") })
            }
            composable(
                route = "guide_detail/{recordId}",
                arguments = listOf(navArgument("recordId") { type = NavType.LongType }),
            ) { entry ->
                val recordId = entry.arguments?.getLong("recordId") ?: return@composable
                GuideDetailScreen(
                    recordId = recordId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}