package com.gardiyan.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gardiyan.app.ui.screens.DashboardScreen
import com.gardiyan.app.ui.screens.PermissionsScreen
import com.gardiyan.app.ui.screens.ProfileScreen
import com.gardiyan.app.ui.screens.ProtectedAppsScreen
import com.gardiyan.app.ui.screens.SetupTargetScreen
import com.gardiyan.app.viewmodel.GuardianViewModel

const val ROUTE_PERMISSIONS = "permissions"
const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_PROTECTED = "protected"
const val ROUTE_SETUP = "setup"
const val ROUTE_SETTINGS = "settings"

@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: GuardianViewModel,
    isOverlayEnabled: Boolean,
    isUsageEnabled: Boolean,
    isAccessibilityEnabled: Boolean,
    isBatteryExempted: Boolean,
    isNotificationsEnabled: Boolean
) {
    val hasAllPermissions = isOverlayEnabled && isUsageEnabled && isAccessibilityEnabled && isBatteryExempted && isNotificationsEnabled

    NavHost(
        navController = navController,
        startDestination = if (hasAllPermissions) ROUTE_DASHBOARD else ROUTE_PERMISSIONS
    ) {
        composable(ROUTE_PERMISSIONS) {
            PermissionsScreen(
                viewModel = viewModel,
                isOverlayEnabled = isOverlayEnabled,
                isUsageEnabled = isUsageEnabled,
                isAccessibilityEnabled = isAccessibilityEnabled,
                isBatteryExempted = isBatteryExempted,
                isNotificationsEnabled = isNotificationsEnabled,
                onNavigateToDashboard = {
                    navController.navigate(ROUTE_DASHBOARD) {
                        popUpTo(ROUTE_PERMISSIONS) { inclusive = true }
                    }
                }
            )
        }
        composable(ROUTE_DASHBOARD) {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToSetup = {
                    navController.navigate(ROUTE_SETUP)
                },
                onNavigateToProtected = {
                    navController.navigate(ROUTE_PROTECTED) {
                        popUpTo(ROUTE_DASHBOARD) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable(ROUTE_PROTECTED) {
            ProtectedAppsScreen(
                viewModel = viewModel
            )
        }
        composable(ROUTE_SETUP) {
            SetupTargetScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onCompleted = {
                    navController.navigate(ROUTE_DASHBOARD) {
                        popUpTo(ROUTE_SETUP) { inclusive = true }
                    }
                }
            )
        }
        composable(ROUTE_SETTINGS) {
            ProfileScreen(
                viewModel = viewModel,
                isOverlayEnabled = isOverlayEnabled,
                isUsageEnabled = isUsageEnabled,
                isAccessibilityEnabled = isAccessibilityEnabled,
                isBatteryExempted = isBatteryExempted,
                isNotificationsEnabled = isNotificationsEnabled
            )
        }
    }
}
