package ru.systemt.highwater.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.systemt.highwater.data.ble.BleManager
import ru.systemt.highwater.domain.model.BleDeviceInfo
import ru.systemt.highwater.domain.model.BleState
import ru.systemt.highwater.domain.repository.DevicePreferencesRepository
import ru.systemt.highwater.service.BleServiceManager

sealed interface DeviceSelectionEvent {
    data object DeviceSelected : DeviceSelectionEvent
}

@HiltViewModel
class DeviceSelectionViewModel @Inject constructor(
    private val bleManager: BleManager,
    private val devicePreferences: DevicePreferencesRepository,
    private val serviceManager: BleServiceManager,
) : ViewModel() {

    val bleState: StateFlow<BleState> = bleManager.state

    private val eventsChannel = Channel<DeviceSelectionEvent>(Channel.BUFFERED)
    val events: Flow<DeviceSelectionEvent> = eventsChannel.receiveAsFlow()

    fun startScan() {
        serviceManager.start()
        bleManager.startDeviceScan()
    }

    fun stopScan() {
        bleManager.stopDeviceScan()
    }

    fun selectDevice(device: BleDeviceInfo) {
        viewModelScope.launch {
            bleManager.stopDeviceScan()
            devicePreferences.saveDeviceAddress(device.address)
            eventsChannel.send(DeviceSelectionEvent.DeviceSelected)
        }
    }
}
