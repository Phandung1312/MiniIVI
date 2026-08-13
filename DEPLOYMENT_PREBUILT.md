# Deploy a Prebuilt MiniIVI Bundle to an AAOS Emulator

This procedure deploys MiniIVI when the three signed APKs and five permission XML files have already been prepared. It does not require the application source trees or a Gradle build.

Use [DEPLOYMENT.md](DEPLOYMENT.md) when the applications must be built from source.

All MiniIVI applications are privileged system applications. **Never use `adb install`.** The APKs and permission files must be copied to the writable system partitions and the emulator must be rebooted.

## 1. Required Bundle Layout

Extract the prebuilt package into a directory named `MiniIVI-prebuilt`. Its contents must match this layout:

```text
MiniIVI-prebuilt/
|-- system/
|   |-- priv-app/
|   |   `-- CarLauncher.apk
|   `-- etc/
|       |-- permissions/
|       |   `-- privapp-permissions-com.android.car.launcher.xml
|       `-- default-permissions/
|           `-- default-permissions-com.android.car.launcher.xml
`-- system_ext/
    |-- priv-app/
    |   |-- MiniIVICarService/
    |   |   `-- MiniIVICarService.apk
    |   `-- CarSystemUI/
    |       `-- CarSystemUI.apk
    `-- etc/
        |-- permissions/
        |   |-- privapp-permissions-com.miniivi.car.service.xml
        |   `-- privapp-permissions-com.android.car.systemui.xml
        `-- default-permissions/
            `-- default-permissions-com.miniivi.car.service.xml
```

The bundle must contain exactly three APKs and five XML files. The three APKs must be built from a compatible source revision and signed with the platform certificate used by the target AAOS image. All three packages declare `android.uid.system`.

Open PowerShell in the directory that contains `MiniIVI-prebuilt`, then validate the bundle before connecting to the emulator:

```powershell
$bundleRoot = (Resolve-Path -LiteralPath '.\MiniIVI-prebuilt').Path

$bundleFiles = @(
    'system\priv-app\CarLauncher.apk'
    'system\etc\permissions\privapp-permissions-com.android.car.launcher.xml'
    'system\etc\default-permissions\default-permissions-com.android.car.launcher.xml'
    'system_ext\priv-app\MiniIVICarService\MiniIVICarService.apk'
    'system_ext\priv-app\CarSystemUI\CarSystemUI.apk'
    'system_ext\etc\permissions\privapp-permissions-com.miniivi.car.service.xml'
    'system_ext\etc\permissions\privapp-permissions-com.android.car.systemui.xml'
    'system_ext\etc\default-permissions\default-permissions-com.miniivi.car.service.xml'
)

$missingFiles = $bundleFiles | Where-Object {
    -not (Test-Path -LiteralPath (Join-Path $bundleRoot $_))
}

