# HighWater

Android-приложение для работы с BLE-устройством измерения уровня воды.
Реализовано по `Specifications.md`.

## Стек

- Kotlin 2.0.21, Jetpack Compose (BOM 2024.12.01), Material 3
- Coroutines / Flow, `StateFlow` как единственный источник состояния BLE-сессии
- Hilt 2.52 (DI), Navigation Compose 2.8.5, DataStore Preferences 1.1.1
- AGP 8.7.3, Gradle 8.11.1, JDK 17, compileSdk/targetSdk 35, minSdk 28

Архитектура: MVVM + Unidirectional Data Flow, слои `ui` → `domain` → `data`.

## Структура

```
app/src/main/java/ru/systemt/highwater/
├── data/
│   ├── ble/BleConfig.kt        # таймауты, фильтр manufacturer data, константы уведомления
│   ├── ble/BleManager.kt       # сканирование, фильтрация, GATT, StateFlow<BleState>
│   └── preferences/            # DataStore + реализация репозитория MAC-адреса
├── domain/
│   ├── model/                  # BleDeviceInfo, BleState, BlePhase
│   └── repository/             # DevicePreferencesRepository
├── service/
│   ├── BleForegroundService.kt # FGS типа connectedDevice + постоянное уведомление
│   └── BleServiceManager.kt    # запуск/остановка сервиса
├── ui/
│   ├── main/                   # MainScreen + MainViewModel
│   ├── devices/                # DeviceSelectionScreen + ViewModel
│   ├── measurement/            # MeasurementScreen + ViewModel
│   ├── permissions/            # BlePermissionGate (Accompanist)
│   └── theme/                  # Material 3 тема
├── di/AppModule.kt             # Hilt-модули
├── MainActivity.kt
└── HighWaterApp.kt
```

## Ключевые решения

- **Foreground-сервис.** `BleForegroundService` (`foregroundServiceType="connectedDevice"`)
  запускается перед сканированием/поиском и показывает постоянное уведомление
  с текущей фазой. Сервис останавливается через 5 секунд после того, как BLE-сессия
  стала неактивной (`BleConfig.IDLE_STOP_DELAY_MS`).
- **Единый `BleManager`** — Hilt `@Singleton`; и UI, и сервис наблюдают один и тот же
  `StateFlow<BleState>`. Состояние переживает сворачивание приложения и смену экранов,
  поэтому при возврате на `MeasurementScreen` интерфейс сразу показывает актуальный
  статус подключения.
- **Сканирование списка** — 60 с (`BleConfig.SCAN_DURATION_MS`), линейный прогресс.
- **Поиск сохранённого устройства** — 15 с (`BleConfig.SEARCH_TIMEOUT_MS`), затем
  состояние `FAILED` с кнопками «Сканировать» и «Выбор другого устройства».
- **Подключение** — таймаут 15 с (`BleConfig.CONNECT_TIMEOUT_MS`). Если устройство
  не отвечает на `connectGatt`, сессия переходит в `FAILED` с сообщением о таймауте,
  а не зависает в состоянии «Подключение…».
- **Фильтр** — `ScanFilter.Builder().setManufacturerData(companyId, prefix, mask)`.
- **MAC-адрес** сохраняется в DataStore и переживает перезапуск приложения.
- **Разрешения.** Android 12+: `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`;
  Android 11 и ниже: `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION`
  (`maxSdkVersion=30`). На Android 13+ дополнительно запрашивается
  `POST_NOTIFICATIONS` для уведомления сервиса.

## Настройка под реальное устройство

В `data/ble/BleConfig.kt` заданы заглушки:

```kotlin
const val COMPANY_ID = 0x1234                  // ← Company ID производителя
val MANUFACTURER_DATA_PREFIX = byteArrayOf(0x48, 0x57)  // ← первые байты данных ("HW")
val MANUFACTURER_DATA_MASK = ByteArray(2) { 0xFF.toByte() }
```

Замените их на значения из документации устройства, иначе список сканирования
останется пустым.

## Сборка

```powershell
# JDK 17 указан в gradle.properties (org.gradle.java.home)
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleRelease
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Установка на подключённый телефон:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n ru.systemt.highwater/.MainActivity
```

Инструментальные тесты Compose (нужно устройство):

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

> Если после переименования ресурсов сборочная система показывает устаревшие
> resource id, пересоберите с `--no-build-cache` (в проекте уже встречалась
> такая проблема с кэшем на этом компьютере).

## Замечания по проверке

Проверено на реальном устройстве (Mi A1, Android 11, API 30):
- запуск приложения и главный экран;
- запрос разрешений (на API 30 — `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION`);
- сканирование списка устройств с прогрессом и foreground-уведомлением;
- выбор устройства, сохранение MAC в DataStore;
- поиск, подключение и экран «Подключено» (имя, MAC, RSSI, состояние);
- сохранение соединения при сворачивании приложения и мгновенное восстановление UI;
- остановка foreground-сервиса и удаление уведомления через 5 с после завершения сессии;
- таймаут подключения и экран ошибки с кнопками «Сканировать» / «Выбор другого устройства»;
- сценарий «Выбор другого устройства» со сбросом состояния.

Сборка:
- `:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:assembleDebugAndroidTest`,
  `:app:lintDebug`, `:app:assembleRelease` — успешно.
- Lint: 0 ошибок; остаются только информационные `GradleDependency`
  (доступны более новые версии библиотек).
- Unit-тесты: `BleConfigTest`, `BleStateTest` — 4 теста, 0 падений.
