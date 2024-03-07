package com.example.screenrecorderexample.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Intent
import android.os.Build
import android.os.HandlerThread
import android.os.IBinder
import android.os.Process
import androidx.core.app.NotificationCompat


class MediaProjectionService: Service() {
    val NOTIFICATION_ID = 0x01
    private var mNotificationChannelDefault: NotificationChannel? = null
    private val IN_ROUTE_MONITORING_SERVICE_NOTIFICATION_CHANNEL_ID = "screen_record_service_channel_0"
    private val screenRecordServiceNotificationChannelName = "Screen Record Notification"
    private val defaultNotificationImportance = NotificationManager.IMPORTANCE_LOW
    private var mNotificationManager: NotificationManager? = null
    private var mBuilderDefault: NotificationCompat.Builder? = null
    override fun onBind(p0: Intent?): IBinder? {
        return null;
    }

    override fun onCreate() {
        super.onCreate()

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

        startForeground(NOTIFICATION_ID, notification)
        val thread = HandlerThread(
            "ServiceStartArguments",
            Process.THREAD_PRIORITY_BACKGROUND
        )
        thread.start()

//        val mServiceLooper = thread.looper
//        mServiceHandler = ServiceHandler(mServiceLooper)
    }


    private fun getNotificationManager(): NotificationManager? {
        if (mNotificationManager == null) {
            mNotificationManager =
                this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        }
        return mNotificationManager
    }
}