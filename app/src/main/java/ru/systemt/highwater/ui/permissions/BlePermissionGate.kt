package ru.systemt.highwater.ui.permissions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import ru.systemt.highwater.R

/** Разрешения, без которых BLE-сканирование и подключение невозможны. */
fun requiredBlePermissions(): List<String> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(Manifest.permission.BLUETOOTH_SCAN)
        add(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
}

/** Необязательные разрешения (уведомление foreground-сервиса). */
fun optionalBlePermissions(): List<String> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}

/**
 * Показывает запрос runtime-разрешений, пока не выданы обязательные BLE-разрешения,
 * и отдаёт управление содержимому экрана после их получения.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BlePermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val requiredPermissions = remember { requiredBlePermissions() }
    val optionalPermissions = remember { optionalBlePermissions() }

    val requiredState = rememberMultiplePermissionsState(requiredPermissions)
    val optionalState = rememberMultiplePermissionsState(optionalPermissions)

    var resumeTick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        resumeTick++
    }

    // Читаем statuses и resumeTick, чтобы Compose перерисовывался при изменении
    // разрешений и при возврате из системных настроек.
    val statuses = requiredState.permissions.associate { it.permission to it.status }
    val granted = remember(resumeTick, statuses) {
        requiredPermissions.all { permission ->
            statuses[permission] == PermissionStatus.Granted
        }
    }

    var requiredRequested by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!granted && !requiredRequested) {
            requiredRequested = true
            requiredState.launchMultiplePermissionRequest()
        }
    }

    if (granted) {
        // Уведомление foreground-сервиса необязательно для работы BLE,
        // поэтому запрашиваем его отдельно и не блокируем экран.
        var optionalRequested by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(optionalPermissions) {
            val missing = optionalPermissions.any { permission ->
                optionalState.permissions.firstOrNull { it.permission == permission }
                    ?.status != PermissionStatus.Granted
            }
            if (missing && !optionalRequested) {
                optionalRequested = true
                optionalState.launchMultiplePermissionRequest()
            }
        }
        content()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.permission_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.permission_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { requiredState.launchMultiplePermissionRequest() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.permission_grant))
        }

        val permanentlyDenied = requiredRequested && requiredState.permissions.any {
            !it.status.isGranted && !it.status.shouldShowRationale
        }
        if (permanentlyDenied) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.permission_open_settings))
            }
        }
    }
}
