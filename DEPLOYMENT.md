# Build and Deploy MiniIVI

This guide builds, pushes, and verifies every MiniIVI APK on the `DreamCar` Android Automotive emulator. All applications in this repository are system applications. Never use `adb install`; copy every APK and its policy files to the correct writable system partition.

## 1. Prerequisites

- Android SDK Platform Tools and Emulator, with `adb` and `emulator` available.
- JDK 17.
- The `DreamCar` AVD or another `userdebug`/`eng` AAOS image that supports `adb root` and `adb remount`.
- Platform signing properties and keys for the target image.

Configure `CarSystemUI/signing.properties` and `CarLauncher/signing.properties` using `CarSystemUI/signing.properties.example`. CarService reuses the CarSystemUI configuration. MiniIviMaps reuses the CarLauncher configuration because its preview service is protected by a signature permission.

Run repository-relative commands from the MiniIVI root. Build projects sequentially because they share source modules.

Start the emulator with a writable system image and wait for Android:

```powershell
emulator -avd DreamCar -writable-system -no-snapshot
adb wait-for-device
adb shell getprop sys.boot_completed
```

Continue only when `sys.boot_completed` returns `1`.

## 2. MiniIVI Car Service

Role: privileged backend for vehicle state, HVAC, audio, and Bluetooth control.

Build and test:

```powershell
Push-Location .\CarService
.\gradlew.bat :app:testDebugUnitTest :car-service-client:testDebugUnitTest :app:assembleDebug --console=plain --no-daemon
Pop-Location
```

Push the APK and its permission policies:

```powershell
adb root
adb wait-for-device
adb remount
adb shell "mkdir -p /system_ext/priv-app/MiniIVICarService /system_ext/etc/permissions /system_ext/etc/default-permissions"
adb push .\CarService\app\build\outputs\apk\debug\app-debug.apk /system_ext/priv-app/MiniIVICarService/MiniIVICarService.apk
adb push .\CarService\system_ext\etc\permissions\privapp-permissions-com.miniivi.car.service.xml /system_ext/etc/permissions/privapp-permissions-com.miniivi.car.service.xml
adb push .\CarService\system_ext\etc\default-permissions\default-permissions-com.miniivi.car.service.xml /system_ext/etc/default-permissions/default-permissions-com.miniivi.car.service.xml
adb shell "chmod 0644 /system_ext/priv-app/MiniIVICarService/MiniIVICarService.apk /system_ext/etc/permissions/privapp-permissions-com.miniivi.car.service.xml /system_ext/etc/default-permissions/default-permissions-com.miniivi.car.service.xml"
```

After the final reboot, verify the package, system UID, privileged permissions, and clients:

```powershell
adb shell pm path com.miniivi.car.service
adb shell "dumpsys package com.miniivi.car.service | grep -E 'sharedUser=|BLUETOOTH_CONNECT: granted|BLUETOOTH_SCAN: granted|BLUETOOTH_PRIVILEGED: granted|LOCAL_MAC_ADDRESS: granted'"
adb shell "dumpsys activity services com.miniivi.car.service | grep -E 'ServiceRecord|Client AppBindRecord|baseDir='"
```

## 3. CarSystemUI and Boot Artifacts

Role: system navigation, overlays, status surfaces, control center, boot animation, and boot-progress branding.

Build and test:

```powershell
Push-Location .\CarSystemUI
.\gradlew.bat :boot-brand:test :boot-animation:check :boot-animation:assemble :boot-progress-overlay:check :app:testDebugUnitTest :app:compileDebugKotlin :app:assembleDebug --console=plain --no-daemon
Pop-Location
```

Push the application and allowlist:

```powershell
adb shell "mkdir -p /system_ext/priv-app/CarSystemUI /system_ext/etc/permissions"
adb push .\CarSystemUI\app\build\outputs\apk\debug\app-debug.apk /system_ext/priv-app/CarSystemUI/CarSystemUI.apk
adb push .\CarSystemUI\system_ext\etc\permissions\privapp-permissions-com.android.car.systemui.xml /system_ext/etc/permissions/privapp-permissions-com.android.car.systemui.xml
adb shell "chmod 0644 /system_ext/priv-app/CarSystemUI/CarSystemUI.apk /system_ext/etc/permissions/privapp-permissions-com.android.car.systemui.xml"
```

