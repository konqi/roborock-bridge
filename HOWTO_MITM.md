# Decoding the protocol

This document describes how to break and enter the decoded stream of data between the roborock app and roborock servers.
Following this guide you will be able to find slight variations in the protocol and thus suggest changes to the
implementation that add compatibility for your device.

## What you will need

- a rooted android device (don't worry, a virtual one is fine)
- [mitmproxy](https://mitmproxy.org/)
- [wireshark](https://www.wireshark.org/)

## Getting a rooted virtual android device (AVD)

- Install Android Studio
- Start the Android Device Manager \
  This is somewhat hidden on the start screen of android studio. Select "Virtual
  Device Manager"
    - on MacOS hit the ⠇ in the top right corner
    - on Windows under "More Actions"
- Create an android virtual device (AVD) and start it (best to use a system image that has "Google Play" enabled, see target column)
- Root the AVD with [rootAVD](https://gitlab.com/newbit/rootAVD) \
  This will also install Magisk, which we will use later on.
  The commands to run are:
  - `./rootAVD.sh ListAllAVDs` on Linux and Mac or `rootAVD.bat ListAllAVDs` on Windows
  - If you have just the emulated device running then you can copy the first line that ends with ramdisk.img from the output and run it
  - If rootAVD complains that ADB is missing:
     - Go back to Android Studio and in the same place where you started the device manager, select `SDK Manager`
     - Go to the `SDK Tools` tab and make sure you have the `Android SDK Platform-Tools` installed
     - You should also see the `Android SDK Location` at the top of the current window, copy this location and execute the following in your terminal
         - on Windows `set PATH=%PATH%;<the path you copied>\platform-tools`
         - on Linux / MacOS `PATH=$PATH;<the path you copied>/platform-tools`
     - Now in the same window re-attempt rooting the AVD

## Install the Roborock app on the virtual device

If the following seems too much trouble, or you simply don't have an android device, I suggest you create a virtual android
device with Play Store enabled and install the app the regular way.
You'll have to log into a Google Account on the virtual device, but that might be the most convenient way to do it.

You can get the android app from many places, some more shady than others.
I simply downloaded the app from my actual phone using

    ./adb -e shell pm list packages

and then

    ./adb pull <path from list packages command> <path to where you want the apk locally>

With the apk on your local device, you can simply drag and drop it onto the running emulated device.

Off course, to do this you have enable developer mode on your android phone and connect it.


## Install wireguard on the android virtual device (AVD)

Install wireguard on the android device, you have two good options:

- If you have the Play Store installed and configured, simply use that
- You can download the apk directly from [wireguard](https://www.wireguard.com/install/) and drag & drop it on your
  device

## Setup mitmproxy

Install [mitmproxy](https://mitmproxy.org/) on your machine any way you like if you haven't done so already.

Run mitmdump and have it log its ssl keys, with:
  - on Linux, MacOS: `SSLKEYLOGFILE="$PWD/sslkeylogfile.txt" mitmdump --mode wireguard` ([see](https://docs.mitmproxy.org/stable/howto-wireshark-tls/))
  - on Windows (in PowerShell): `$env:SSLKEYLOGFILE="~/.mitmproxy/sslkeylogfile.txt"` and then `mitmdump --mode wireguard`

Copy wireguard config onto the virtual device
  - Copy the lines between the `--------` in the console into a text file and save it as `wireguard.conf` (conf-extension is important)
  - Drag & Drop the file onto the emulated device
  - Open Wireguard on the emulated device, select the "+"-Icon and select `Import from file or archive`
  - Select the dropped file (it should be in the downloads folder on the device)

Connect the newly created wireguard connect. Android might ask your for permission; simply allow everything.

## Install root certificates

This is done via a Magisk module ([see also](https://docs.mitmproxy.org/stable/howto-install-system-trusted-ca-android/#instructions-when-using-magisk)).
While connected via wireguard:
  - Open Chrome on the emulated android device and navigate to `http://mitm.it/cert/magisk`. This will only work while your wireguard connection is healthy.
  - Download the offered file
  - Close the browser and open the Magisk app
  - In the Magisk app open `Modules` in the lower right and select `Install from storage` select the mitmproxy module, confirm and reboot AVD.
  - Verify by opening Chrome and navigate to any website that uses HTTPS. If you don't get a security warning, then the setup was successful

### Troubleshooting Magisk & CA certificates
If Magisk isn't properly installed (you're unable to access modules) do the following: 
 - Disconnect Wireguard
 - Update Magisk App (within Magisk app, confirm everyting, this might not work while wireguard is connected)
 - Select Install via "Direct Install"
 - Reboot AVD
 - Reopen the Magisk app. It might ask you to update, which you can do but for me it kept asking.

If it seems you can't get the certificate installed:
 - Get yourself an older virtual device. e.g. API Level 30 or 31.
 - Play around with the Magisk-App, Update the app and then reinstall Magisk from within the app
 - Manually install the certificate:
   - While  connected via wireguard
   - Use Chrome to navigate to `http://mitm.it`
   - Download the certificate next to the android icon
   - Open Adroid Settings and search for `certificate`, and select `CA certificate` and select `CA certificate` as the certificate to install.
   - Proceed and select the downloaded certificate

## The actual sniffing

Start Wireshark.
In `Settings` -> `Protocols` -> `TLS`, select the file created above as `(Pre)-Master-Secret log filename`.
Start to capture on your default network interface (the one you're connected to the internet with).

Start the Roborock app on the android device.

If you're interested in the mqtt protocol you can apply a filter as:

    mqtt.msgtype == 3

If you're interested in the Roborock REST api, I recommend to filter for

    http or http2

If there is still too much going on, add more filters with ip-addresses.

## Analyzing the MQTT traffic

If you've come this far you will probably have noticed that you cannot actually read the mqtt traffic in wireshark.
This is because the protocol is heavily obfuscated and encrypted.
To have a look into the mqtt traffic the service as a "secret" mode to decode mqtt traffic.
To use it follow these instructions:

1. In Wireshark, with the filter `mqtt.msgtype == 3` active, select

   `File` → `Export Packet Dissections` → `As JSON`

   Now save only `Displayed` packets to a file.

2. Use this file as input for

   ```shell
   java -jar app.jar --mode=reduce --input=captures/capture.json --output=captures/capture.csv
   ```

   This creates a csv file from the JSON file that contains a lot of information we don't need.

3. Use the CSV file as input for decryption, which is called like this:

   ```shell
   java -jar app.jar --mode=decode --input=captures/simple.csv --device=deviceId:deviceKey
   ```
