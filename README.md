# ADBPlugin
Tasker Plugin to send ADB Shell commands
https://play.google.com/store/apps/details?id=com.ADBPlugin

Most of the actual "plugin" stuff can be found at https://github.com/Jolanrensen/ADBPlugin/tree/master/app/src/main/java/com/ADBPlugin. The rest consists of libraries and the necessary locale/tasker-api stuff.
The latest (debug) build of the app can be found at https://github.com/Jolanrensen/ADBPlugin/blob/master/app/build/outputs/apk/app-debug.apk

This Tasker plugin allows you to send a (series of) ADB Shell command(s) to a remote device (or the device itself) that has ADB over WiFi enabled. This app does need Tasker to work! (https://play.google.com/store/apps/details?id=net.dinglisch.android.taskerm, this is a paid app with a 7 days free trial)

On the target device you can do anything you would normally be able to do when using the terminal or ADB Shell of that device. This includes opening apps, moving files, simulating input et cetera (The sky is the limit and Google is your friend).

My app can be used as an action in Tasker and needs to be configured properly when added to a task to work.

NOTE: 
-	You can send multiple commands at once by separating them with a ";". 
-	All commands in a single action are automatically followed with an "exit" command so to close the ADB connection. This means that if you want to send multiple commands that are depending on each other, you will need to put them in a single Tasker action and separate them with ";".
-	If you want multiple actions of my plugin to run after each other in a Tasker task, please keep the timeout set as is. Tasker will continue when the command is sent.
-	The first time you try to connect to another device via ADB it will ask you if you trust the device. For this plugin to work correctly, you'll need to always "trust this computer".
-	The output of the console can be viewed and reused from within Tasker. This can be done by accessing %output1, %output2 etc. or by iteration over %output() using a for loop. 
-	The plugin will generate two key files on your sdcard to be able to make a secure ADB connection to the devices.

Now for some useful commands! 
-	A command I myself use a lot is to turn my ADB over WiFi enabled AndroidTV (Nvidia SHIELD) on or off by sending the command "input keyevent POWER", this simulates a power button press.
-	Of course you can vary which key to press, for example to press the arrow keys you can do "input keyevent DPAD_RIGHT" or “…LEFT” etc.
-	Another simple command is "reboot", which will, you guessed it, reboot the device! You can also put "reboot -p" here to power it down.
-	A cool thing to be able to do is to launch apps on the device. You will have to Google a bit to find the Main activity of an app. This example will open Chrome on the device: "am start -n com.android.chrome/com.google.android.apps.chrome.Main".
-	When you fill in “localhost” in the IP address field, every command will be executed on the device itself! This works even without root if you, of course, have ADB over WiFi enabled (can be activated from a pc using “adb tcpip 5555”).
Of course there are many other commands, the sky is the limit!
This is my first ever published app, so please leave feedback and submit bugs so I can try my best to fix them! Any tips are more than welcome!

The idea to create this app came from https://play.google.com/store/apps/details?id=com.cgutman.androidremotedebugger, which allows the user to connect to a remote device via an interface in the app itself. Please read the description of that app too, it contains some tips on enabling ADB over WiFi. I use the same AdbLib Java library in my app.

I use the AdbLib library found at https://github.com/cgutman/AdbLib and use https://github.com/cgutman/AdbLibTest as an example for how to use the library. I adapted this example to work with a given command and converted it to a Tasker plugin.

For help, you can email me or visit the XDA-Developers thread at https://forum.xda-developers.com/u/tasker-tips-tricks/plugin-remote-adb-shell-t3562013. This thread also contains some useful tips for accessing the local device running the task without root.

I made the app free so anyone can try it for themselves but of course donations are always welcome at http://paypal.me/JolanRensen.
