@file:Suppress("NullableBooleanElvis")

package com.ADBPlugin.receiver

import android.content.Intent
import android.util.Log
import com.ADBPlugin.Constants.LOG_TAG
import com.ADBPlugin.Constants.handleFireMessage
import com.ADBPlugin.NotificationHandler

/**
 * Same as FireReceiver, however, it can be fired from Tasker's service intent system
 */

class FireReceiverService : IntentServiceParallel(name = "ActionService") {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            57,
            NotificationHandler(applicationContext).tempNotification()
        )
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onHandleIntent(intent: Intent) {
        Log.d(LOG_TAG, "running from service")
        handleFireMessage(applicationContext, intent)
        // Don't need to tell Tasker I'm not done yet, since the thread is still running
    }
}