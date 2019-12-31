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
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MenuItem.SHOW_AS_ACTION_ALWAYS
import android.view.MenuItem.SHOW_AS_ACTION_WITH_TEXT
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.ADBPlugin.Constants
import com.ADBPlugin.Constants.jsonObjectOf
import com.ADBPlugin.Constants.launchTasker
import com.ADBPlugin.R
import com.ADBPlugin.SendSingleCommand
import com.ADBPlugin.TaskerPlugin
import com.ADBPlugin.bundle.BundleScrubber
import com.ADBPlugin.bundle.PluginBundleManager
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponse.*
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.SkuDetailsParams
import kotlinx.android.synthetic.main.donation_dialog.*
import kotlinx.android.synthetic.main.main.*
import kotlinx.android.synthetic.main.testing_dialog.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.concurrent.thread

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

    var isLaunchedFromTasker = true

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // request permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 10
            )
        }

        BundleScrubber.scrub(intent)

        val localeBundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE)
        BundleScrubber.scrub(localeBundle)

        setContentView(R.layout.main)

        // Opened from launcher
        if (intent?.action == Intent.ACTION_MAIN) {
            isLaunchedFromTasker = false

            AlertDialog.Builder(this)
                .setMessage(getString(R.string.dialog_message, getString(R.string.app_name)))
                .setTitle(R.string.before_start)
                .setPositiveButton(R.string.test) { dialog, _ ->
                    dialog.cancel()
                }
                .setNegativeButton(R.string.open_tasker) { _, _ ->
                    launchTasker()
                }
                .create()
                .show()
        }

        if (savedInstanceState == null) {
            if (PluginBundleManager.isBundleValid(localeBundle)) {
                val message =
                    localeBundle?.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE)
                message?.apply {
                    if (contains('§')) { // backwards compatibility
                        val splitMessage =
                            split("§".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        input_ip.setText(splitMessage[0])
                        input_port.setText(splitMessage[1])
                        input_command.setText(splitMessage[2])
                        input_timeout.setText("50")
                        ctrl_c_switch.isChecked = false
                    } else {
                        val jsonObject = JSONObject(message)
                        input_ip.setText(jsonObject["ip"] as String)
                        input_port.setText(jsonObject["port"] as String)
                        input_command.setText(jsonObject["command"] as String)
                        input_timeout.setText(jsonObject["timeout"] as String)
                        ctrl_c_switch.isChecked = jsonObject["ctrl_c"] as Boolean
                    }
                }
            }
        }

        dropdown_command.isVisible = isLaunchedFromTasker
        dropdown_ip.isVisible = isLaunchedFromTasker
        dropdown_port.isVisible = isLaunchedFromTasker
        dropdown_timeout.isVisible = isLaunchedFromTasker

        // load tasker variables
        if (TaskerPlugin.hostSupportsRelevantVariables(intent.extras)) {
            val passedNames = arrayListOf<String>().apply {
                add(getString(R.string.variables))
                addAll(TaskerPlugin.getRelevantVariableList(intent.extras))
            }
            passedNames.forEach { Log.d(Constants.LOG_TAG, "variable: $it") }

            val arrayAdapter = ArrayAdapter<String>(
                this@EditActivity,
                android.R.layout.simple_spinner_dropdown_item, passedNames
            )

            dropdown_ip.apply {
                adapter = arrayAdapter
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
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
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
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
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        if (position != 0) {
                            input_command.text.append(passedNames[position])
                            setSelection(0)
                        }
                    }
                }
            }

            dropdown_timeout.apply {
                adapter = arrayAdapter
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        if (position != 0) {
                            input_timeout.text.append(passedNames[position])
                            setSelection(0)
                        }
                    }
                }
            }
        }

        donate_button.setOnClickListener {
            val dialog = AlertDialog.Builder(this)
                .setTitle(R.string.donate)
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .setIcon(R.drawable.money_black)
                .setView(
                    LayoutInflater
                        .from(this)
                        .inflate(R.layout.donation_dialog, null)
                )
                .create()

            dialog.show()

            var billingClient: BillingClient? = null
            billingClient = BillingClient.newBuilder(this)
                .setListener { responseCode, purchases ->
                    Log.d(
                        Constants.LOG_TAG,
                        "on purchases updated response: $responseCode, purchases: $purchases"
                    )
                    when (responseCode) {
                        FEATURE_NOT_SUPPORTED -> Unit
                        SERVICE_DISCONNECTED -> Unit
                        OK -> {
                            if (purchases != null) {
                                dialog.dismiss()
                                Toast.makeText(
                                    this,
                                    getString(R.string.donation_successful),
                                    Toast.LENGTH_LONG
                                ).show()
                                for (purchase in purchases) { // should only be 1
                                    billingClient!!.consumeAsync(purchase.purchaseToken) { responseCode, _ ->
                                        if (responseCode == OK)
                                            Log.d(Constants.LOG_TAG, "purchase consumed")
                                    }
                                }
                            }
                        }
                        USER_CANCELED -> Unit
                        SERVICE_UNAVAILABLE -> Unit
                        BILLING_UNAVAILABLE -> Unit
                        ITEM_UNAVAILABLE -> Unit
                        DEVELOPER_ERROR -> Unit
                        ERROR -> Unit
                        ITEM_ALREADY_OWNED -> Unit
                        ITEM_NOT_OWNED -> Unit
                    }
                }
                .build()

            var tries = 0
            val billingClientStateListener = object : BillingClientStateListener {
                override fun onBillingServiceDisconnected() {
                    Log.e(Constants.LOG_TAG, "Billing service disconnected")
                    if (tries == 10) {
                        runOnUiThread {
                            Toast.makeText(
                                this@EditActivity,
                                getString(R.string.couldnt_connect_to_play),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    val listener = this
                    GlobalScope.launch {
                        delay(100)
                        billingClient.startConnection(listener)
                        tries++
                    }
                }

                override fun onBillingSetupFinished(@BillingClient.BillingResponse responseCode: Int) {
                    Log.d(Constants.LOG_TAG, "Billing setup finished")
                    // get purchases
                    val skuList = arrayListOf(
                        "thank_you",
                        "big_thank_you",
                        "bigger_thank_you",
                        "biggest_thank_you"
                    )
                    billingClient.querySkuDetailsAsync(
                        SkuDetailsParams.newBuilder()
                            .setSkusList(skuList)
                            .setType(BillingClient.SkuType.INAPP)
                            .build()
                    ) { responseCode, skuDetailsList ->
                        Log.d(
                            Constants.LOG_TAG,
                            "Billing sku details received, $responseCode, $skuDetailsList"
                        )
                        if (responseCode == OK && skuDetailsList != null) {
                            for (skuDetails in skuDetailsList) {
                                val sku = skuDetails.sku
                                val price = skuDetails.price
                                when (sku) {
                                    "thank_you" -> dialog.thank_you_price.text = price
                                    "big_thank_you" -> dialog.big_thank_you_price.text = price
                                    "bigger_thank_you" -> dialog.bigger_thank_you_price.text = price
                                    "biggest_thank_you" -> dialog.biggest_thank_you_price.text =
                                        price
                                }
                            }
                        } else {
                            billing_not_working.isVisible = true
                            billing_not_working2.isVisible = true
                        }
                    }

                    val doPurchase = View.OnClickListener {
                        Log.d(Constants.LOG_TAG, "Billing onclick $it")
                        billingClient!!.launchBillingFlow(
                            this@EditActivity,
                            BillingFlowParams.newBuilder()
                                .setSku(
                                    when (it.id) {
                                        dialog.thank_you_card.id -> "thank_you"
                                        dialog.big_thank_you_card.id -> "big_thank_you"
                                        dialog.bigger_thank_you_card.id -> "bigger_thank_you"
                                        dialog.biggest_thank_you_card.id -> "biggest_thank_you"
                                        else -> null
                                    }
                                )
                                .setType(BillingClient.SkuType.INAPP)
                                .build()
                        )
                    }
                    dialog.apply {
                        thank_you_card.setOnClickListener(doPurchase)
                        big_thank_you_card.setOnClickListener(doPurchase)
                        bigger_thank_you_card.setOnClickListener(doPurchase)
                        biggest_thank_you_card.setOnClickListener(doPurchase)
                    }
                }
            }
            billingClient.startConnection(billingClientStateListener)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (isLaunchedFromTasker) {
            return super.onCreateOptionsMenu(menu)
        }
        menuInflater.inflate(R.menu.testing_menu, menu)
        menu.findItem(R.id.test)?.setShowAsAction(SHOW_AS_ACTION_ALWAYS or SHOW_AS_ACTION_WITH_TEXT)
        return true
    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem) =
        if (!isLaunchedFromTasker)
            when (item.itemId) {
                R.id.test -> {
                    val ipAddress = input_ip.text.toString()
                    val port = input_port.text.toString()
                    val command = input_command.text.toString()
                    val timeout = input_timeout.text.toString()
                    val ctrlC = ctrl_c_switch.isChecked

                    if (ipAddress.isBlank() || port.isBlank() || command.isBlank() || timeout.isBlank()) {
                        Toast.makeText(
                            applicationContext,
                            "Fill in all the fields!",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    } else {
                        val dialogView = layoutInflater.inflate(R.layout.testing_dialog, null)
                        val dialog = AlertDialog.Builder(this)
                            .setView(dialogView)
                            .setView(dialogView)
                            .setTitle(R.string.executing)
                            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                                dialog.cancel()
                            }
                            .create()
                        dialog.show()

                        thread(start = true) {
                            val logs = arrayListOf<String>()
                            try {
                                SendSingleCommand(
                                    logs = logs,
                                    context = this,
                                    ip = ipAddress,
                                    port = port.toInt(),
                                    command = command,
                                    timeout = timeout.toInt(),
                                    ctrlC = ctrlC
                                ) { _ ->
                                    var logcat = "${getString(R.string.logs)}\n"
                                    logs.forEach {
                                        logcat += it + "\n"
                                    }
                                    runOnUiThread {
                                        dialog.setTitle(R.string.success)
                                        dialogView.loading.isVisible = false
                                        dialogView.results_wrapper.isVisible = true
                                        dialogView.results.text = logcat
                                    }
                                }
                            } catch (e: Exception) {
                                var logcat = "${getString(R.string.logs)}\n"
                                logs.forEach {
                                    logcat += it + "\n"
                                }
                                runOnUiThread {
                                    dialog.setTitle(R.string.error)
                                    dialogView.loading.isVisible = false
                                    dialogView.results_wrapper.isVisible = true
                                    dialogView.results.text = "$logcat\n$e"
                                }
                            }
                        }
                    }
                    true
                }
                else -> super.onMenuItemSelected(featureId, item)
            }
        else super.onMenuItemSelected(featureId, item)

    override fun finish() {
        if (!isCanceled && isLaunchedFromTasker) {
            val ipAddress = input_ip.text.toString()
            val port = input_port.text.toString()
            val command = input_command.text.toString()
            val timeout = input_timeout.text.toString()
            val ctrlC = ctrl_c_switch.isChecked

            val result = jsonObjectOf(
                "ip" to ipAddress,
                "port" to port,
                "command" to command,
                "timeout" to timeout,
                "ctrl_c" to ctrlC
            )

            if (ipAddress.isNotEmpty() && port.isNotEmpty() && command.isNotEmpty() && timeout.isNotEmpty()) {
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
                val resultBundle =
                    PluginBundleManager.generateBundle(applicationContext, result.toString())
                resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, resultBundle)

                if (TaskerPlugin.Setting.hostSupportsOnFireVariableReplacement(this))
                    TaskerPlugin.Setting.setVariableReplaceKeys(
                        resultBundle,
                        arrayOf(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE)
                    )

                if (TaskerPlugin.hostSupportsRelevantVariables(intent.extras))
                    TaskerPlugin.addRelevantVariableList(
                        resultIntent, arrayOf(
                            """%output()
                                Raw terminal output.
                                All the output given by the console after executing your command.
                                Replace '()' with the index of the output, so, %output1, %output2 and so on.
                                You can also iterate over the output with For by putting %output() in Items.""".trimIndent(),
                            """%errors
                                Stacktrace of errors if they occur.
                                'Continue Task After Error' must be enabled to use this result.
                                Comparable to logcat""".trimIndent()
                        )
                    )

                /*
                 * The blurb is concise status text to be displayed in the host's UI.
                 */
                val blurb = generateBlurb(
                    applicationContext,
                    "$ipAddress:$port\n" +
                        "$command\n" +
                        "$timeout ms\n" +
                        if (ctrlC) "Ctrl+c" else "".trimMargin()
                )
                resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, blurb)
                setResult(Activity.RESULT_OK, resultIntent)
                super.finish()
            } else {
                Toast.makeText(applicationContext, "Fill in all the fields!", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            super.finish()
        }
    }

    companion object {

        /**
         * @param context Application context.
         * @param message The toast message to be displayed by the plug-in. Cannot be null.
         * @return A blurb for the plug-in.
         */
        /* package */
        internal fun generateBlurb(context: Context, message: String): String {
            val maxBlurbLength =
                context.resources.getInteger(R.integer.twofortyfouram_locale_maximum_blurb_length)

            return if (message.length > maxBlurbLength) {
                message.substring(0, maxBlurbLength)
            } else message
        }
    }
}