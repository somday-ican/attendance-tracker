package com.example.attendance.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsDataStore(private val context: Context) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
        
        private val COMPANY_LATITUDE = doublePreferencesKey("company_latitude")
        private val COMPANY_LONGITUDE = doublePreferencesKey("company_longitude")
        private val COMPANY_ADDRESS = stringPreferencesKey("company_address")
        private val GEOFENCE_RADIUS_METERS = floatPreferencesKey("geofence_radius_meters")
        private val ENABLE_WEEKEND_REMINDER = booleanPreferencesKey("enable_weekend_reminder")
    }
    
    data class CompanyLocation(
        val latitude: Double? = null,
        val longitude: Double? = null,
        val address: String? = null,
        val geofenceRadiusMeters: Float = 150f,
        val enableWeekendReminder: Boolean = true
    )
    
    val companyLocationFlow: Flow<CompanyLocation> = context.dataStore.data
        .map { preferences ->
            CompanyLocation(
                latitude = preferences[COMPANY_LATITUDE],
                longitude = preferences[COMPANY_LONGITUDE],
                address = preferences[COMPANY_ADDRESS],
                geofenceRadiusMeters = preferences[GEOFENCE_RADIUS_METERS] ?: 150f,
                enableWeekendReminder = preferences[ENABLE_WEEKEND_REMINDER] ?: true
            )
        }
    
    suspend fun saveCompanyLocation(
        latitude: Double?,
        longitude: Double?,
        address: String?,
        geofenceRadiusMeters: Float,
        enableWeekendReminder: Boolean
    ) {
        context.dataStore.edit { preferences ->
            if (latitude != null) {
                preferences[COMPANY_LATITUDE] = latitude
            } else {
                preferences.remove(COMPANY_LATITUDE)
            }
            
            if (longitude != null) {
                preferences[COMPANY_LONGITUDE] = longitude
            } else {
                preferences.remove(COMPANY_LONGITUDE)
            }
            
            if (address != null) {
                preferences[COMPANY_ADDRESS] = address
            } else {
                preferences.remove(COMPANY_ADDRESS)
            }
            
            preferences[GEOFENCE_RADIUS_METERS] = geofenceRadiusMeters
            preferences[ENABLE_WEEKEND_REMINDER] = enableWeekendReminder
        }
    }
    
    suspend fun clearCompanyLocation() {
        context.dataStore.edit { preferences ->
            preferences.remove(COMPANY_LATITUDE)
            preferences.remove(COMPANY_LONGITUDE)
            preferences.remove(COMPANY_ADDRESS)
            preferences.remove(GEOFENCE_RADIUS_METERS)
            preferences.remove(ENABLE_WEEKEND_REMINDER)
        }
    }
}