package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.RadarDriverApp

class FloatingBubbleService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = (application as? RadarDriverApp)?.runtimeController?.overlayManager
        when (intent?.action) {
            ACTION_SHOW_BUBBLE -> {
                if (PermissionHelper.hasOverlayPermission(this)) {
                    manager?.showOrUpdate()
                }
            }
            ACTION_HIDE_BUBBLE -> {
                manager?.hide()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        (application as? RadarDriverApp)?.runtimeController?.overlayManager?.hide()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
            }
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Radar Driver ativo")
            .setContentText("Balão flutuante pronto sobre Uber e 99")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_SHOW_BUBBLE = "com.example.action.SHOW_BUBBLE"
        const val ACTION_HIDE_BUBBLE = "com.example.action.HIDE_BUBBLE"
        private const val CHANNEL_ID = "radar_floating_bubble"
        private const val NOTIFICATION_ID = 4099
    }
}
