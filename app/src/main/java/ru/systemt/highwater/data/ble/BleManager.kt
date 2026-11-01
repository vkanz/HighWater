package ru.systemt.highwater.data.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.systemt.highwater.domain.model.BleDeviceInfo
import ru.systemt.highwater.domain.model.BlePhase
import ru.systemt.highwater.domain.model.BleState

/**
 * Синглтон, инкапсулирующий всю работу с BLE: сканирование, фильтрацию,
 * подключение и публикацию состояния через [StateFlow].
 *
 * Экземпляр живёт в процессе приложения, поэтому foreground-сервис и UI
 * работают с одним и тем же состоянием.
 */
@Singleton
class BleManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(BluetoothManager::class.java)

    private val adapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private val _state = MutableStateFlow(BleState())
    val state: StateFlow<BleState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var scanMode = ScanMode.NONE
    private var targetAddress: String? = null
    private var scanProgressJob: Job? = null
    private var rssiJob: Job? = null
    private var connectTimeoutJob: Job? = null
    private var gatt: BluetoothGatt? = null
    private var connectedDeviceInfo: BleDeviceInfo? = null
    private var expectDisconnect = false

    /** Поколение соединения: позволяет игнорировать запоздавшие callback-и старого GATT. */
    private var generation = 0
    private val gattGenerations = ConcurrentHashMap<BluetoothGatt, Int>()

    private enum class ScanMode { NONE, DEVICE_LIST, SEARCH }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val scanFilter: ScanFilter = ScanFilter.Builder()
        .setManufacturerData(
            BleConfig.COMPANY_ID,
            BleConfig.MANUFACTURER_DATA_PREFIX,
            BleConfig.MANUFACTURER_DATA_MASK,
        )
        .build()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::handleScanResult)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "BLE scan failed: $errorCode")
            val mode = scanMode
            stopScanInternal()
            _state.update {
                it.copy(
                    phase = BlePhase.FAILED,
                    errorMessage = "Ошибка сканирования Bluetooth (код $errorCode)",
                    scanProgress = if (mode == ScanMode.DEVICE_LIST) 1f else it.scanProgress,
                    searchProgress = if (mode == ScanMode.SEARCH) 1f else it.searchProgress,
                )
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (!isCurrent(gatt)) {
                safeClose(gatt)
                gattGenerations.remove(gatt)
                return
            }

            when {
                status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED ->
                    handleGattConnected(gatt)

                newState == BluetoothProfile.STATE_DISCONNECTED ||
                    status != BluetoothGatt.GATT_SUCCESS ->
                    handleGattDisconnected(gatt, status)

                else -> Unit
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d(TAG, "Services discovered, status=$status")
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            _state.update { state ->
                if (state.phase != BlePhase.CONNECTED) {
                    state
                } else {
                    state.copy(
                        rssi = rssi,
                        connectedDevice = state.connectedDevice?.copy(rssi = rssi),
                    )
                }
            }
        }
    }

    /** Запускает 60-секундное сканирование для выбора устройства. */
    fun startDeviceScan() {
        if (!ensureBluetoothReady()) return
        stopScanInternal()
        closeGatt()

        targetAddress = null
        scanMode = ScanMode.DEVICE_LIST
        _state.value = BleState(phase = BlePhase.SCANNING)
        startScanInternal(BleConfig.SCAN_DURATION_MS)
    }

    /** Останавливает сканирование списка устройств, сохраняя найденный список. */
    fun stopDeviceScan() {
        if (scanMode != ScanMode.DEVICE_LIST) return
        stopScanInternal()
        _state.update { it.copy(phase = BlePhase.IDLE, scanProgress = 1f) }
    }

    /**
     * Ищет сохранённое устройство по MAC-адресу (таймаут 15 секунд)
     * и подключается к нему.
     */
    fun connectToDevice(address: String) {
        if (!ensureBluetoothReady()) return
        if (!hasConnectPermission()) {
            fail("Нет разрешения на подключение к Bluetooth-устройствам")
            return
        }

        stopScanInternal()
        closeGatt()

        targetAddress = address
        scanMode = ScanMode.SEARCH
        _state.value = BleState(phase = BlePhase.SEARCHING, targetAddress = address)
        startScanInternal(BleConfig.SEARCH_TIMEOUT_MS)
    }

    /** Подключается, только если устройство ещё не подключено и не ищется. */
    fun ensureSearching(address: String) {
        val current = _state.value
        if (current.phase == BlePhase.CONNECTED &&
            current.connectedDevice?.address.equals(address, ignoreCase = true)
        ) {
            return
        }
        if ((current.phase == BlePhase.SEARCHING || current.phase == BlePhase.CONNECTING) &&
            current.targetAddress.equals(address, ignoreCase = true)
        ) {
            return
        }
        connectToDevice(address)
    }

    /** Полностью сбрасывает сессию (используется при выборе другого устройства). */
    fun reset() {
        stopScanInternal()
        closeGatt()
        targetAddress = null
        _state.value = BleState()
    }

    private fun ensureBluetoothReady(): Boolean {
        if (!hasScanPermission()) {
            fail("Нет разрешения на поиск Bluetooth-устройств")
            return false
        }
        if (!hasConnectPermission()) {
            fail("Нет разрешения на доступ к Bluetooth-устройствам")
            return false
        }

        val bluetoothAdapter = adapter
        if (bluetoothAdapter == null) {
            fail("Bluetooth не поддерживается на этом устройстве")
            return false
        }

        val enabled = try {
            bluetoothAdapter.isEnabled
        } catch (_: SecurityException) {
            false
        }
        if (!enabled) {
            fail("Bluetooth выключен. Включите его и повторите попытку")
            return false
        }
        return true
    }

    @SuppressLint("MissingPermission")
    private fun startScanInternal(durationMs: Long) {
        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null) {
            fail("Bluetooth-сканер недоступен")
            return
        }

        val startedAt = SystemClock.elapsedRealtime()
        try {
            //scanner.startScan(listOf(scanFilter), scanSettings, scanCallback)
            scanner.startScan(null, scanSettings, scanCallback)
        } catch (_: SecurityException) {
            fail("Нет разрешения на сканирование Bluetooth-устройств")
            return
        }

        val modeAtStart = scanMode
        scanProgressJob?.cancel()
        scanProgressJob = scope.launch {
            while (isActive) {
                val elapsed = SystemClock.elapsedRealtime() - startedAt
                val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                _state.update { state ->
                    when (modeAtStart) {
                        ScanMode.DEVICE_LIST ->
                            if (state.phase == BlePhase.SCANNING) state.copy(scanProgress = progress) else state

                        ScanMode.SEARCH ->
                            if (state.phase == BlePhase.SEARCHING) state.copy(searchProgress = progress) else state

                        ScanMode.NONE -> state
                    }
                }

                if (elapsed >= durationMs) {
                    handleScanTimeout(modeAtStart)
                    break
                }
                delay(BleConfig.PROGRESS_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun handleScanTimeout(mode: ScanMode) {
        if (scanMode != mode) return
        stopScanInternal()
        when (mode) {
            ScanMode.DEVICE_LIST -> _state.update {
                it.copy(phase = BlePhase.IDLE, scanProgress = 1f)
            }

            ScanMode.SEARCH -> _state.update {
                it.copy(
                    phase = BlePhase.FAILED,
                    searchProgress = 1f,
                    errorMessage = "Устройство не найдено за ${BleConfig.SEARCH_TIMEOUT_MS / 1000} с",
                )
            }

            ScanMode.NONE -> Unit
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScanInternal() {
        scanProgressJob?.cancel()
        scanProgressJob = null

        if (scanMode != ScanMode.NONE) {
            try {
                adapter?.bluetoothLeScanner?.stopScan(scanCallback)
            } catch (_: SecurityException) {
                // Bluetooth уже выключен или разрешение отозвано.
            }
        }
        scanMode = ScanMode.NONE
    }

    @SuppressLint("MissingPermission")
    private fun handleScanResult(result: ScanResult) {
        val device = result.device ?: return
        val address = try {
            device.address
        } catch (_: SecurityException) {
            return
        }

        val info = BleDeviceInfo(
            name = safeDeviceName(device),
            address = address,
            rssi = result.rssi,
        )

        when (scanMode) {
            ScanMode.DEVICE_LIST -> _state.update { state ->
                if (state.phase != BlePhase.SCANNING) {
                    state
                } else {
                    val devices = (state.devices.filterNot { it.address == address } + info)
                        .sortedByDescending { it.rssi }
                    state.copy(devices = devices)
                }
            }

            ScanMode.SEARCH -> {
                if (!address.equals(targetAddress, ignoreCase = true)) return
                stopScanInternal()
                connectedDeviceInfo = info
                _state.update {
                    it.copy(
                        phase = BlePhase.CONNECTING,
                        targetAddress = address,
                        searchProgress = 1f,
                        connectedDevice = null,
                        rssi = null,
                        errorMessage = null,
                    )
                }
                connectGatt(device)
            }

            ScanMode.NONE -> Unit
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectGatt(device: BluetoothDevice) {
        if (!hasConnectPermission()) {
            fail("Нет разрешения на подключение к Bluetooth-устройствам")
            return
        }

        val gattInstance = try {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (_: SecurityException) {
            fail("Нет разрешения на подключение к Bluetooth-устройствам")
            return
        }

        if (gattInstance == null) {
            fail("Не удалось начать подключение к устройству")
            return
        }

        gattGenerations[gattInstance] = generation
        gatt = gattInstance
        startConnectTimeout(gattInstance)
    }

    /** Если устройство не отвечает на connectGatt, переводим UI в состояние ошибки. */
    private fun startConnectTimeout(gattInstance: BluetoothGatt) {
        connectTimeoutJob?.cancel()
        connectTimeoutJob = scope.launch {
            delay(BleConfig.CONNECT_TIMEOUT_MS)
            if (gatt === gattInstance && _state.value.phase == BlePhase.CONNECTING) {
                fail(
                    "Не удалось подключиться к устройству: таймаут " +
                        "${BleConfig.CONNECT_TIMEOUT_MS / 1000} с",
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleGattConnected(gatt: BluetoothGatt) {
        connectTimeoutJob?.cancel()
        connectTimeoutJob = null
        this.gatt = gatt
        expectDisconnect = false

        val info = connectedDeviceInfo
            ?: BleDeviceInfo(
                name = safeDeviceName(gatt.device),
                address = gatt.device.address,
                rssi = 0,
            )

        _state.update {
            it.copy(
                phase = BlePhase.CONNECTED,
                targetAddress = info.address,
                connectedDevice = info,
                rssi = info.rssi,
                searchProgress = 1f,
                errorMessage = null,
            )
        }

        try {
            gatt.discoverServices()
        } catch (_: SecurityException) {
            // Разрешение могло быть отозвано сразу после подключения.
        }
        startRssiUpdates()
    }

    private fun handleGattDisconnected(gatt: BluetoothGatt, status: Int) {
        connectTimeoutJob?.cancel()
        connectTimeoutJob = null
        safeClose(gatt)
        if (this.gatt === gatt) this.gatt = null
        gattGenerations.remove(gatt)
        rssiJob?.cancel()
        rssiJob = null

        if (expectDisconnect) {
            expectDisconnect = false
            return
        }

        val message = if (status == BluetoothGatt.GATT_SUCCESS) {
            "Соединение с устройством потеряно"
        } else {
            "Не удалось подключиться к устройству (код $status)"
        }

        _state.update {
            it.copy(
                phase = BlePhase.FAILED,
                errorMessage = message,
                connectedDevice = null,
                rssi = null,
                searchProgress = 1f,
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRssiUpdates() {
        rssiJob?.cancel()
        rssiJob = scope.launch {
            while (isActive) {
                delay(BleConfig.RSSI_UPDATE_INTERVAL_MS)
                val currentGatt = gatt ?: break
                if (gattGenerations[currentGatt] != generation) break
                if (_state.value.phase != BlePhase.CONNECTED) break
                try {
                    currentGatt.readRemoteRssi()
                } catch (_: SecurityException) {
                    break
                }
            }
        }
    }

    private fun closeGatt() {
        expectDisconnect = true
        rssiJob?.cancel()
        rssiJob = null
        connectTimeoutJob?.cancel()
        connectTimeoutJob = null

        val current = gatt
        gatt = null
        connectedDeviceInfo = null
        generation++

        if (current != null) {
            safeClose(current)
        }
    }

    @SuppressLint("MissingPermission")
    private fun safeClose(gatt: BluetoothGatt) {
        try {
            gatt.disconnect()
        } catch (_: SecurityException) {
            // Игнорируем: соединение уже недоступно.
        }
        try {
            gatt.close()
        } catch (_: SecurityException) {
            // close() не требует разрешений, но на всякий случай.
        }
    }

    private fun isCurrent(gatt: BluetoothGatt): Boolean {
        val gattGeneration = gattGenerations[gatt] ?: return true
        return gattGeneration == generation
    }

    private fun fail(message: String) {
        val mode = scanMode
        stopScanInternal()
        closeGatt()
        _state.update {
            it.copy(
                phase = BlePhase.FAILED,
                errorMessage = message,
                scanProgress = if (mode == ScanMode.DEVICE_LIST) 1f else it.scanProgress,
                searchProgress = if (mode == ScanMode.SEARCH) 1f else it.searchProgress,
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun safeDeviceName(device: BluetoothDevice?): String? {
        device ?: return null
        if (!hasConnectPermission()) return null
        return try {
            device.name?.takeIf { it.isNotBlank() }
        } catch (_: SecurityException) {
            null
        }
    }

    private fun hasScanPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        }

    private fun hasConnectPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val TAG = "BleManager"
    }
}
