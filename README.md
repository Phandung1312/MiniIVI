# Install CarSystemUI and CarLauncher

## Prerequisites

- Remove the original `CarSystemUI` and `CarLauncher` apps from the device.
- Make sure the device is remounted and the `/system` and `/system_ext` partitions are writable.

```powershell
adb root
adb remount
```

## Signing key

This project requires the platform signing key that matches the target device image. The keystore is not included in Git.

Obtain the keystore and its credentials from the ROM/project owner. Configure `signing.properties` and place the keystore under `keys/` in both module directories:

- `CarSystemUI/signing.properties` and `CarSystemUI/keys/`
- `CarLauncher/signing.properties` and `CarLauncher/keys/`

Use [CarSystemUI/signing.properties.example](CarSystemUI/signing.properties.example) as the property-file format reference.

## Build

```powershell
cd CarSystemUI
.\gradlew.bat :app:assembleDebug

cd ..\CarLauncher
.\gradlew.bat :app:assembleDebug
```

## Install

Run these commands from the `MiniIVI` root directory:

```powershell
# CarSystemUI
adb push .\CarSystemUI\app\build\outputs\apk\debug\app-debug.apk /system_ext/priv-app/CarSystemUI.apk
adb shell "chmod 644 /system_ext/priv-app/CarSystemUI.apk"
adb push .\CarSystemUI\system_ext\etc\permissions\privapp-permissions-com.android.car.systemui.xml /system_ext/etc/permissions/privapp-permissions-com.android.car.systemui.xml

# CarLauncher
adb push .\CarLauncher\app\build\outputs\apk\debug\app-debug.apk /system/priv-app/CarLauncher.apk
adb shell "chmod 644 /system/priv-app/CarLauncher.apk"
adb push .\CarLauncher\system\etc\permissions\privapp-permissions-com.android.car.launcher.xml /system/etc/permissions/privapp-permissions-com.android.car.launcher.xml

adb shell "sync"
adb reboot
```
