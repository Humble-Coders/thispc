package com.humblesolutions.cutq.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.humblesolutions.cutq.repository.AndroidAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class ProfileSetupUiState(
    val name: String = "",
    val gender: String = "",
    val dobDay: Int = 1,
    val dobMonth: Int = 1,
    val dobYear: Int = Calendar.getInstance().get(Calendar.YEAR) - 25,
    val dobEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class ProfileSetupViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AndroidAuthRepository()

    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    val years: List<Int> = (Calendar.getInstance().get(Calendar.YEAR) - 13 downTo 1930).toList()

    val days: List<Int> get() {
        val m = _uiState.value.dobMonth
        val y = _uiState.value.dobYear
        val max = when (m) {
            2    -> if (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        return (1..max).toList()
    }

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, error = null) }
    fun onGenderSelect(g: String) = _uiState.update { it.copy(gender = g, error = null) }
    fun onDobDayChange(d: Int) = _uiState.update { it.copy(dobDay = d.coerceIn(1, days.size)) }
    fun onDobMonthChange(m: Int) = _uiState.update { it.copy(dobMonth = m, dobDay = _uiState.value.dobDay.coerceIn(1, days.size)) }
    fun onDobYearChange(y: Int) = _uiState.update { it.copy(dobYear = y, dobDay = _uiState.value.dobDay.coerceIn(1, days.size)) }
    fun toggleDob() = _uiState.update { it.copy(dobEnabled = !it.dobEnabled) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    fun save() {
        val s = _uiState.value
        if (s.name.isBlank()) { _uiState.update { it.copy(error = "Please enter your name") }; return }
        if (s.gender.isBlank()) { _uiState.update { it.copy(error = "Please select your gender") }; return }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val dob = if (s.dobEnabled) {
                    "${s.dobDay.toString().padStart(2, '0')}-${s.dobMonth.toString().padStart(2, '0')}-${s.dobYear}"
                } else null
                repo.saveProfile(s.name.trim(), s.gender, dob)
                _uiState.update { it.copy(isLoading = false, success = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to save. Please try again.") }
            }
        }
    }
}
