package ru.systemt.highwater.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.systemt.highwater.MainActivity
import ru.systemt.highwater.R
import ru.systemt.highwater.data.ble.BleConfig
import ru.systemt.highwater.data.ble.BleManager
import ru.systemt.highwater.domain.model.BlePhase
import ru.systemt.highwater.domain.model.BleState

/**
 * Foreground-сервис типа `connectedDevice`. Держит процесс живым, пока идёт
 * сканирование/поиск/подключение или установлено BLE-соединение, и показывает
 * постоянное уведомление о текущем состоянии.
 */
@AndroidEntryPoint
class BleForegroundService : Service() {

    @Inject
    lateinit var bleManager: BleManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var idleStopJob: Job? = null
    private lateinit var notificationManager: NotificationManager

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()

        ServiceCompat.startForeground(
            this,
            BleConfig.NOTIFICATION_ID,
            buildNotification(bleManager.state.value),
            foregroundServiceType(),
        )

        scope.launch {
            bleManager.state.collect { state ->
                notificationManager.notify(
                    BleConfig.NOTIFICATION_ID,
                    buildNotification(state),
                )

                if (state.isActive) {
                    idleStopJob?.cancel()
                    idleStopJob = null
                } else if (idleStopJob == null) {
                    idleStopJob = scope.launch {
                        delay(BleConfig.IDLE_STOP_DELAY_MS)
                        stopSelf()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            BleConfig.NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(state: BleState): Notification {
        val text = when (state.phase) {
            BlePhase.SCANNING -> getString(R.string.notification_status_scanning)
            BlePhase.SEARCHING -> getString(R.string.notification_status_searching)
            BlePhase.CONNECTING -> getString(R.string.notification_status_connecting)
            BlePhase.CONNECTED -> getString(
                R.string.notification_status_connected,
                state.connectedDevice?.name ?: state.connectedDevice?.address ?: "",
            )

            else -> getString(R.string.notification_status_idle)
        }

        return NotificationCompat.Builder(this, BleConfig.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_highwater)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setContentIntent(contentIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun foregroundServiceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        } else {
            0
        }
}
