package com.ADBPlugin;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.cgutman.adblib.AdbBase64;
import com.cgutman.adblib.AdbConnection;
import com.cgutman.adblib.AdbCrypto;
import com.cgutman.adblib.AdbStream;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

import static android.org.apache.commons.codec.binary.Base64.encodeBase64String;


/**
 * Created by Jolan Rensen on 21-2-2017.
 * Used https://github.com/cgutman/AdbLibTest as example.
 */
public class SendSingleCommand {
    private String ip;
    private int port;
    private String command;
    private Context context;
    private ArrayList<String> splitResponses = null;

    // This implements the AdbBase64 interface required for AdbCrypto
    public static AdbBase64 getBase64Impl() {
        return new AdbBase64() {
            @Override
            public String encodeToString(byte[] arg0) {
                return encodeBase64String(arg0);
            }
        };
    }

    // This function loads a keypair from the specified files if one exists, and if not,
    // it creates a new keypair and saves it in the specified files
    private AdbCrypto setupCrypto(String pubKeyFile, String privKeyFile)
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        File pub = new File(Environment.getExternalStorageDirectory().getPath(), pubKeyFile);
        File priv = new File(Environment.getExternalStorageDirectory().getPath(), privKeyFile);
        AdbCrypto c = null;

        // Try to load a key pair from the files
        if (pub.exists() && priv.exists()) {
            try {
                c = AdbCrypto.loadAdbKeyPair(SendSingleCommand.getBase64Impl(), priv, pub);
            } catch (IOException e) {
                // Failed to read from file
                c = null;
            } catch (InvalidKeySpecException e) {
                // Key spec was invalid
                c = null;
            } catch (NoSuchAlgorithmException e) {
                // RSA algorithm was unsupported with the crypo packages available
                c = null;
            }
        }

        if (c == null) {
            // We couldn't load a key, so let's generate a new one
            c = AdbCrypto.generateAdbKeyPair(SendSingleCommand.getBase64Impl());

            // Save it
            c.saveAdbKeyPair(priv, pub);
            Log.i(Constants.LOG_TAG, "Generated new keypair");
        } else {
            Log.i(Constants.LOG_TAG, "Loaded existing keypair");
        }

        return c;
    }

    /**
     * Method to send a given ADB Shell command to a given device.
     *
     * @param context Context of the activity, to get the file store location (might be deprecated)
     * @param ip      IP of the device to connect to
     * @param port    Port of the device with given {@code ip} to connect to
     * @param command The command to be executed on {@code ip:port}
     */
    public void SendSingleCommand(Context context, String ip, int port, String command)
            throws IOException {
        this.ip = ip;
        this.port = port;
        this.command = command;
        this.context = context;

        AdbConnection adb;
        Socket sock;
        AdbCrypto crypto;

        // Setup the crypto object required for the AdbConnection
        try {
            crypto = setupCrypto("pub.key", "priv.key");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException("Couldn't read/write keys from /sdcard/pub.key and /sdcard/priv.key");
        }

        // Connect the socket to the remote host
        Log.i(Constants.LOG_TAG, "Socket connecting at " + ip + ":" + port);
        try {
            sock = new Socket(ip, port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new UnknownHostException(ip + " is no valid ip address");
        } catch (ConnectException e) {
            e.printStackTrace();
            throw new ConnectException("Device at " + ip + ":" + port + " has no adb enabled or connection is refused");
        } catch (NoRouteToHostException e) {
            e.printStackTrace();
            throw new NoRouteToHostException("Couldn't find adb device at " + ip + ":" + port);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Log.i(Constants.LOG_TAG, "Socket connected, creating AdbConnection...");

        // Construct the AdbConnection object
        try {
            adb = AdbConnection.create(sock, crypto);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Start the application layer connection process
        Log.d(Constants.LOG_TAG, "Created, ADB connecting...");
        try {
            adb.connect();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            Log.e(Constants.LOG_TAG, "ADB already connected...");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }
        Log.d(Constants.LOG_TAG, "ADB connected, opening shell stream...");


        // Open the shell stream of ADB
        final AdbStream stream;
        try {
            stream = adb.open("shell:");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }


        Log.d(Constants.LOG_TAG, "Opened, writing command: " + this.command);
        try {
            stream.write(this.command + '\n');
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }

        Log.d(Constants.LOG_TAG, "Command sent, getting responses");

        /*
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        String responses = "";
        boolean done = false;
        while (!done) {
            try {
                byte[] responseBytes = stream.read();
                String response = new String(responseBytes, "US-ASCII");
                if (response.substring(response.length() - 2).equals("$ ") ||
                        response.substring(response.length() - 2).equals("# ")) {
                    done = true;
                    responses += response;
                    break;
                } else {
                    responses += response;
                }
            } catch (InterruptedException e) {
                Log.e(Constants.LOG_TAG, e.getMessage());
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(Constants.LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
        }

        Log.d(Constants.LOG_TAG, "response: " + responses);

        // Trying to split the response on newlines, not waterproof
        splitResponses = new ArrayList<>();
            for (String item : responses.split("\\n")) {
                splitResponses.add(item.trim());
            }


        Log.d(Constants.LOG_TAG, "Sending exit command and waiting for stream to close");
        try {
            stream.write(" exit" + '\n');
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }

        while (!stream.isClosed()) {
            try {
                Log.d(Constants.LOG_TAG, "response after exit: " + new String(stream.read(), "US-ASCII"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }

        Log.d(Constants.LOG_TAG, "Stream closed, closing Adb...");

        try {
            adb.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(Constants.LOG_TAG, "ADB Closed");
    }

    public ArrayList<String> getSplitResponses() {
        return splitResponses;
    }
}
