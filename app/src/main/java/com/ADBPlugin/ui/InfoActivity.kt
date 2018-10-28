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

package com.ADBPlugin.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.ADBPlugin.Constants
import com.twofortyfouram.locale.PackageUtilities
import java.util.Locale

/**
 * If the user tries to launch the plug-in via the "Open" button in Google Play, this will redirect the user
 * to Tasker.
 */
class InfoActivity : Activity() {

    /**
     * URI to Locale in the native version of the Google Play.
     */
    private val APP_STORE_URI = "https://play.google.com/store/apps/details?id=net.dinglisch.android.taskerm"

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        finish()
    }
}