if ($missingFiles) {
    throw "The prebuilt bundle is incomplete:`n$($missingFiles -join "`n")"
}

$bundleFiles |
    ForEach-Object { Get-Item -LiteralPath (Join-Path $bundleRoot $_) } |
    Select-Object FullName, Length, LastWriteTime
```

Do not continue if any artifact is missing. Do not substitute permission files from another MiniIVI release because the allowlists must match the APK manifests.

## 2. Start the Emulator in Writable Mode

Close any existing instance of the target AVD. Locate the Android Emulator and list the installed AVDs:

```powershell
$androidSdk = if ($env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT
} else {
    Join-Path $env:LOCALAPPDATA 'Android\Sdk'
}
$emulator = Join-Path $androidSdk 'emulator\emulator.exe'

& $emulator -list-avds
```

Start the target AAOS AVD without loading or saving snapshots:

```powershell
& $emulator -avd DreamCar -writable-system -no-snapshot
```

Keep the emulator process open. The writable system overlay exists only for the current emulator session. Closing the emulator discards files pushed to that overlay.

In a second PowerShell terminal, return to the directory containing `MiniIVI-prebuilt`, initialize the bundle path again, and wait for Android:

```powershell
$bundleRoot = (Resolve-Path -LiteralPath '.\MiniIVI-prebuilt').Path

adb wait-for-device

$bootDeadline = (Get-Date).AddMinutes(3)
do {
    $bootCompleted = (adb shell getprop sys.boot_completed).Trim()
    if ($bootCompleted -eq '1') {
        break
    }
    Start-Sleep -Seconds 2
} while ((Get-Date) -lt $bootDeadline)

if ($bootCompleted -ne '1') {
    throw 'The emulator did not complete boot within three minutes.'
}

adb devices -l
adb shell getprop ro.build.type
adb shell getprop ro.debuggable
```

The image must support `adb root` and `adb remount`. This normally requires a `userdebug` or `eng` image with `ro.debuggable=1`.

## 3. Remount the System Partitions

```powershell
adb root
adb wait-for-device
adb remount
```

Confirm that Package Manager currently uses the expected application locations:

```powershell
adb shell pm path com.miniivi.car.service
adb shell pm path com.android.car.systemui
adb shell pm path com.android.car.launcher
```

The expected paths are:

```text
package:/system_ext/priv-app/MiniIVICarService/MiniIVICarService.apk
package:/system_ext/priv-app/CarSystemUI/CarSystemUI.apk
package:/system/priv-app/CarLauncher.apk
```

Stop if the target image uses a different partition layout. A privileged-permission allowlist must reside on the same partition as its APK.

## 4. Push the Complete Bundle

Create every required directory:

```powershell
adb shell "mkdir -p /system_ext/priv-app/MiniIVICarService"
adb shell "mkdir -p /system_ext/priv-app/CarSystemUI"
adb shell "mkdir -p /system_ext/etc/permissions"
adb shell "mkdir -p /system_ext/etc/default-permissions"
adb shell "mkdir -p /system/priv-app"
adb shell "mkdir -p /system/etc/permissions"
adb shell "mkdir -p /system/etc/default-permissions"
```

Push the three APKs:

```powershell
adb push (Join-Path $bundleRoot 'system_ext\priv-app\MiniIVICarService\MiniIVICarService.apk') /system_ext/priv-app/MiniIVICarService/MiniIVICarService.apk
adb push (Join-Path $bundleRoot 'system_ext\priv-app\CarSystemUI\CarSystemUI.apk') /system_ext/priv-app/CarSystemUI/CarSystemUI.apk
adb push (Join-Path $bundleRoot 'system\priv-app\CarLauncher.apk') /system/priv-app/CarLauncher.apk
```

Push the five permission files:

```powershell
adb push (Join-Path $bundleRoot 'system_ext\etc\permissions\privapp-permissions-com.miniivi.car.service.xml') /system_ext/etc/permissions/privapp-permissions-com.miniivi.car.service.xml
adb push (Join-Path $bundleRoot 'system_ext\etc\default-permissions\default-permissions-com.miniivi.car.service.xml') /system_ext/etc/default-permissions/default-permissions-com.miniivi.car.service.xml
adb push (Join-Path $bundleRoot 'system_ext\etc\permissions\privapp-permissions-com.android.car.systemui.xml') /system_ext/etc/permissions/privapp-permissions-com.android.car.systemui.xml
adb push (Join-Path $bundleRoot 'system\etc\permissions\privapp-permissions-com.android.car.launcher.xml') /system/etc/permissions/privapp-permissions-com.android.car.launcher.xml
adb push (Join-Path $bundleRoot 'system\etc\default-permissions\default-permissions-com.android.car.launcher.xml') /system/etc/default-permissions/default-permissions-com.android.car.launcher.xml
```

Apply mode `0644` to every deployed artifact:

```powershell
adb shell "chmod 0644 /system_ext/priv-app/MiniIVICarService/MiniIVICarService.apk"
adb shell "chmod 0644 /system_ext/priv-app/CarSystemUI/CarSystemUI.apk"
adb shell "chmod 0644 /system/priv-app/CarLauncher.apk"
adb shell "chmod 0644 /system_ext/etc/permissions/privapp-permissions-com.miniivi.car.service.xml"
adb shell "chmod 0644 /system_ext/etc/default-permissions/default-permissions-com.miniivi.car.service.xml"
adb shell "chmod 0644 /system_ext/etc/permissions/privapp-permissions-com.android.car.systemui.xml"
adb shell "chmod 0644 /system/etc/permissions/privapp-permissions-com.android.car.launcher.xml"
adb shell "chmod 0644 /system/etc/default-permissions/default-permissions-com.android.car.launcher.xml"
```

CarSystemUI must exist only at `/system_ext/priv-app/CarSystemUI/CarSystemUI.apk`. Check for a stale flat duplicate:

```powershell
adb shell "if [ -e /system_ext/priv-app/CarSystemUI.apk ]; then echo 'ERROR: stale duplicate /system_ext/priv-app/CarSystemUI.apk exists'; exit 1; fi"
```

If the check reports a duplicate, first confirm that the directory-based APK exists, then remove only the stale flat file:

```powershell
adb shell "test -f /system_ext/priv-app/CarSystemUI/CarSystemUI.apk && rm -f /system_ext/priv-app/CarSystemUI.apk"
```

Verify all eight deployed files:

```powershell
adb shell "ls -l \
  /system_ext/priv-app/MiniIVICarService/MiniIVICarService.apk \
  /system_ext/priv-app/CarSystemUI/CarSystemUI.apk \
  /system/priv-app/CarLauncher.apk \
  /system_ext/etc/permissions/privapp-permissions-com.miniivi.car.service.xml \
  /system_ext/etc/default-permissions/default-permissions-com.miniivi.car.service.xml \
  /system_ext/etc/permissions/privapp-permissions-com.android.car.systemui.xml \
  /system/etc/permissions/privapp-permissions-com.android.car.launcher.xml \
  /system/etc/default-permissions/default-permissions-com.android.car.launcher.xml"
```

Every file should normally be owned by `root` and show mode `-rw-r--r--`.

## 5. Reboot and Verify

```powershell
adb shell sync
adb reboot
adb wait-for-device

$bootDeadline = (Get-Date).AddMinutes(3)
do {
    $bootCompleted = (adb shell getprop sys.boot_completed).Trim()
    if ($bootCompleted -eq '1') {
        break
    }
    Start-Sleep -Seconds 2
} while ((Get-Date) -lt $bootDeadline)

if ($bootCompleted -ne '1') {
    throw 'Android did not complete boot after deployment.'
}
```

Verify package paths, processes, CarService binding, shared UID, and permissions:

```powershell
adb shell "pm path com.miniivi.car.service; pm path com.android.car.systemui; pm path com.android.car.launcher"
adb shell "ps -A | grep -E 'miniivi.car.service|car.systemui|car.launcher'"
adb shell "dumpsys activity services com.miniivi.car.service | grep -E 'ServiceRecord|Client AppBindRecord|baseDir='"
adb shell "dumpsys package com.miniivi.car.service | grep -E 'sharedUser=|BLUETOOTH_CONNECT: granted|BLUETOOTH_SCAN: granted|BLUETOOTH_PRIVILEGED: granted|LOCAL_MAC_ADDRESS: granted'"
adb shell "dumpsys package com.android.car.systemui | grep sharedUser="
adb shell "dumpsys package com.android.car.launcher | grep sharedUser="
```

Compare the local and device APK hashes:

```powershell
Get-FileHash -Algorithm SHA256 (Join-Path $bundleRoot 'system_ext\priv-app\MiniIVICarService\MiniIVICarService.apk')
Get-FileHash -Algorithm SHA256 (Join-Path $bundleRoot 'system_ext\priv-app\CarSystemUI\CarSystemUI.apk')
Get-FileHash -Algorithm SHA256 (Join-Path $bundleRoot 'system\priv-app\CarLauncher.apk')

adb shell "sha256sum \
  /system_ext/priv-app/MiniIVICarService/MiniIVICarService.apk \
  /system_ext/priv-app/CarSystemUI/CarSystemUI.apk \
  /system/priv-app/CarLauncher.apk"
```

The hashes must match in CarService, CarSystemUI, and CarLauncher order.

Clear old logs, exercise the Launcher, Control Center, HVAC, brightness, audio, and Bluetooth features, then inspect errors:

```powershell
adb logcat -c
# Exercise the MiniIVI user interface here.
adb logcat -d -v brief | Select-String -CaseSensitive -Pattern `
    'FATAL EXCEPTION', `
    'E/MiniIvi', `
    'SecurityException.*com.miniivi.car.service', `
    'Process: com.android.car.systemui', `
    'Process: com.android.car.launcher'
```

The deployment is valid only when all three package paths are correct, the APK hashes match, required permissions are granted, CarService has bound clients, and the smoke test produces no MiniIVI crash or permission error.

## 6. Common Failures

- If `adb root` is rejected, use a `userdebug` or `eng` AAOS image.
- If `adb remount` fails, restart the AVD with `-writable-system -no-snapshot` and repeat the remount.
- If Package Manager reports a signature or shared UID mismatch, obtain a bundle signed with the platform key for that exact system image. Wipe or recreate the AVD when stale UID or certificate state cannot be migrated safely.
- If permission XML files are ignored, verify their partition, filename, mode `0644`, and reboot the emulator.
- If changes disappear after closing the emulator, restart it in writable mode and deploy the bundle again. Writable overlays are temporary.
- If Package Manager reports duplicate CarSystemUI packages, keep `/system_ext/priv-app/CarSystemUI/CarSystemUI.apk` and remove only `/system_ext/priv-app/CarSystemUI.apk`.
- If Bluetooth runtime permissions are missing for an existing AAOS user, inspect `cmd user list` and the CarService package state. The complete recovery procedure is documented in [DEPLOYMENT.md](DEPLOYMENT.md#bluetooth-permissions-are-declared-but-unavailable).