Keep only the directory-based SystemUI APK:

```powershell
adb shell "if [ -f /system_ext/priv-app/CarSystemUI/CarSystemUI.apk ] && [ -e /system_ext/priv-app/CarSystemUI.apk ]; then rm -f /system_ext/priv-app/CarSystemUI.apk; fi"
```

Push both boot animations and the framework overlay:

```powershell
adb shell "mkdir -p /product/media /product/overlay"
adb push .\CarSystemUI\boot-animation\build\outputs\bootanimation\bootanimation.zip /product/media/bootanimation.zip
adb push .\CarSystemUI\boot-animation\build\outputs\bootanimation\bootanimation-dark.zip /product/media/bootanimation-dark.zip
adb push .\CarSystemUI\boot-progress-overlay\build\outputs\boot-progress-overlay\MiniIviBootProgressOverlay.apk /product/overlay/MiniIviBootProgressOverlay.apk
adb shell "chmod 0644 /product/media/bootanimation.zip /product/media/bootanimation-dark.zip /product/overlay/MiniIviBootProgressOverlay.apk"
```

After reboot, verify the package, overlay, navigation process, boot animation, brightness, volume, and HVAC controls. The boot receiver starts `CarSystemUIService`; the Hilt `CarSystemUIApplication` initializes the feature components before the service callback. Do not use `adb install`; deploy the APK to the system partition as shown above.

```powershell
adb shell pm path com.android.car.systemui
adb shell "cmd overlay list android | grep com.miniivi.bootprogress.overlay"
adb shell "ps -A | grep com.android.car.systemui"
adb logcat -d -s BootAnimation:*
```

## 4. MiniIVI Maps

Role: regular system map application with a signature-protected preview service used by CarLauncher.

Build and test:

```powershell
Push-Location .\MiniIviMaps
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain --no-daemon
Pop-Location
```

MiniIviMaps shares the system UID. When upgrading an existing Map installation that used
its own UID, remove its per-user installations before replacing the system APK. This
one-time migration clears Map data for those users:

```powershell
adb shell "pm uninstall --user 0 com.miniivi.maps"
adb shell "pm uninstall --user 10 com.miniivi.maps"
```

Push the APK and fixed default location permissions:

```powershell
adb shell "mkdir -p /system/priv-app/MiniIviMaps /system/etc/default-permissions"
adb push .\MiniIviMaps\app\build\outputs\apk\debug\app-debug.apk /system/priv-app/MiniIviMaps/MiniIviMaps.apk
adb push .\MiniIviMaps\system\etc\default-permissions\default-permissions-com.miniivi.maps.xml /system/etc/default-permissions/default-permissions-com.miniivi.maps.xml
adb shell "rm -rf /system/app/MiniIviMaps"
adb shell "chmod 0644 /system/priv-app/MiniIviMaps/MiniIviMaps.apk /system/etc/default-permissions/default-permissions-com.miniivi.maps.xml"
adb reboot
adb wait-for-device
adb shell getprop sys.boot_completed
adb shell "pm install-existing --user 0 com.miniivi.maps"
adb shell "pm install-existing --user 10 com.miniivi.maps"
```

After boot completes, verify the package path, system UID, and fixed location grants for
both users before checking the full-screen map and launcher preview:

```powershell
adb shell pm path com.miniivi.maps
adb shell "cmd package list packages -U com.miniivi.maps"
adb shell "dumpsys package com.miniivi.maps | grep -E 'User (0|10):|ACCESS_FINE_LOCATION: granted=true|ACCESS_COARSE_LOCATION: granted=true|ACCESS_BACKGROUND_LOCATION: granted=true'"
```

## 5. CarLauncher

Role: AAOS home application, dashboard, application entry points, local media player, and MiniIviMaps preview host.

Build and test after MiniIviMaps so the shared preview contract has already been verified:

```powershell
Push-Location .\CarLauncher
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain --no-daemon
Pop-Location
```

Push the APK and its permission policies:

