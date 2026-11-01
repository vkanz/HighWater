package ru.systemt.highwater.ui.measurement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.systemt.highwater.R
import ru.systemt.highwater.domain.model.BlePhase
import ru.systemt.highwater.domain.model.BleState
import ru.systemt.highwater.ui.permissions.BlePermissionGate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementScreen(
    onBack: () -> Unit,
    onOpenDeviceSelection: () -> Unit,
    viewModel: MeasurementViewModel = hiltViewModel(),
) {
    val state by viewModel.bleState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                MeasurementEvent.OpenDeviceSelection -> onOpenDeviceSelection()
            }
        }
    }

    BlePermissionGate {
        LaunchedEffect(Unit) {
            viewModel.start()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.measurement_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
            ) {
                when (state.phase) {
                    BlePhase.SEARCHING -> SearchingContent(
                        address = state.targetAddress,
                        progress = state.searchProgress,
                    )

                    BlePhase.CONNECTING -> ConnectingContent()

                    BlePhase.CONNECTED -> ConnectedContent(state = state)

                    BlePhase.FAILED -> FailedContent(
                        errorMessage = state.errorMessage,
                        onRetry = viewModel::retry,
                        onSelectAnother = viewModel::selectAnotherDevice,
                    )

                    BlePhase.IDLE, BlePhase.SCANNING -> LoadingContent()
                }
            }
        }
    }
}

@Composable
private fun SearchingContent(address: String?, progress: Float) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.measurement_searching, address.orEmpty()),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ConnectingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.measurement_connecting),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ConnectedContent(state: BleState) {
    val device = state.connectedDevice
    val rssi = state.rssi ?: device?.rssi ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.measurement_state_connected),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InfoRow(
                    label = stringResource(R.string.measurement_info_name),
                    value = device?.name ?: stringResource(R.string.device_unnamed),
                )
                InfoRow(
                    label = stringResource(R.string.measurement_info_address),
                    value = device?.address ?: "—",
                )
                InfoRow(
                    label = stringResource(R.string.measurement_info_rssi),
                    value = stringResource(R.string.device_rssi, rssi),
                )
                InfoRow(
                    label = stringResource(R.string.measurement_info_state),
                    value = stringResource(R.string.measurement_state_connected),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.measurement_placeholder_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.measurement_placeholder_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FailedContent(
    errorMessage: String?,
    onRetry: () -> Unit,
    onSelectAnother: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.measurement_failed_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = errorMessage?.let {
                stringResource(R.string.measurement_error, it)
            } ?: stringResource(R.string.measurement_error, ""),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.retry_scan))
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onSelectAnother,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.select_another_device))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1.4f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
        )
    }
}
