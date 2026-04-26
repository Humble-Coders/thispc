package com.humblesolutions.cutq.viewmodel

import android.Manifest
import android.app.Application
import android.content.IntentSender
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.humblesolutions.cutq.model.City
import com.humblesolutions.cutq.model.UserLocation
import com.humblesolutions.cutq.repository.AndroidLocationRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LocationSheet { NONE, SOFT_ASK, CITY_PICKER, RATIONALE }

data class LocationUiState(
    val sheet: LocationSheet = LocationSheet.NONE,
    val userLocation: UserLocation? = null,
    val displayName: String = "All Cities",
    val isLocating: Boolean = false,
    val hasPermission: Boolean = false,
)

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val locationRepo = AndroidLocationRepository(application)
    private val prefs = application.getSharedPreferences("cutq_location", 0)

    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    private val _gpsResolutionEvent = MutableSharedFlow<IntentSender>(extraBufferCapacity = 1)
    val gpsResolutionEvent: SharedFlow<IntentSender> = _gpsResolutionEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            val granted = hasLocationPermission()
            val saved   = locationRepo.getSavedLocation()
            when {
                saved != null -> _uiState.update {
                    it.copy(
                        userLocation  = saved,
                        displayName   = locationDisplayName(saved),
                        hasPermission = granted
                    )
                }
                granted -> {
                    _uiState.update { it.copy(hasPermission = true) }
                    checkSettingsAndFetch()
                }
                else -> {
                    val hasAskedBefore = prefs.getBoolean("location_asked", false)
                    _uiState.update {
                        it.copy(sheet = if (hasAskedBefore) LocationSheet.NONE else LocationSheet.SOFT_ASK)
                    }
                }
            }
        }
    }

    // ── Soft-ask sheet (Phase 1) ──────────────────────────────────────────
    fun onSoftAskUseMyLocationClicked() {
        markAsked()
        _uiState.update { it.copy(sheet = LocationSheet.NONE) }
        // Caller triggers the OS permission launcher after this.
    }

    fun onSoftAskEnterManuallyClicked() {
        markAsked()
        _uiState.update { it.copy(sheet = LocationSheet.CITY_PICKER) }
    }

    fun onSoftAskSkipClicked() {
        markAsked()
        _uiState.update { it.copy(sheet = LocationSheet.NONE) }
    }

    // ── Permission dialog callback ────────────────────────────────────────
    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasPermission = granted) }
        if (granted) checkSettingsAndFetch()
    }

    // ── Pill tap → open city picker ───────────────────────────────────────
    fun onLocationPillTap() {
        refreshPermissionState()
        _uiState.update { it.copy(sheet = LocationSheet.CITY_PICKER) }
    }

    // ── City picker actions ───────────────────────────────────────────────
    fun onPickerCitySelected(city: City) {
        viewModelScope.launch {
            val loc = city.toUserLocation()
            locationRepo.saveLocation(loc)
            _uiState.update {
                it.copy(
                    userLocation = loc,
                    displayName  = locationDisplayName(loc),
                    sheet        = LocationSheet.NONE
                )
            }
        }
    }

    fun onPickerUseCurrentLocationClicked() {
        refreshPermissionState()
        if (_uiState.value.hasPermission) {
            _uiState.update { it.copy(sheet = LocationSheet.NONE) }
            checkSettingsAndFetch()
        } else {
            _uiState.update { it.copy(sheet = LocationSheet.RATIONALE) }
        }
    }

    // ── Rationale sheet (Phase 3) ─────────────────────────────────────────
    fun onRationaleAllowClicked() {
        _uiState.update { it.copy(sheet = LocationSheet.NONE) }
        // Caller triggers OS permission launcher after this.
    }

    fun onRationaleDismissClicked() {
        _uiState.update { it.copy(sheet = LocationSheet.NONE) }
    }

    fun dismissSheet() {
        _uiState.update { it.copy(sheet = LocationSheet.NONE) }
    }

    // ── Internals ─────────────────────────────────────────────────────────
    private fun checkSettingsAndFetch() {
        viewModelScope.launch {
            val intentSender = locationRepo.checkLocationSettings()
            if (intentSender != null) {
                _gpsResolutionEvent.tryEmit(intentSender)
            } else {
                fetchLocation()
            }
        }
    }

    private fun fetchLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLocating = true) }
            val location = locationRepo.getCurrentLocation()
            if (location != null) {
                locationRepo.saveLocation(location)
                _uiState.update {
                    it.copy(
                        userLocation = location,
                        displayName  = locationDisplayName(location),
                        isLocating   = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLocating = false) }
            }
        }
    }

    private fun markAsked() {
        prefs.edit().putBoolean("location_asked", true).apply()
    }

    private fun refreshPermissionState() {
        _uiState.update { it.copy(hasPermission = hasLocationPermission()) }
    }

    private fun hasLocationPermission(): Boolean {
        val ctx = getApplication<Application>()
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun locationDisplayName(loc: UserLocation): String = when {
        loc.areaName.isNotEmpty() && loc.cityName.isNotEmpty() -> "${loc.areaName}, ${loc.cityName}"
        loc.cityName.isNotEmpty() -> loc.cityName
        loc.areaName.isNotEmpty() -> loc.areaName
        else -> "Current Location"
    }
}
