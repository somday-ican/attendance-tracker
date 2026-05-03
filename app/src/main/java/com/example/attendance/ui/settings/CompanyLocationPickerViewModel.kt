package com.example.attendance.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendance.data.settings.SettingsDataStore
import kotlinx.coroutines.launch

class CompanyLocationPickerViewModel(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    fun saveLocation(
        latitude: Double,
        longitude: Double,
        radius: Float,
        address: String,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            settingsDataStore.saveCompanyLocation(
                latitude = latitude,
                longitude = longitude,
                address = address,
                geofenceRadiusMeters = radius,
                enableWeekendReminder = true
            )
            onSaved()
        }
    }
}