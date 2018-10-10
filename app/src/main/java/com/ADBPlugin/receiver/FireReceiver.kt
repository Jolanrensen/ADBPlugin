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
import android.os.Bundle
import android.util.Log

import com.ADBPlugin.Constants
import com.ADBPlugin.Constants.jsonObjectOf
import com.ADBPlugin.SendSingleCommand
import com.ADBPlugin.TaskerPlugin
import com.ADBPlugin.bundle.BundleScrubber
import com.ADBPlugin.bundle.PluginBundleManager
import com.ADBPlugin.ui.EditActivity
import org.json.JSONArray
import org.json.JSONObject

import java.io.IOException
import java.util.ArrayList
import java.util.Locale

/**
 * This is the "fire" BroadcastReceiver for a Locale Plug-in setting.
 *
 * @see com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING
 *
 * @see com.twofortyfouram.locale.Intent.EXTRA_BUNDLE
 */
class FireReceiver : BroadcastReceiver() {

    var message: String? = null

    /**
     * @param context {@inheritDoc}.
     * @param intent  the incoming [com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING] Intent. This
     * should contain the [com.twofortyfouram.locale.Intent.EXTRA_BUNDLE] that was saved by
     * [EditActivity] and later broadcast by Locale.
     */
    override fun onReceive(context: Context, intent: Intent) {

        /*
         * Always be strict on input parameters! A malicious third-party app could send a malformed Intent.
         */

        if (com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING != intent.action) {
            if (Constants.IS_LOGGABLE) {
                Log.e(Constants.LOG_TAG,
                        String.format(Locale.US, "Received unexpected Intent action %s", intent.action)) //$NON-NLS-1$
            }
            return
        }

        BundleScrubber.scrub(intent)

        val bundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE)
        BundleScrubber.scrub(bundle)

        if (PluginBundleManager.isBundleValid(bundle)) {
            message = bundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE)

            val values =
                    if (message!!.contains('§')) { //backwards compatibility
                        val split = message!!.split("§".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        jsonObjectOf(
                                "ip"        to split[0],
                                "port"      to split[1].toInt(),
                                "command"   to split[2],
                                "ctrl_c"    to false
                        )
                    } else JSONObject(message)


            Thread(Runnable {
                try {
                    //Run the program with all the given variables
                    val sendSingleCommand = SendSingleCommand(context, values["ip"] as String, values["port"] as Int, values["command"] as String, values["ctrl_c"] as Boolean)

                    //Log the result and signal Tasker
                    Log.d(Constants.LOG_TAG, "Executed single command")

                    //Gathering responses
                    val responses = sendSingleCommand.splitResponses
                    val responseBundle = Bundle()
                    responseBundle.putStringArrayList("%output", responses)
                    TaskerPlugin.addVariableBundle(getResultExtras(true), responseBundle)

                    //Tell Takser I'm done
                    TaskerPlugin.Setting.signalFinish(context, intent, TaskerPlugin.Setting.RESULT_CODE_OK, responseBundle)
                } catch (e: Exception) { // if couldn't read/write key files
                    Log.e(Constants.LOG_TAG, "", e)
                    displayError(e, context, intent)
                }
            }).start()

            //Tell Tasker I'm not done yet, since the thread is still running
            if (isOrderedBroadcast) {
                resultCode = TaskerPlugin.Setting.RESULT_CODE_PENDING
            }
        }
    }

    /**
     * Simple method to inform Tasker and the system of the error that has occurred.
     *
     * @param e
     * @param context
     * @param intent
     */
    fun displayError(e: Exception, context: Context, intent: Intent) {
        Log.e(Constants.LOG_TAG, e.message)
        val errors = Bundle()
        errors.putString(TaskerPlugin.Setting.VARNAME_ERROR_MESSAGE, e.message)
        TaskerPlugin.Setting.signalFinish(context, intent, TaskerPlugin.Setting.RESULT_CODE_FAILED_PLUGIN_FIRST, errors)
    }

    fun JSONArray.toArrayList(): ArrayList<Any> {
        val array = arrayListOf<Any>()
        forEach { array.add(it) }
        return array
    }

    fun JSONArray.forEach(run: (Any) -> Unit) {
        for (i in 0 until length()) {
            run(this[i])
        }
    }

}