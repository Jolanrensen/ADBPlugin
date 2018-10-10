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

import android.annotation.TargetApi
import android.app.Activity
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem

import com.twofortyfouram.locale.BreadCrumber
import com.ADBPlugin.Constants
import com.ADBPlugin.R

/**
 * Superclass for plug-in Activities. This class takes care of initializing aspects of the plug-in's UI to
 * look more integrated with the plug-in host.
 */
abstract class AbstractPluginActivity : Activity() {
    /**
     * Flag boolean that can only be set to true via the "Don't Save"
     * [com.twofortyfouram.locale.platform.R.id.twofortyfouram_locale_menu_dontsave] menu item in
     * [.onMenuItemSelected].
     */
    /*
     * There is no need to save/restore this field's state.
     */
    /**
     * During [.finish], subclasses can call this method to determine whether the Activity was
     * canceled.
     *
     * @return True if the Activity was canceled. False if the Activity was not canceled.
     */
    protected var isCanceled = false
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupTitleApi11()
    }

    private fun setupTitleApi11() {
        var callingApplicationLabel: CharSequence? = null
        try {
            callingApplicationLabel = packageManager.getApplicationLabel(packageManager.getApplicationInfo(callingPackage,
                    0))
        } catch (e: NameNotFoundException) {
            if (Constants.IS_LOGGABLE) {
                Log.e(Constants.LOG_TAG, "Calling package couldn't be found", e) //$NON-NLS-1$
            }
        }

        if (null != callingApplicationLabel) {
            title = callingApplicationLabel
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.twofortyfouram_locale_help_save_dontsave, menu)
        setupActionBarApi11()
        setupActionBarApi14()

        return true
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private fun setupActionBarApi11() {
        actionBar!!.subtitle = BreadCrumber.generateBreadcrumb(applicationContext, intent,
                getString(R.string.plugin_name))
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private fun setupActionBarApi14() {
        actionBar!!.setDisplayHomeAsUpEnabled(true)

        /*
         * Note: There is a small TOCTOU error here, in that the host could be uninstalled right after
         * launching the plug-in. That would cause getApplicationIcon() to return the default application
         * icon. It won't fail, but it will return an incorrect icon.
         *
         * In practice, the chances that the host will be uninstalled while the plug-in UI is running are very
         * slim.
         */
        try {
            actionBar!!.setIcon(packageManager.getApplicationIcon(callingPackage))
        } catch (e: NameNotFoundException) {
            if (Constants.IS_LOGGABLE) {
                Log.w(Constants.LOG_TAG, "An error occurred loading the host's icon", e) //$NON-NLS-1$
            }
        }

    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        R.id.twofortyfouram_locale_menu_dontsave -> {
            isCanceled = true
            finish()
            true
        }
        R.id.twofortyfouram_locale_menu_save -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }


}
