package com.example.attendance.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendance.data.settings.SettingsDataStore
import com.example.attendance.data.settings.SettingsDataStore.CompanyLocation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsDataStore: SettingsDataStore) : ViewModel() {
    
    val companyLocation: StateFlow<CompanyLocation> = settingsDataStore.companyLocationFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CompanyLocation()
        )
    
    fun saveCompanyLocation(
        latitude: String?,
        longitude: String?,
        address: String?,
        geofenceRadiusMeters: String,
        enableWeekendReminder: Boolean
    ) {
        viewModelScope.launch {
            val lat = latitude?.toDoubleOrNull()
            val lng = longitude?.toDoubleOrNull()
            val radius = geofenceRadiusMeters.toFloatOrNull() ?: 150f
            
            settingsDataStore.saveCompanyLocation(
                latitude = lat,
                longitude = lng,
                address = address?.takeIf { it.isNotBlank() },
                geofenceRadiusMeters = radius,
                enableWeekendReminder = enableWeekendReminder
            )
        }
    }
    
    fun clearCompanyLocation() {
        viewModelScope.launch {
            settingsDataStore.clearCompanyLocation()
        }
    }
}