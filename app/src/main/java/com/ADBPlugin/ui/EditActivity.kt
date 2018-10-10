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

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.ADBPlugin.R
import com.ADBPlugin.TaskerPlugin
import com.ADBPlugin.bundle.BundleScrubber
import com.ADBPlugin.bundle.PluginBundleManager
import kotlinx.android.synthetic.main.main.*
import com.ADBPlugin.Constants.jsonObjectOf
import org.json.JSONObject
import android.util.Log
import android.view.View
import android.widget.*
import com.ADBPlugin.Constants


/**
 * This is the "Edit" activity for a Locale Plug-in.
 *
 *
 * This Activity can be started in one of two states:
 *
 *  * New plug-in instance: The Activity's Intent will not contain
 * [com.twofortyfouram.locale.Intent.EXTRA_BUNDLE].
 *  * Old plug-in instance: The Activity's Intent will contain
 * [com.twofortyfouram.locale.Intent.EXTRA_BUNDLE] from a previously saved plug-in instance that the
 * user is editing.
 *
 *
 * @see com.twofortyfouram.locale.Intent.ACTION_EDIT_SETTING
 *
 * @see com.twofortyfouram.locale.Intent.EXTRA_BUNDLE
 */
class EditActivity : AbstractPluginActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // request permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 10)
        }

        BundleScrubber.scrub(intent)

        val localeBundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE)
        BundleScrubber.scrub(localeBundle)

        setContentView(R.layout.main)

        if (null == savedInstanceState) {
            if (PluginBundleManager.isBundleValid(localeBundle)) {
                val message = localeBundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE)
                message?.apply {
                    if (contains('§')) { // backwards compatibility
                        val splitMessage = split("§".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        input_ip.setText(splitMessage[0])
                        input_port.setText(splitMessage[1])
                        input_command.setText(splitMessage[2])
                        ctrl_c_switch.isEnabled = false
                    } else {
                        val jsonObject = JSONObject(message)
                        input_ip.setText(jsonObject["ip"] as String)
                        input_port.setText(jsonObject["port"] as String)
                        input_command.setText(jsonObject["command"] as String)
                        ctrl_c_switch.isChecked = jsonObject["ctrl_c"] as Boolean
                    }
                }
            }
        }

        // load tasker variables
        if (TaskerPlugin.hostSupportsRelevantVariables(intent.extras)) {
            val passedNames = arrayListOf<String>().apply {
                add(getString(R.string.variables))
                addAll(TaskerPlugin.getRelevantVariableList(intent.extras))
            }
            passedNames.forEach { Log.d(Constants.LOG_TAG, "variable: $it") }

            val arrayAdapter = ArrayAdapter<String>(this@EditActivity,
                    android.R.layout.simple_spinner_dropdown_item, passedNames)

            dropdown_ip.apply {
                adapter = arrayAdapter
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position != 0) {
                            input_ip.text.append(passedNames[position])
                            setSelection(0)
                        }
                    }
                }
            }

            dropdown_port.apply {
                adapter = arrayAdapter
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position != 0) {
                            input_port.text.append(passedNames[position])
                            setSelection(0)
                        }
                    }
                }
            }

            dropdown_command.apply {
                adapter = arrayAdapter
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position != 0) {
                            input_command.text.append(passedNames[position])
                            setSelection(0)
                        }
                    }
                }
            }


        }
    }

    override fun finish() {
        if (!isCanceled) {
            val ipAddress = input_ip.text.toString()
            val port = input_port.text.toString()
            val command = input_command.text.toString()
            val ctrlC = ctrl_c_switch.isChecked

            val result = jsonObjectOf(
                    "ip"        to ipAddress,
                    "port"      to port,
                    "command"   to command,
                    "ctrl_c"    to ctrlC
            )

            if (ipAddress.isNotEmpty() && port.isNotEmpty() && command.isNotEmpty()) {
                val resultIntent = Intent()

                if (TaskerPlugin.Setting.hostSupportsSynchronousExecution(intent.extras))
                    TaskerPlugin.Setting.requestTimeoutMS(resultIntent, 60000)


                /*
                 * This extra is the data to ourselves: either for the Activity or the BroadcastReceiver. Note
                 * that anything placed in this Bundle must be available to Locale's class loader. So storing
                 * String, int, and other standard objects will work just fine. Parcelable objects are not
                 * acceptable, unless they also implement Serializable. Serializable objects must be standard
                 * Android platform objects (A Serializable class private to this plug-in's APK cannot be
                 * stored in the Bundle, as Locale's classloader will not recognize it).
                 */
                val resultBundle = PluginBundleManager.generateBundle(applicationContext, result.toString())
                resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, resultBundle)

                if (TaskerPlugin.Setting.hostSupportsOnFireVariableReplacement(this))
                    TaskerPlugin.Setting.setVariableReplaceKeys(resultBundle, arrayOf(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE))

                if (TaskerPlugin.hostSupportsRelevantVariables(intent.extras))
                    TaskerPlugin.addRelevantVariableList(resultIntent, arrayOf("%output()\nRaw console output\nAll the output given by the console after executing your command. " +
                            "Replace '()' with the index of the output, so, %output1, %output2 and so on." +
                            "You can also iterate over the output with For by putting %output() in Items."))

                /*
                 * The blurb is concise status text to be displayed in the host's UI.
                 */
                val blurb = generateBlurb(applicationContext,
                        "$ipAddress:$port\n" +
                                "$command\n"+
                                if (ctrlC) "Ctrl+c" else "".trimMargin())
                resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, blurb)
                setResult(Activity.RESULT_OK, resultIntent)
            }
        }

        super.finish()
    }

    companion object {

        /**
         * @param context Application context.
         * @param message The toast message to be displayed by the plug-in. Cannot be null.
         * @return A blurb for the plug-in.
         */
        /* package */
        internal fun generateBlurb(context: Context, message: String): String {
            val maxBlurbLength = context.resources.getInteger(R.integer.twofortyfouram_locale_maximum_blurb_length)

            return if (message.length > maxBlurbLength) {
                message.substring(0, maxBlurbLength)
            } else message

        }
    }
}