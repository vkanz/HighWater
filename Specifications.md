# Project specification for Android app "HighWater"

## Tech Stack

Kotlin, Jetpack Compose, Coroutines/Flow, Hilt (DI), Jetpack Navigation Compose, Jetpack DataStore.

Architecture: Follow official "Guide to app architecture" (MVVM, Unidirectional Data Flow, Layered Architecture: UI -> Domain -> Data).

## Core Requirement

The app interacts with Bluetooth Low Energy (BLE) devices. 

The BLE connection and scanning must survive app backgrounding (e.g., when the user switches to another app). This requires a robust background execution strategy.

## 1. Android Specifics & Permissions (Crucial)

Handle runtime permissions correctly for Android 12+ (BLUETOOTH_SCAN, BLUETOOTH_CONNECT) and older versions (ACCESS_FINE_LOCATION).

To keep the connection alive in the background, implement a Foreground Service (type connectedDevice) with a persistent notification.

The BLE logic should be encapsulated in a BleManager (singleton or Hilt-provided) that is bound to this Foreground Service. 

The UI (ViewModels) should observe the BleManager via Kotlin StateFlow.

## 2. App Name & Navigation

App Name: HighWater.

Use Jetpack Navigation Compose.

Navigation Graph:
- MainScreen
- DeviceSelectionScreen
- MeasurementScreen

## 3. Screen Details

### 3.1. Main Screen

Two primary buttons: "Выбрать устройство" (Select Device) and "Начать измерение" (Start Measurement).

Logic for "Начать измерение":
- If a device MAC address is already saved in DataStore -> navigate to MeasurementScreen.
- If no device is saved -> navigate to DeviceSelectionScreen.

Logic for "Выбрать устройство": Always navigate to DeviceSelectionScreen.

### 3.2. Device Selection Screen

Displays a list of visible BLE devices.

Scanning (discovering) runs for 60 seconds. Make interval easily configurable (e.g., in a BleConfig object).

Show a linear progress bar during the 60s scan.

Scan Filter: Implement filtering by the first bytes of the advertising packet (use ScanFilter.Builder().setManufacturerData(...)). Make the Company ID and expected data bytes easily configurable (e.g., in a BleConfig object).

When a device is clicked:
- Save its MAC address permanently using Jetpack DataStore (Preferences).
- Stop scanning.
- Navigate to MeasurementScreen.

### 3.3. Measurement Screen

State 1: Searching. Searches for the saved MAC address. Show a progress bar. (Timeout: e.g., 15 seconds).

State 2: Connecting/Connected. If found, initiates connection. Upon success, display basic device info (Name, MAC, RSSI, Connection State). Note: UI for sending/receiving data will be added later, just provide a placeholder container for it.

State 3: Failed. If search times out or connection fails, show an error message and two buttons:
- "Сканировать" (Scan): Restarts the search/connection process with a progress bar.
- "Выбор другого устройства" (Select another device): Clears current state and navigates back to DeviceSelectionScreen.

Background behavior: If the user minimizes the app or navigates away, the Foreground Service must maintain the BLE connection. When the user returns to this screen, the UI must instantly reflect the current connection state from the BleManager.

Navigation: The system "Back" button or a custom back arrow should navigate to the MainScreen.

## 4. Implementation Instructions

- Start by setting up the AndroidManifest.xml with all necessary BLE, Location, and Foreground Service permissions.
- Create the BleManager class handling scanning, filtering, connecting, and state broadcasting via StateFlow.
- Create the BleForegroundService to host the BleManager.
- Implement the DataStore repository for saving the MAC address.
- Implement the ViewModels for each screen.
- Build the Compose UI and Navigation graph.
- Ensure all long-running operations use Coroutines and properly handle lifecycle/cancellation.