/*
 * Copyright 2013 two forty four a.m. LLC <http://www.twofortyfouram.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 * This file was used and modified by Jolan Rensen to create com.ADBPlugin.
 */

package com.ADBPlugin.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ADBPlugin.Constants
import com.ADBPlugin.Constants.handleFireMessage
import com.ADBPlugin.TaskerPlugin
import com.ADBPlugin.ui.EditActivity
import kotlin.concurrent.thread

/**
 * This is the "fire" BroadcastReceiver for a Locale Plug-in setting.
 *
 * @see com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING
 *
 * @see com.twofortyfouram.locale.Intent.EXTRA_BUNDLE
 */
class FireReceiver : BroadcastReceiver() {

    /**
     * @param context {@inheritDoc}.
     * @param intent  the incoming [com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING] Intent. This
     * should contain the [com.twofortyfouram.locale.Intent.EXTRA_BUNDLE] that was saved by
     * [EditActivity] and later broadcast by Locale.
     */
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(Constants.LOG_TAG, "running from broadcastreceiver")
        thread(start = true) {
            handleFireMessage(context, intent)
        }

        // Tell Tasker I'm not done yet, since the thread is still running
        if (isOrderedBroadcast) {
            resultCode = TaskerPlugin.Setting.RESULT_CODE_PENDING
        }
    }
}