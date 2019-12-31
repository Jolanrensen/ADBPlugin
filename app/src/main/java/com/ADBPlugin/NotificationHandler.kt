package com.ADBPlugin

import com.ADBPlugin.ui.EditActivity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService


class NotificationHandler(private var context: Context) {
    val channel = "Action"
    val builder = NotificationCompat.Builder(context, channel)

    init {
        if (Build.VERSION.SDK_INT >= 26) {
            val notificationManager = context.getSystemService<NotificationManager>()!!
            val name = context.getString(R.string.channel_name)
            val importance = NotificationManager.IMPORTANCE_LOW
            notificationManager.createNotificationChannel(
                NotificationChannel(channel, name, importance).apply {
                    description = context.getString(R.string.channel_description)
                    enableVibration(false)
                }
            )
        }
    }

    fun tempNotification(): Notification {
        val appIntent = Intent(context, EditActivity::class.java)
        val pendingAppIntent =
            PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setSmallIcon(R.drawable.icon)
            .setOngoing(true)
            .setVibrate(longArrayOf())
            .setContentTitle(context.getString(R.string.action_running))
            .setContentIntent(pendingAppIntent)

        return builder.build()
    }

}
