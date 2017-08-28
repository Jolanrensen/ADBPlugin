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

package com.ADBPlugin.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.ADBPlugin.Constants;
import com.ADBPlugin.SendSingleCommand;
import com.ADBPlugin.TaskerPlugin;
import com.ADBPlugin.bundle.BundleScrubber;
import com.ADBPlugin.bundle.PluginBundleManager;
import com.ADBPlugin.ui.EditActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * This is the "fire" BroadcastReceiver for a Locale Plug-in setting.
 *
 * @see com.twofortyfouram.locale.Intent#ACTION_FIRE_SETTING
 * @see com.twofortyfouram.locale.Intent#EXTRA_BUNDLE
 */
public final class FireReceiver extends BroadcastReceiver {
    public static String message;

    /**
     * @param context {@inheritDoc}.
     * @param intent  the incoming {@link com.twofortyfouram.locale.Intent#ACTION_FIRE_SETTING} Intent. This
     *                should contain the {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE} that was saved by
     *                {@link EditActivity} and later broadcast by Locale.
     */
    @Override
    public void onReceive(final Context context, final Intent intent) {
        Thread thread = null;
        /*
         * Always be strict on input parameters! A malicious third-party app could send a malformed Intent.
         */

        if (!com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING.equals(intent.getAction())) {
            if (Constants.IS_LOGGABLE) {
                Log.e(Constants.LOG_TAG,
                        String.format(Locale.US, "Received unexpected Intent action %s", intent.getAction())); //$NON-NLS-1$
            }
            return;
        }

        BundleScrubber.scrub(intent);

        final Bundle bundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
        BundleScrubber.scrub(bundle);

        if (PluginBundleManager.isBundleValid(bundle)) {
            message = bundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE);


            final String[] messageSplit = message.split("§");
            /*for (String part: messageSplit) {
                Toast.makeText(context, part, Toast.LENGTH_LONG).show();
            }*/

            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //run the program with all the given variables
                        SendSingleCommand sendSingleCommand = new SendSingleCommand();
                        sendSingleCommand.SendSingleCommand(context, messageSplit[0], Integer.parseInt(messageSplit[1]), messageSplit[2]);
                        //Log the result and signal Tasker
                        Log.d(Constants.LOG_TAG, "Executed single command");
                        ArrayList<String> responses = sendSingleCommand.getSplitResponses();
                        Bundle responseBundle = new Bundle();
                        responseBundle.putStringArrayList("%output", responses);
                        TaskerPlugin.addVariableBundle(getResultExtras(true), responseBundle);
                        TaskerPlugin.Setting.signalFinish(context, intent, TaskerPlugin.Setting.RESULT_CODE_OK, responseBundle);
                    } catch (IOException e) { //if couldn't read/write key files
                        displayError(e, context, intent);
                    }
                }
            });
            thread.start();

            //Tell Tasker I'm not done yet, since the thread is still running
            if (isOrderedBroadcast()) {
                setResultCode(TaskerPlugin.Setting.RESULT_CODE_PENDING);
                //Log.d(Constants.LOG_TAG, "Told Tasker to wait for a bit");
            }
        }
    }

    /**
     * Simple method to inform Tasker and the system of the error that has occured.
     *
     * @param e
     * @param context
     * @param intent
     */
    public void displayError(Exception e, Context context, Intent intent) {
        Log.e(Constants.LOG_TAG, e.getMessage());
        Bundle errors = new Bundle();
        errors.putString(TaskerPlugin.Setting.VARNAME_ERROR_MESSAGE, e.getMessage());
        TaskerPlugin.Setting.signalFinish(context, intent, TaskerPlugin.Setting.RESULT_CODE_FAILED_PLUGIN_FIRST, errors);
    }
}