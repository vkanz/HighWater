package ru.systemt.highwater.ui.measurement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.systemt.highwater.data.ble.BleManager
import ru.systemt.highwater.domain.model.BleState
import ru.systemt.highwater.domain.repository.DevicePreferencesRepository
import ru.systemt.highwater.service.BleServiceManager

sealed interface MeasurementEvent {
    data object OpenDeviceSelection : MeasurementEvent
}

@HiltViewModel
class MeasurementViewModel @Inject constructor(
    private val bleManager: BleManager,
    private val devicePreferences: DevicePreferencesRepository,
    private val serviceManager: BleServiceManager,
) : ViewModel() {

    val bleState: StateFlow<BleState> = bleManager.state

    private val eventsChannel = Channel<MeasurementEvent>(Channel.BUFFERED)
    val events: Flow<MeasurementEvent> = eventsChannel.receiveAsFlow()

    private var started = false

    /**
     * Вызывается при входе на экран. Повторный вход (возврат с MainScreen)
     * не перезапускает поиск — состояние мгновенно берётся из [BleManager].
     */
    fun start() {
        if (started) return
        started = true
        serviceManager.start()

        viewModelScope.launch {
            val address = devicePreferences.savedDeviceAddress.first()
            if (address.isNullOrBlank()) {
                eventsChannel.send(MeasurementEvent.OpenDeviceSelection)
            } else {
                bleManager.ensureSearching(address)
            }
        }
    }

    fun retry() {
        serviceManager.start()
        viewModelScope.launch {
            val address = devicePreferences.savedDeviceAddress.first()
            if (address.isNullOrBlank()) {
                eventsChannel.send(MeasurementEvent.OpenDeviceSelection)
            } else {
                bleManager.connectToDevice(address)
            }
        }
    }

    fun selectAnotherDevice() {
        bleManager.reset()
        serviceManager.stop()
        viewModelScope.launch {
            eventsChannel.send(MeasurementEvent.OpenDeviceSelection)
        }
    }
}
