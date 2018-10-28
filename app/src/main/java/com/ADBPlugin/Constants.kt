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

import android.content.Context
import org.json.JSONObject

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
}
