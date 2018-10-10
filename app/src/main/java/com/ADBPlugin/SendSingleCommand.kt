package com.ADBPlugin

import android.content.Context
import android.os.Environment
import android.util.Log

import com.tananaev.adblib.AdbBase64
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import com.tananaev.adblib.AdbStream

import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.Socket
import java.net.UnknownHostException
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.util.ArrayList


import android.org.apache.commons.codec.binary.Base64.encodeBase64String
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.lang.Exception
import java.lang.UnsupportedOperationException
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit


/**
 * Created by Jolan Rensen on 21-2-2017.
 * Used https://github.com/cgutman/AdbLibTest as example.
 */

class SendSingleCommand

/**
 * Method to send a given ADB Shell command to a given device.
 *
 * @param context Context of the activity, to get the file store location (might be deprecated)
 * @param ip      IP of the device to connect to
 * @param port    Port of the device with given `ip` to connect to
 * @param command The command to be executed on `ip:port`
 */
@Throws(IOException::class, Exception::class)
constructor(private val context: Context,
            private val ip: String,
            private val port: Int,
            private val command: String,
            private val ctrlC: Boolean) {

    var splitResponses: ArrayList<String>? = null
        private set

    var stream: AdbStream
    var adb: AdbConnection

    // This implements the AdbBase64 interface required for AdbCrypto
    val base64Impl: AdbBase64
        get() = AdbBase64 { arg0 -> encodeBase64String(arg0) }

    init {
        val sock: Socket
        val crypto: AdbCrypto

        // Setup the crypto object required for the AdbConnection
        try {
            crypto = setupCrypto("pub.key", "priv.key")
        } catch (e: IOException) {
            throw IOException("Couldn't read/write keys from /sdcard/ADBPlugin/pub.key and /sdcard/ADBPlugin/priv.key, make sure you gave storage read/write permission.", e)
        }

        // Connect the socket to the remote host
        Log.i(Constants.LOG_TAG, "Socket connecting at $ip:$port")
        try {
            sock = Socket(ip, port)
        } catch (e: UnknownHostException) {
            throw Exception("$ip is no valid ip address", e)
        } catch (e: ConnectException) {
            throw Exception("Device at $ip:$port has no adb enabled or connection is refused", e)
        } catch (e: NoRouteToHostException) {
            throw Exception("Couldn't find adb device at $ip:$port", e)
        }

        Log.i(Constants.LOG_TAG, "Socket connected, creating AdbConnection...")

        // Construct the AdbConnection object
        adb = AdbConnection.create(sock, crypto)

        // Start the application layer connection process
        Log.d(Constants.LOG_TAG, "Created, ADB connecting...")
        try {
            adb.connect()
        } catch (e: IllegalStateException) {
            Log.e(Constants.LOG_TAG, "ADB already connected...", e)
        } catch (e: InterruptedException) {
            throw Exception("unable to wait for the connection to finish", e)
        } catch (e: IOException) {
            throw IOException("the socket fails while connecting", e)
        }
        Log.d(Constants.LOG_TAG, "ADB connected, opening shell stream...")

        // sending...
        try {
            stream = adb.open("shell:")
        } catch (e: IOException) {
            throw IOException("stream fails while sending the packet", e)
        } catch (e: UnsupportedEncodingException) {
            throw Exception("destination cannot be encoded to UTF-8", e)
        } catch (e: InterruptedException) {
            throw Exception("unable to wait for the connection to finish", e)
        }

        // wait until stream is opened
        Log.d(Constants.LOG_TAG, "Writing command: $command")

        //var commandSent = false
        //while (!commandSent) {
            try {
                if (ctrlC) stream.write(byteArrayOf(0x03))
                stream.write(command + '\n')
         //       commandSent = true
            } catch (e: IOException) {
                throw IOException("Couldn't write command, stream fails while sending data, try without CTRL+C", e)
            } catch (e: InterruptedException) {
                throw Exception("Couldn't write command, unable to wait to send data", e)
            }
       // }

        Log.d(Constants.LOG_TAG, "Command sent, getting responses")

        var responses = ""
        var done = false
        while (!done && !stream.isClosed) {
            try {
                val responseBytes = stream.read()
                val response = String(responseBytes, charset("US-ASCII"))
                if (response.substring(response.length - 2) == "$ " ||
                        response.substring(response.length - 2) == "# " ||
                        response.substring(response.length - 1) == "$" ||
                        response.substring(response.length - 1) == "#") {
                    done = true
                    responses += response
                    break
                } else {
                    responses += response
                }
            } catch (e: InterruptedException) {
                throw Exception("Couldn't get response, unable to wait for data", e)
            } catch (e: IOException) {
                throw Exception("Couldn't get response, stream fails while waiting or stream closed", e)
            } catch (e: IndexOutOfBoundsException) {
                throw Exception("Couldn't parse response", e)
            }
        }

        Log.d(Constants.LOG_TAG, "response:\n$responses")

        // Trying to split the response on newlines, not waterproof
        splitResponses = ArrayList()
        for (item in responses.split("\\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            splitResponses!!.add(item.trim { it <= ' ' })
        }

        Log.d(Constants.LOG_TAG, "Sending close command and waiting for stream to close")
        stream.close()
        launch {
            delay(10, TimeUnit.SECONDS)
            if (!stream.isClosed)
                throw Exception("Stream didn't close after 10 seconds waiting")
        }
        launch {
            Log.d(Constants.LOG_TAG, "Closing stream...")
            while (!stream.isClosed) {}
            Log.d(Constants.LOG_TAG, "Stream closed, closing Adb...")

            try {
                adb.close()
            } catch (e: IOException) {
                throw IOException("Couldn't close ADB connection socket", e)
            }
            launch {
                delay(10, TimeUnit.SECONDS)
                if (!sock.isClosed)
                    throw Exception("ADB connection socket didn't close after 10 seconds waiting")
            }
            launch {
                while (!sock.isClosed) {}
                Log.d(Constants.LOG_TAG, "ADB connection socket closed")
            }
        }
    }

    // This function loads a keypair from the specified files if one exists, and if not,
    // it creates a new keypair and saves it in the specified files
    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class, IOException::class)
    private fun setupCrypto(pubKeyFile: String, privKeyFile: String): AdbCrypto {
        val pub = File(context.filesDir, pubKeyFile)
        val priv = File(context.filesDir, privKeyFile)
        Log.d(Constants.LOG_TAG, "keyfiles paths: ${pub.path}, ${priv.path}")
        var c: AdbCrypto? = null

        // Try to load a key pair from the files
        if (pub.exists() && priv.exists()) {
            c = try {
                AdbCrypto.loadAdbKeyPair(base64Impl, priv, pub)
            } catch (e: IOException) {
                // Failed to read from file
                null
            } catch (e: InvalidKeySpecException) {
                // Key spec was invalid
                null
            } catch (e: NoSuchAlgorithmException) {
                // RSA algorithm was unsupported with the crypo packages available
                null
            }
        }

        if (c == null) {
            // We couldn't load a key, so let's generate a new one
            c = AdbCrypto.generateAdbKeyPair(base64Impl)

            // Save it
            c!!.saveAdbKeyPair(priv, pub)
            Log.i(Constants.LOG_TAG, "Generated new keypair")
        } else {
            Log.i(Constants.LOG_TAG, "Loaded existing keypair")
        }

        return c
    }
}
