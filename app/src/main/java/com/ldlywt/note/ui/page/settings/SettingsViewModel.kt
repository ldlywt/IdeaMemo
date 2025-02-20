package com.ldlywt.note.ui.page.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ldlywt.note.biometric.AppBioMetricManager
import com.ldlywt.note.biometric.BiometricAuthListener

import com.ldlywt.note.ui.page.main.MainActivity
import com.ldlywt.note.utils.SharedPreferencesUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val appBioMetricManager: AppBioMetricManager,
) : AndroidViewModel(application) {

    private val _biometricAuthState = MutableStateFlow(false)
    val biometricAuthState: StateFlow<Boolean> = _biometricAuthState

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _biometricAuthState.value = SharedPreferencesUtils.useSafe.first()
        }
    }

    fun showBiometricPrompt(activity: MainActivity) {
        appBioMetricManager.initBiometricPrompt(
            activity = activity,
            listener = object : BiometricAuthListener {
                override fun onBiometricAuthSuccess() {
                    viewModelScope.launch {
                        SharedPreferencesUtils.updateUseSafe(!_biometricAuthState.value)
                        _biometricAuthState.value = !_biometricAuthState.value
                    }

                }

                override fun onUserCancelled() {
                }

                override fun onErrorOccurred() {
                }
            }
        )
    }
}
