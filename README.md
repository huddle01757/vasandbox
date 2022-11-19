# vasandbox

# Modifying the USB message -> Intent mapping
- Open the Android project in Android Studio
- Open res/values/usbmessages.xml
- Messages from USB are expected to be UTF8 string seperated by a *single* character delimiter -e.g. "^"
- The mapping array is structured with the first item in the mapping being the USB message followed by the delimiter.  Subsequent items are the strings being the intents for that message. See below for an example.
- You can add as many mappings as you'd like with the requirement that a USB message must have one or more mappings following it.

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="message_delimiter">^</string>

    <string-array name="message_mapping">
        <item>PTT-DOWN^</item>
        <item>com.rallytac.engageandroid.reference.PTT_ON</item>
        <item>com.dillonkane.ice.ptt.press</item>

        <item>PTT-UP^</item>
        <item>com.rallytac.engageandroid.reference.PTT_OFF</item>
        <item>com.dillonkane.ice.ptt.release</item>

        <item>CHANNEL-NEXT^</item>
        <item>com.dillonkane.ice.channel.next</item>

        <item>CHANNEL-PREV^</item>
        <item>com.dillonkane.ice.channel.previous</item>
    </string-array>
</resources>
```

# Installing/upgrading the APK
```shell
$ adb shell pm uninstall com.rallytac.usbservicetest
$ adb install ptthw/android/RtsUsbServiceTest/app/release/rtsusbservicetest-<version_to_install>.apk
```

# Logcat from development machine
```shell
$ adb logcat | grep -i rtsusbservice
```
