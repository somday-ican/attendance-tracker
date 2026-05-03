package com.example.attendance.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.attendance.data.local.AppDatabase
import com.example.attendance.data.repository.AttendanceRepository
import com.example.attendance.data.settings.SettingsDataStore
import com.example.attendance.location.GeofenceManager
import com.example.attendance.ui.home.HomeScreen
import com.example.attendance.ui.home.HomeViewModel
import com.example.attendance.ui.records.RecordsScreen
import com.example.attendance.ui.records.RecordsViewModel
import com.example.attendance.ui.settings.CompanyLocationPickerScreen
import com.example.attendance.ui.settings.CompanyLocationPickerViewModel
import com.example.attendance.ui.settings.SettingsScreen
import com.example.attendance.ui.settings.SettingsViewModel

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val RECORDS = "records"
    const val COMPANY_LOCATION_PICKER = "company_location_picker"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val settingsDataStore = remember { SettingsDataStore(context) }
    val database = remember { AppDatabase.getInstance(context) }
    val repository = remember { AttendanceRepository(database.attendanceDao(), settingsDataStore) }
    val geofenceManager = remember { GeofenceManager(context) }

    val companyLocation by settingsDataStore.companyLocationFlow
        .collectAsState(initial = com.example.attendance.data.settings.SettingsDataStore.CompanyLocation())

    LaunchedEffect(companyLocation.latitude, companyLocation.longitude, companyLocation.geofenceRadiusMeters) {
        val lat = companyLocation.latitude
        val lng = companyLocation.longitude
        val radius = companyLocation.geofenceRadiusMeters
        if (lat != null && lng != null) {
            geofenceManager.registerGeofence(lat, lng, radius)
        } else {
            geofenceManager.removeGeofence()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return HomeViewModel(repository) as T
                    }
                }
            )
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToRecords = { navController.navigate(Routes.RECORDS) }
            )
        }

        composable(Routes.SETTINGS) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return SettingsViewModel(settingsDataStore) as T
                    }
                }
            )
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPicker = { navController.navigate(Routes.COMPANY_LOCATION_PICKER) }
            )
        }

        composable(Routes.COMPANY_LOCATION_PICKER) {
            val pickerViewModel: CompanyLocationPickerViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return CompanyLocationPickerViewModel(settingsDataStore) as T
                    }
                }
            )
            CompanyLocationPickerScreen(
                viewModel = pickerViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.RECORDS) {
            val recordsViewModel: RecordsViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return RecordsViewModel(repository) as T
                    }
                }
            )
            RecordsScreen(
                viewModel = recordsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}