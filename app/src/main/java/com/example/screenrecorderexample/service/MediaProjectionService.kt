package com.example.screenrecorderexample.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.HandlerThread
import android.os.IBinder
import android.os.Process
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.screenrecorderexample.util.Constants
import com.example.screenrecorderexample.util.Constants.Companion.IN_ROUTE_MONITORING_SERVICE_NOTIFICATION_CHANNEL_ID
import com.example.screenrecorderexample.util.Constants.Companion.NOTIFICATION_ID


class MediaProjectionService: Service() {
    private var mNotificationChannelDefault: NotificationChannel? = null
    private val screenRecordServiceNotificationChannelName = "Screen Record Notification"
    private val defaultNotificationImportance = NotificationManager.IMPORTANCE_LOW
    private var mNotificationManager: NotificationManager? = null
    private var mBuilderDefault: NotificationCompat.Builder? = null
    private val tag = "MediaProjectionService"
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action.equals(Constants.START_FOREGROUND_ACTION)) {
            startService()
            sendBroadcast(Intent("com.example.YourServiceReady"))
        } else if (intent?.action.equals(Constants.STOP_FOREGROUND_ACTION)) {
            stopService(startId)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startService() {
        Log.d(tag, "Start foreground - enter")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationChannelDefault = NotificationChannel(
                IN_ROUTE_MONITORING_SERVICE_NOTIFICATION_CHANNEL_ID,
                screenRecordServiceNotificationChannelName,
                defaultNotificationImportance
            )
            mNotificationChannelDefault?.let {
                getNotificationManager()?.createNotificationChannel(it)
            }
        }
        mBuilderDefault = NotificationCompat.Builder(
            this,
            IN_ROUTE_MONITORING_SERVICE_NOTIFICATION_CHANNEL_ID
        )
        mBuilderDefault?.setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notification = mBuilderDefault!!
            .setContentTitle("Screen recorder")
            .setContentText("Your screen is being recorded and saved to your phone.")
            .setTicker("Tickertext")
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        val thread = HandlerThread(
            "ServiceStartArguments",
            Process.THREAD_PRIORITY_BACKGROUND
        )
        thread.start()
        Log.d(tag, "Start foreground - exit")
    }

    private fun stopService(startId: Int) {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelfResult(startId);
        stopSelf()
    }

    private fun getNotificationManager(): NotificationManager? {
        if (mNotificationManager == null) {
            mNotificationManager =
                this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        }
        return mNotificationManager
    }
}