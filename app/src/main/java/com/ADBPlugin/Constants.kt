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
 */

package com.ADBPlugin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.os.bundleOf
import com.ADBPlugin.bundle.BundleScrubber
import com.ADBPlugin.bundle.PluginBundleManager
import com.twofortyfouram.locale.PackageUtilities
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter
import java.util.ArrayList
import java.util.Locale

/**
 * Class of constants used by this Locale plug-in.
 */
object Constants {
    /**
     * Log tag for logcat messages.
     */

    val LOG_TAG = "com.ADBPlugin"

    /**
     * Flag to enable logcat messages.
     */
    val IS_LOGGABLE = BuildConfig.DEBUG

    /**
     * Flag to enable runtime checking of method parameters.
     */
    val IS_PARAMETER_CHECKING_ENABLED = BuildConfig.DEBUG

    /**
     * Flag to enable runtime checking of whether a method is called on the correct thread.
     */
    val IS_CORRECT_THREAD_CHECKING_ENABLED = BuildConfig.DEBUG

    /**
     * Determines the "versionCode" in the `AndroidManifest`.
     *
     * @param context to read the versionCode.
     * @return versionCode of the app.
     */
    fun getVersionCode(context: Context?): Int {
        if (Constants.IS_PARAMETER_CHECKING_ENABLED) {
            if (null == context) {
                throw IllegalArgumentException("context cannot be null") //$NON-NLS-1$
            }
        }

        return try {
            context!!.packageManager.getPackageInfo(context.packageName, 0).versionCode
        } catch (e: UnsupportedOperationException) {
            /*
         * This exception is thrown by test contexts
         */

            1
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun jsonObjectOf(vararg pair: Pair<String, Any>) = JSONObject()
        .apply {
            pair.forEach {
                put(it.first, it.second)
            }
        }

    private val APP_STORE_URI =
        "https://play.google.com/store/apps/details?id=net.dinglisch.android.taskerm"

    fun Activity.launchTasker() {
        val manager = packageManager

        val compatiblePackage = PackageUtilities.getCompatiblePackage(manager, null)

        if (null != compatiblePackage) {
            // after this point, assume Locale-compatible package is installed
            Log.v(
                Constants.LOG_TAG,
                String.format(
                    Locale.US,
                    "Locale-compatible package %s is installed",
                    compatiblePackage
                )
            ) //$NON-NLS-1$
            try {
                val i = manager.getLaunchIntentForPackage(compatiblePackage)
                i!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
            } catch (e: Exception) {
                /*
                 * Under normal circumstances, this shouldn't happen. Potential causes would be a TOCTOU error
                 * where the application is uninstalled or the application enforcing permissions that it
                 * shouldn't be.
                 */
                Log.e(Constants.LOG_TAG, "Error launching Activity", e) //$NON-NLS-1$
            }
        } else {
            Log.i(Constants.LOG_TAG, "Locale-compatible package is not installed") //$NON-NLS-1$

            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            String.format(
                                Locale.US,
                                APP_STORE_URI,
                                "com.twofortyfouram.locale", packageName
                            )
                        )
                    ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                ) //$NON-NLS-1$
            } catch (e: Exception) {
                Log.e(Constants.LOG_TAG, "Error launching Activity", e) //$NON-NLS-1$
            }
        }
        //finish()
    }

    fun runOnMainThread(post: () -> Unit) = Handler(Looper.getMainLooper()).post(post)

    fun handleFireMessage(context: Context, intent: Intent) {
        /*
         * Always be strict on input parameters! A malicious third-party app could send a malformed Intent.
         */

        if (com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING != intent.action) {
            if (IS_LOGGABLE) {
                Log.e(
                    LOG_TAG,
                    String.format(
                        Locale.US,
                        "Received unexpected Intent action %s",
                        intent.action
                    )
                ) //$NON-NLS-1$
            }
            return
        }

        BundleScrubber.scrub(intent)

        val bundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE)
        BundleScrubber.scrub(bundle)

        if (PluginBundleManager.isBundleValid(bundle)) {
            val message = bundle?.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE)

            val values =
                if (message!!.contains('§')) { //backwards compatibility
                    val split = message.split("§".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    jsonObjectOf(
                        "ip" to split[0],
                        "port" to split[1],
                        "command" to split[2],
                        "timeout" to 50,
                        "ctrl_c" to false
                    )
                } else JSONObject(message)

            val logs = arrayListOf<String>()
            try {
                //Run the program with all the given variables
                SendSingleCommand(
                    logs = logs,
                    context = context,
                    ip = values["ip"] as String,
                    port = (values["port"] as String).toInt(),
                    command = values["command"] as String,
                    timeout = (values["timeout"] as String).toInt(),
                    ctrlC = values["ctrl_c"] as Boolean
                ) {
                    //Log the result and signal Tasker
                    Log.d(LOG_TAG, "Executed single command")

                    // add result if it exists
                    val responseBundle = bundleOf()
                    it?.apply {
                        responseBundle.putStringArrayList("%output", this)

                        TaskerPlugin.addVariableBundle(
                            responseBundle,
                            bundleOf()
                        )
                    }

                    // Tell Takser I'm done
                    TaskerPlugin.Setting.signalFinish(
                        context,
                        intent,
                        TaskerPlugin.Setting.RESULT_CODE_OK,
                        responseBundle
                    )
                }
            } catch (e: Exception) { // if couldn't read/write key files
                Log.e(LOG_TAG, "", e)
                displayError(e, context, intent, logs)
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
    fun displayError(e: Exception, context: Context, intent: Intent, logs: ArrayList<String>) {
        Log.e(LOG_TAG, e.message)
        val errors = Bundle()
        errors.putString(TaskerPlugin.Setting.VARNAME_ERROR_MESSAGE, e.message)

        var logcat = ""
        logs.forEach {
            logcat += it + "\n"
        }
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        errors.putString("%errors", logcat + sw.toString())
        TaskerPlugin.addVariableBundle(
            errors,
            bundleOf()
        )
        TaskerPlugin.Setting.signalFinish(
            context,
            intent,
            TaskerPlugin.Setting.RESULT_CODE_FAILED_PLUGIN_FIRST,
            errors
        )
    }
}
