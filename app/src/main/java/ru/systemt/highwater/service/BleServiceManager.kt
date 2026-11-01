package ru.systemt.highwater.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Тонкая обёртка над запуском/остановкой [BleForegroundService],
 * чтобы ViewModel-и не зависели от деталей Android Service API.
 */
@Singleton
class BleServiceManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun start() {
        ContextCompat.startForegroundService(
            context,
            Intent(context, BleForegroundService::class.java),
        )
    }

    fun stop() {
        context.stopService(Intent(context, BleForegroundService::class.java))
    }
}
