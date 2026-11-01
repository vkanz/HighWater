package ru.systemt.highwater.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.systemt.highwater.ui.devices.DeviceSelectionScreen
import ru.systemt.highwater.ui.main.MainScreen
import ru.systemt.highwater.ui.measurement.MeasurementScreen
import ru.systemt.highwater.ui.navigation.Routes

@Composable
fun HighWaterApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.MAIN,
    ) {
        composable(Routes.MAIN) {
            MainScreen(
                onSelectDevice = {
                    navController.navigate(Routes.DEVICE_SELECTION) {
                        launchSingleTop = true
                    }
                },
                onStartMeasurement = {
                    navController.navigate(Routes.MEASUREMENT) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Routes.DEVICE_SELECTION) {
            DeviceSelectionScreen(
                onBack = { navController.popBackStack() },
                onDeviceSelected = {
                    navController.navigate(Routes.MEASUREMENT) {
                        popUpTo(Routes.DEVICE_SELECTION) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Routes.MEASUREMENT) {
            MeasurementScreen(
                onBack = { navController.popBackStack() },
                onOpenDeviceSelection = {
                    navController.navigate(Routes.DEVICE_SELECTION) {
                        popUpTo(Routes.MEASUREMENT) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