```powershell
adb shell "mkdir -p /system/priv-app /system/etc/permissions /system/etc/default-permissions"
adb push .\CarLauncher\app\build\outputs\apk\debug\app-debug.apk /system/priv-app/CarLauncher.apk
adb push .\CarLauncher\system\etc\permissions\privapp-permissions-com.android.car.launcher.xml /system/etc/permissions/privapp-permissions-com.android.car.launcher.xml
adb push .\CarLauncher\system\etc\default-permissions\default-permissions-com.android.car.launcher.xml /system/etc/default-permissions/default-permissions-com.android.car.launcher.xml
adb shell "chmod 0644 /system/priv-app/CarLauncher.apk /system/etc/permissions/privapp-permissions-com.android.car.launcher.xml /system/etc/default-permissions/default-permissions-com.android.car.launcher.xml"
```

After reboot, verify that it is the active home application, has media permission, displays the Maps preview, and can load and play audio from MediaStore:

```powershell
adb shell pm path com.android.car.launcher
adb shell "dumpsys package com.android.car.launcher | grep -E 'READ_MEDIA_AUDIO: granted|READ_EXTERNAL_STORAGE: granted'"
adb shell "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME"
```

## 6. Reboot and Complete Verification

System permission policies are loaded during boot, so reboot after all artifacts have been pushed:

```powershell
adb shell sync
adb reboot
adb wait-for-device
```

Wait until `adb shell getprop sys.boot_completed` returns `1`, then verify every deployed file:

```powershell
adb shell "ls -l \
  /system_ext/priv-app/MiniIVICarService/MiniIVICarService.apk \
  /system_ext/priv-app/CarSystemUI/CarSystemUI.apk \
  /system/priv-app/CarLauncher.apk \
  /system/priv-app/MiniIviMaps/MiniIviMaps.apk \
  /product/overlay/MiniIviBootProgressOverlay.apk \
  /product/media/bootanimation.zip \
  /product/media/bootanimation-dark.zip"
```

Compare local and device SHA-256 hashes for all APKs and boot files. Exercise Home, Apps, Media, Maps, Bluetooth, brightness, volume, and HVAC, then check for runtime failures:

```powershell
adb logcat -d -v brief | Select-String -CaseSensitive -Pattern `
    'FATAL EXCEPTION', `
    'SecurityException', `
    'Process: com.miniivi', `
    'Process: com.android.car.systemui', `
    'Process: com.android.car.launcher'
```

The deployment passes when all package paths and hashes match, the expected permissions are granted, CarService shows both clients, Maps works in full-screen and preview modes, media playback works, and the log contains no MiniIVI crash or permission denial.

## 7. Runtime Flow Logging

MiniIVI runtime logs use stable tags beginning with `MiniIvi` and structured English messages in the form `event=<name> key=value`. `INFO` records lifecycle, connection, backend, capability, and recovery transitions. `WARN` and `ERROR` records describe degraded or failed operations. High-volume flow details use `DEBUG` and are guarded with `Log.isLoggable`.

Follow all MiniIVI processes in timestamp order:

```powershell
adb logcat -v threadtime | Select-String -CaseSensitive -Pattern 'MiniIvi'
```

Enable `DEBUG` only for the components needed by the current trace. The common tags are:

```powershell
$miniIviDebugTags = @(
    'MiniIviCarService',
    'MiniIviCarClient',
    'MiniIviCarConnection',
    'MiniIviCarHvac',
    'MiniIviCarAudio',
    'MiniIviBrightness',
    'MiniIviVehicleStatus',
    'MiniIviQuickControls',
    'MiniIviBluetooth',
    'MiniIviSystemUi',
    'MiniIviNavigation',
    'MiniIviLauncher',
    'MiniIviLauncherLife',
    'MiniIviMedia',
    'MiniIviMapPreview',
    'MiniIviMaps',
    'MiniIviLocation'
)
$miniIviDebugTags | ForEach-Object { adb shell setprop "log.tag.$_" DEBUG }
```

Restore the normal production threshold after tracing:

```powershell
$miniIviDebugTags | ForEach-Object { adb shell setprop "log.tag.$_" INFO }
```

The logs intentionally omit location coordinates, Bluetooth addresses and names, media titles and URIs, Binder tokens, complete state objects, polling ticks, property callback samples, and intermediate brightness or volume slider writes.
