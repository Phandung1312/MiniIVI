# Deploy a Prebuilt MiniIVI Bundle to an AAOS Emulator

This procedure deploys MiniIVI when the four signed APKs, five permission XML files, and two boot animation archives have already been prepared. It does not require the application source trees or a Gradle build.

Use [DEPLOYMENT.md](DEPLOYMENT.md) when the applications must be built from source.

All MiniIVI applications are privileged system applications. **Never use `adb install`.** The APKs, permission files, and boot animation archives must be copied to the writable system partitions and the emulator must be rebooted.

## 1. Required Bundle Layout

Extract the prebuilt package into a directory named `MiniIVI-prebuilt`. Its contents must match this layout:

```text
MiniIVI-prebuilt/
|-- product/
|   |-- media/
|   |   |-- bootanimation.zip
|   |   `-- bootanimation-dark.zip
|   `-- overlay/
|       `-- MiniIviBootProgressOverlay.apk
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

The bundle must contain exactly four APKs, five XML files, and two boot animation ZIP files. All four APKs must be built from a compatible source revision and signed with the platform certificate used by the target AAOS image. The three executable packages declare `android.uid.system`; `MiniIviBootProgressOverlay.apk` is a code-free RRO targeting `android`.

Open PowerShell in the directory that contains `MiniIVI-prebuilt`, then validate the bundle before connecting to the emulator:

```powershell
$bundleRoot = (Resolve-Path -LiteralPath '.\MiniIVI-prebuilt').Path

$bundleFiles = @(
    'product\media\bootanimation.zip'
    'product\media\bootanimation-dark.zip'
    'product\overlay\MiniIviBootProgressOverlay.apk'
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

Confirm that `/system`, `/system_ext`, and `/product` are writable overlay mounts:

```powershell
adb shell "mount | grep -E ' /system | /system_ext | /product '"
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
adb shell "mkdir -p /product/media"
adb shell "mkdir -p /product/overlay"
```

Push the three executable APKs:

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

Back up the original product boot animations, then push both MiniIVI variants:

```powershell
$bootAnimationBackup = Join-Path $env:TEMP ('MiniIVI-bootanimation-backup-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $bootAnimationBackup | Out-Null
adb pull /product/media/bootanimation.zip (Join-Path $bootAnimationBackup 'bootanimation.zip')
adb pull /product/media/bootanimation-dark.zip (Join-Path $bootAnimationBackup 'bootanimation-dark.zip')
adb push (Join-Path $bundleRoot 'product\media\bootanimation.zip') /product/media/bootanimation.zip
adb push (Join-Path $bundleRoot 'product\media\bootanimation-dark.zip') /product/media/bootanimation-dark.zip
```

Back up an existing MiniIVI boot-progress RRO, if present, and push the bundle RRO:

```powershell
$bootOverlayBackup = Join-Path $bootAnimationBackup 'MiniIviBootProgressOverlay.apk'
adb shell "if [ -f /product/overlay/MiniIviBootProgressOverlay.apk ]; then cp /product/overlay/MiniIviBootProgressOverlay.apk /data/local/tmp/MiniIviBootProgressOverlay.apk; fi"
adb pull /data/local/tmp/MiniIviBootProgressOverlay.apk $bootOverlayBackup
adb shell "rm -f /data/local/tmp/MiniIviBootProgressOverlay.apk"
adb push (Join-Path $bundleRoot 'product\overlay\MiniIviBootProgressOverlay.apk') /product/overlay/MiniIviBootProgressOverlay.apk
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
adb shell "chmod 0644 /product/media/bootanimation.zip"
adb shell "chmod 0644 /product/media/bootanimation-dark.zip"
adb shell "chmod 0644 /product/overlay/MiniIviBootProgressOverlay.apk"
```

CarSystemUI must exist only at `/system_ext/priv-app/CarSystemUI/CarSystemUI.apk`. Check for a stale flat duplicate:

```powershell
adb shell "if [ -e /system_ext/priv-app/CarSystemUI.apk ]; then echo 'ERROR: stale duplicate /system_ext/priv-app/CarSystemUI.apk exists'; exit 1; fi"
```

If the check reports a duplicate, first confirm that the directory-based APK exists, then remove only the stale flat file:

```powershell
adb shell "test -f /system_ext/priv-app/CarSystemUI/CarSystemUI.apk && rm -f /system_ext/priv-app/CarSystemUI.apk"
```

Verify all eleven deployed files:

```powershell
adb shell "ls -l \
  /system_ext/priv-app/MiniIVICarService/MiniIVICarService.apk \
  /system_ext/priv-app/CarSystemUI/CarSystemUI.apk \
  /system/priv-app/CarLauncher.apk \
  /system_ext/etc/permissions/privapp-permissions-com.miniivi.car.service.xml \
  /system_ext/etc/default-permissions/default-permissions-com.miniivi.car.service.xml \
  /system_ext/etc/permissions/privapp-permissions-com.android.car.systemui.xml \
  /system/etc/permissions/privapp-permissions-com.android.car.launcher.xml \
  /system/etc/default-permissions/default-permissions-com.android.car.launcher.xml \
  /product/media/bootanimation.zip \
  /product/media/bootanimation-dark.zip \
  /product/overlay/MiniIviBootProgressOverlay.apk"
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
Get-FileHash -Algorithm SHA256 (Join-Path $bundleRoot 'product\overlay\MiniIviBootProgressOverlay.apk')

adb shell "sha256sum \
  /system_ext/priv-app/MiniIVICarService/MiniIVICarService.apk \
  /system_ext/priv-app/CarSystemUI/CarSystemUI.apk \
  /system/priv-app/CarLauncher.apk \
  /product/overlay/MiniIviBootProgressOverlay.apk"
```

The hashes must match in CarService, CarSystemUI, CarLauncher, and boot-progress RRO order.

Compare the local and device boot animation hashes and confirm the selected product archive:

```powershell
Get-FileHash -Algorithm SHA256 (Join-Path $bundleRoot 'product\media\bootanimation.zip')
Get-FileHash -Algorithm SHA256 (Join-Path $bundleRoot 'product\media\bootanimation-dark.zip')
adb shell "sha256sum /product/media/bootanimation.zip /product/media/bootanimation-dark.zip"
adb logcat -d -s BootAnimation:* | Select-String -Pattern '/product/media/bootanimation'
adb shell "cmd overlay list android | grep com.miniivi.bootprogress.overlay"
adb logcat -d -s MiniIviBootHandoff:* | Select-String -Pattern 'visible|first frame|fail-safe'
```

The selected archive must load successfully, show the IVI intro once, and loop the horizontal shimmer without a circular loader. The RRO must be enabled, no `Android is starting…` title may appear, and the CarSystemUI handoff must remain visible until the Launcher first-frame signal removes it.

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
- If the boot animation is rejected or Android displays the previous animation, verify that both product ZIP files are mode `0644`, contain only stored entries, and were pushed after a successful `/product` remount.
- To restore the original boot animation, push both ZIP files from `$bootAnimationBackup` back to `/product/media`, set mode `0644`, sync, and reboot.
- To restore the boot-progress overlay, push `$bootOverlayBackup` back when it exists; otherwise remove only `/product/overlay/MiniIviBootProgressOverlay.apk`, sync, and reboot.
- If Package Manager reports duplicate CarSystemUI packages, keep `/system_ext/priv-app/CarSystemUI/CarSystemUI.apk` and remove only `/system_ext/priv-app/CarSystemUI.apk`.
- If Bluetooth runtime permissions are missing for an existing AAOS user, inspect `cmd user list` and the CarService package state. The complete recovery procedure is documented in [DEPLOYMENT.md](DEPLOYMENT.md#bluetooth-permissions-are-declared-but-unavailable).
