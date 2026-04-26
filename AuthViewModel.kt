package com.humblesolutions.cutq.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.humblesolutions.cutq.repository.AndroidAuthRepository
import com.humblesolutions.cutq.usecase.SendOtpUseCase
import com.humblesolutions.cutq.usecase.VerifyOtpUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val phone: String = "",
    val otp: String = "",
    val otpResendSeconds: Int = 60,
    val canResendOtp: Boolean = false
)

sealed class AuthEvent {
    object NavigateToPhoneInput : AuthEvent()
    data class NavigateToOtp(val phone: String) : AuthEvent()
    object NavigateToHome : AuthEvent()
    object NavigateToProfileSetup : AuthEvent()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepo = AndroidAuthRepository()
    private val sendOtpUseCase = SendOtpUseCase(authRepo)
    private val verifyOtpUseCase = VerifyOtpUseCase(authRepo)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            authRepo.autoVerifiedEvent.collect {
                _uiState.update { it.copy(isLoading = false) }
                val complete = try { authRepo.checkProfileComplete() } catch (_: Exception) { false }
                _events.send(if (complete) AuthEvent.NavigateToHome else AuthEvent.NavigateToProfileSetup)
            }
        }
    }

    fun attachActivity(activity: Activity) { authRepo.attachActivity(activity) }

    fun onPhoneChange(value: String) =
        _uiState.update { it.copy(phone = value.filter(Char::isDigit).take(10), error = null) }

    fun onOtpChange(value: String) =
        _uiState.update { it.copy(otp = value.filter(Char::isDigit).take(6), error = null) }

    fun clearError() = _uiState.update { it.copy(error = null) }

    fun checkAuthState() {
        viewModelScope.launch {
            try {
                val user = authRepo.getCurrentUser()
                if (user != null) {
                    val complete = authRepo.checkProfileComplete()
                    _events.send(if (complete) AuthEvent.NavigateToHome else AuthEvent.NavigateToProfileSetup)
                } else {
                    _events.send(AuthEvent.NavigateToPhoneInput)
                }
            } catch (e: Exception) {
                _events.send(AuthEvent.NavigateToPhoneInput)
            }
        }
    }

    fun sendOtp() = launchAuth {
        sendOtpUseCase.execute(_uiState.value.phone)
        if (authRepo.wasAutoVerified) {
            val complete = authRepo.checkProfileComplete()
            _events.send(if (complete) AuthEvent.NavigateToHome else AuthEvent.NavigateToProfileSetup)
        } else {
            _events.send(AuthEvent.NavigateToOtp(_uiState.value.phone))
        }
    }

    fun verifyOtp(phone: String) = launchAuth {
        verifyOtpUseCase.execute(phone, _uiState.value.otp)
        val complete = authRepo.checkProfileComplete()
        _events.send(if (complete) AuthEvent.NavigateToHome else AuthEvent.NavigateToProfileSetup)
    }

    fun resendOtp() = launchAuth {
        sendOtpUseCase.execute(_uiState.value.phone)
        _uiState.update { it.copy(otp = "") }
        startResendCountdown()
    }

    fun startResendCountdown() {
        _uiState.update { it.copy(otpResendSeconds = 60, canResendOtp = false) }
        viewModelScope.launch {
            for (i in 60 downTo 0) {
                _uiState.update { it.copy(otpResendSeconds = i) }
                if (i == 0) { _uiState.update { it.copy(canResendOtp = true) }; break }
                delay(1000)
            }
        }
    }

    private fun launchAuth(block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                block()
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Something went wrong.") }
            }
        }
    }
}
