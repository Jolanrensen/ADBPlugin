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
import android.util.Log
import com.twofortyfouram.locale.PackageUtilities
import org.json.JSONObject
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

    private val APP_STORE_URI = "https://play.google.com/store/apps/details?id=net.dinglisch.android.taskerm"

    fun Activity.launchTasker() {
        val manager = packageManager

        val compatiblePackage = PackageUtilities.getCompatiblePackage(manager, null)

        if (null != compatiblePackage) {
            // after this point, assume Locale-compatible package is installed
            Log.v(
                Constants.LOG_TAG,
                String.format(Locale.US, "Locale-compatible package %s is installed", compatiblePackage)
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
}
