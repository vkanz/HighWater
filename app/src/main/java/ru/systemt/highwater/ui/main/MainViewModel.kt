package ru.systemt.highwater.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.systemt.highwater.domain.repository.DevicePreferencesRepository

data class MainUiState(
    val isLoading: Boolean = true,
    val savedDeviceAddress: String? = null,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    repository: DevicePreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = repository.savedDeviceAddress
        .map { address ->
            MainUiState(
                isLoading = false,
                savedDeviceAddress = address,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MainUiState(),
        )
}
