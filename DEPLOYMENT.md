# Deploy MiniIVI to an AAOS Emulator

This document is the authoritative procedure for building and deploying the complete MiniIVI stack to a writable Android Automotive OS emulator:

- `CarService`
- `CarSystemUI`
- `CarLauncher`

All three applications are privileged system applications. Deploy them by copying their artifacts to the appropriate system partition. **Never use `adb install` for any application in this repository.**

If the four signed APKs, five permission XML files, and two boot animation archives have already been packaged, use [DEPLOYMENT_PREBUILT.md](DEPLOYMENT_PREBUILT.md) instead. That procedure skips the source build and deploys directly from a self-contained prebuilt bundle.

## 1. Prerequisites

Install or prepare the following on the Windows host:

- Android SDK Platform Tools, including `adb`.
- Android Emulator and an AAOS AVD. The examples use the `DreamCar` AVD.
- JDK 17.
- A target image with `userdebug` or `eng` behavior that supports `adb root` and `adb remount`.
- The platform signing certificate that matches the emulator system image.

Run every repository-relative command from the `MiniIVI` root directory unless a section explicitly changes directory.

### Platform signing configuration

The APKs must use the same platform certificate as the AAOS image because all three applications currently use `android.uid.system` or interact with packages that use it.

Configure these files and their referenced keystores before building:

- `CarSystemUI/signing.properties`
- `CarLauncher/signing.properties`

`CarService` deliberately reuses `CarSystemUI/signing.properties`. Its build fails when that platform signing configuration is missing or incomplete.

Use `CarSystemUI/signing.properties.example` as the property format reference. Do not commit keystores, passwords, or generated signing files.

## 2. Start a Writable Emulator

Close any running instance of the target AVD before starting it in writable mode. First locate the emulator executable and list the available AVDs:

```powershell
$androidSdk = if ($env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT
} else {
    Join-Path $env:LOCALAPPDATA 'Android\Sdk'
}
$emulator = Join-Path $androidSdk 'emulator\emulator.exe'

& $emulator -list-avds
```

Start the AAOS emulator with a writable system image and without snapshot loading or saving:

```powershell
& $emulator -avd DreamCar -writable-system -no-snapshot
```

Keep this emulator process open. `-writable-system` creates a temporary writable copy of the system image. The copy can consume several hundred megabytes and is destroyed when the emulator exits. The MiniIVI system files must therefore be deployed again after starting a new writable emulator session.

In a second PowerShell terminal, wait for Android to boot:

```powershell
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
adb shell getprop ro.product.name
adb shell getprop ro.build.type
adb shell getprop ro.debuggable
```

Expected results include one device in the `device` state, a `userdebug` or `eng` build, and `ro.debuggable=1`.

## 3. Build and Test the Applications

Build the projects sequentially. `CarSystemUI` and `CarLauncher` include the CarService API/client modules, so running their Gradle builds concurrently with the CarService build can race while generating AIDL sources.

Verify that `JAVA_HOME` points to a JDK 17 installation for the current PowerShell session:

```powershell
if (-not $env:JAVA_HOME) {
    throw 'Set JAVA_HOME to a JDK 17 installation before building.'
}

$java = Join-Path $env:JAVA_HOME 'bin\java.exe'
& $java -version
```

If the reported major version is not 17, update `JAVA_HOME` using the JDK location on the current host and run the check again.

Build and test CarService:

```powershell
Push-Location .\CarService
.\gradlew.bat :app:testDebugUnitTest :car-service-client:testDebugUnitTest :app:assembleDebug --console=plain --no-daemon
Pop-Location
```

Build and test CarSystemUI:

```powershell
Push-Location .\CarSystemUI
.\gradlew.bat :boot-brand:test :boot-animation:check :boot-animation:assemble :boot-progress-overlay:check :app:testDebugUnitTest :app:assembleDebug --console=plain --no-daemon
Pop-Location
```

Build and test CarLauncher:

```powershell
Push-Location .\CarLauncher
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain --no-daemon
Pop-Location
```

Confirm that all APKs and permission files exist before modifying the emulator:

```powershell
$deploymentFiles = @(
    '.\CarService\app\build\outputs\apk\debug\app-debug.apk'
    '.\CarService\system_ext\etc\permissions\privapp-permissions-com.miniivi.car.service.xml'
    '.\CarService\system_ext\etc\default-permissions\default-permissions-com.miniivi.car.service.xml'
    '.\CarSystemUI\app\build\outputs\apk\debug\app-debug.apk'
    '.\CarSystemUI\system_ext\etc\permissions\privapp-permissions-com.android.car.systemui.xml'
    '.\CarSystemUI\boot-animation\build\outputs\bootanimation\bootanimation.zip'
    '.\CarSystemUI\boot-animation\build\outputs\bootanimation\bootanimation-dark.zip'
    '.\CarSystemUI\boot-progress-overlay\build\outputs\boot-progress-overlay\MiniIviBootProgressOverlay.apk'
    '.\CarLauncher\app\build\outputs\apk\debug\app-debug.apk'
    '.\CarLauncher\system\etc\permissions\privapp-permissions-com.android.car.launcher.xml'
    '.\CarLauncher\system\etc\default-permissions\default-permissions-com.android.car.launcher.xml'
)

$missingFiles = $deploymentFiles | Where-Object { -not (Test-Path -LiteralPath $_) }
if ($missingFiles) {
    throw "Missing deployment files:`n$($missingFiles -join "`n")"
}

$deploymentFiles | Get-Item | Select-Object FullName, Length, LastWriteTime
```

The complete deployment contains four APKs, five permission XML files, and two boot animation ZIP files.

## 4. Prepare the System Partitions

Restart ADB as root and remount the writable partitions:

```powershell
adb root
adb wait-for-device
adb remount
```

`adb remount` must report success for `/system`, `/system_ext`, and `/product`. Verify the active mounts:

```powershell
adb shell "mount | grep -E ' /system | /system_ext | /product '"
```

On a writable emulator, the output normally includes overlay mount entries for all three partitions. The successful `adb remount` result and a subsequent test push are the authoritative checks that the overlay accepts writes.

Inspect the package locations currently known to Package Manager:

```powershell
adb shell pm path com.miniivi.car.service
adb shell pm path com.android.car.systemui
adb shell pm path com.android.car.launcher
```

The expected MiniIVI locations are:

```text
package:/system_ext/priv-app/MiniIVICarService/MiniIVICarService.apk
package:/system_ext/priv-app/CarSystemUI/CarSystemUI.apk
package:/system/priv-app/CarLauncher.apk
```

If the target image uses different partitions or package directories, stop and reconcile the image layout before pushing files. Privileged-permission allowlists must be placed on the same partition as their APK.

## 5. Push All System Artifacts

Create the application and configuration directories:

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

### CarService

```powershell
adb push .\CarService\app\build\outputs\apk\debug\app-debug.apk /system_ext/priv-app/MiniIVICarService/MiniIVICarService.apk
adb push .\CarService\system_ext\etc\permissions\privapp-permissions-com.miniivi.car.service.xml /system_ext/etc/permissions/privapp-permissions-com.miniivi.car.service.xml
adb push .\CarService\system_ext\etc\default-permissions\default-permissions-com.miniivi.car.service.xml /system_ext/etc/default-permissions/default-permissions-com.miniivi.car.service.xml
```

### CarSystemUI

```powershell
adb push .\CarSystemUI\app\build\outputs\apk\debug\app-debug.apk /system_ext/priv-app/CarSystemUI/CarSystemUI.apk
adb push .\CarSystemUI\system_ext\etc\permissions\privapp-permissions-com.android.car.systemui.xml /system_ext/etc/permissions/privapp-permissions-com.android.car.systemui.xml
```

Always use `/system_ext/priv-app/CarSystemUI/CarSystemUI.apk`. Do not push a second copy to `/system_ext/priv-app/CarSystemUI.apk`; Package Manager treats it as a duplicate package and can keep using the directory-based APK instead.

Before rebooting, check whether a stale flat SystemUI APK exists:

```powershell
adb shell "if [ -e /system_ext/priv-app/CarSystemUI.apk ]; then echo 'ERROR: stale duplicate /system_ext/priv-app/CarSystemUI.apk exists'; exit 1; fi"
```

If this check fails, confirm that the directory-based APK is present and correct, then remove only the stale flat file:

```powershell
adb shell "test -f /system_ext/priv-app/CarSystemUI/CarSystemUI.apk && rm -f /system_ext/priv-app/CarSystemUI.apk"
```

### MiniIVI boot animation

Back up the original product boot animations before replacing them:

```powershell
$bootAnimationBackup = Join-Path $env:TEMP ('MiniIVI-bootanimation-backup-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $bootAnimationBackup | Out-Null
adb pull /product/media/bootanimation.zip (Join-Path $bootAnimationBackup 'bootanimation.zip')
adb pull /product/media/bootanimation-dark.zip (Join-Path $bootAnimationBackup 'bootanimation-dark.zip')
```

Push both MiniIVI variants so the Google animation cannot reappear when the boot theme changes:

```powershell
adb push .\CarSystemUI\boot-animation\build\outputs\bootanimation\bootanimation.zip /product/media/bootanimation.zip
adb push .\CarSystemUI\boot-animation\build\outputs\bootanimation\bootanimation-dark.zip /product/media/bootanimation-dark.zip
```

Back up an existing MiniIVI boot-progress overlay, if present, then push the
code-free RRO. This package replaces the framework boot-progress text while the
CarSystemUI handoff window is starting:

```powershell
$bootOverlayBackup = Join-Path $bootAnimationBackup 'MiniIviBootProgressOverlay.apk'
adb shell "if [ -f /product/overlay/MiniIviBootProgressOverlay.apk ]; then cp /product/overlay/MiniIviBootProgressOverlay.apk /data/local/tmp/MiniIviBootProgressOverlay.apk; fi"
adb pull /data/local/tmp/MiniIviBootProgressOverlay.apk $bootOverlayBackup
adb shell "rm -f /data/local/tmp/MiniIviBootProgressOverlay.apk"
adb push .\CarSystemUI\boot-progress-overlay\build\outputs\boot-progress-overlay\MiniIviBootProgressOverlay.apk /product/overlay/MiniIviBootProgressOverlay.apk
```

### CarLauncher

```powershell
adb push .\CarLauncher\app\build\outputs\apk\debug\app-debug.apk /system/priv-app/CarLauncher.apk
adb push .\CarLauncher\system\etc\permissions\privapp-permissions-com.android.car.launcher.xml /system/etc/permissions/privapp-permissions-com.android.car.launcher.xml
adb push .\CarLauncher\system\etc\default-permissions\default-permissions-com.android.car.launcher.xml /system/etc/default-permissions/default-permissions-com.android.car.launcher.xml
```

Apply read permissions required for system APKs and XML configuration files:

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

Verify all eleven files on the emulator before rebooting:

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

Every file should be owned by `root`, readable by Android, and normally show mode `-rw-r--r--`.

## 6. Sync, Reboot, and Wait for Android

Permission allowlists and default-permission exceptions are read during system startup. A reboot is mandatory after changing APK manifests, AIDL contracts, privileged permissions, or default permissions.

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

Do not interact with the launcher or Control Center until `sys.boot_completed` returns `1`.

## 7. Verify the Deployment

### Package paths and processes

```powershell
adb shell "pm path com.miniivi.car.service; pm path com.android.car.systemui; pm path com.android.car.launcher"
adb shell "ps -A | grep -E 'miniivi.car.service|car.systemui|car.launcher'"
```

CarService and CarSystemUI should run as `system`. CarLauncher normally runs as the foreground AAOS user with the system application ID.

### CarService binding

```powershell
adb shell "dumpsys activity services com.miniivi.car.service | grep -E 'ServiceRecord|Client AppBindRecord|baseDir='"
```

The output should show `CarSystemUI` and `CarLauncher` as clients of `MiniIviCarService` after their features have started.

### Shared UID and permissions

```powershell
adb shell "dumpsys package com.miniivi.car.service | grep -E 'sharedUser=|BLUETOOTH_CONNECT: granted|BLUETOOTH_SCAN: granted|BLUETOOTH_PRIVILEGED: granted|LOCAL_MAC_ADDRESS: granted'"
adb shell "dumpsys package com.android.car.systemui | grep -E 'sharedUser=|granted=true' | head -40"
adb shell "dumpsys package com.android.car.launcher | grep -E 'sharedUser=|READ_MEDIA_AUDIO: granted|READ_EXTERNAL_STORAGE: granted'"
```

CarService currently declares `android:sharedUserId="android.uid.system"`. Bluetooth connect/scan permissions must be granted for the active users, while `BLUETOOTH_PRIVILEGED` and `LOCAL_MAC_ADDRESS` must appear as granted install permissions.

### APK integrity

Compare local SHA-256 values with the files on the emulator:

```powershell
Get-FileHash -Algorithm SHA256 .\CarService\app\build\outputs\apk\debug\app-debug.apk
Get-FileHash -Algorithm SHA256 .\CarSystemUI\app\build\outputs\apk\debug\app-debug.apk
Get-FileHash -Algorithm SHA256 .\CarLauncher\app\build\outputs\apk\debug\app-debug.apk
Get-FileHash -Algorithm SHA256 .\CarSystemUI\boot-progress-overlay\build\outputs\boot-progress-overlay\MiniIviBootProgressOverlay.apk

adb shell "sha256sum \
  /system_ext/priv-app/MiniIVICarService/MiniIVICarService.apk \
  /system_ext/priv-app/CarSystemUI/CarSystemUI.apk \
  /system/priv-app/CarLauncher.apk \
  /product/overlay/MiniIviBootProgressOverlay.apk"
```

The hashes must match in CarService, CarSystemUI, CarLauncher, and boot-progress RRO order.

### Boot animation integrity

Compare the two local boot animation hashes with the deployed product files and confirm which archive was selected during the last boot:

```powershell
Get-FileHash -Algorithm SHA256 .\CarSystemUI\boot-animation\build\outputs\bootanimation\bootanimation.zip
Get-FileHash -Algorithm SHA256 .\CarSystemUI\boot-animation\build\outputs\bootanimation\bootanimation-dark.zip
adb shell "sha256sum /product/media/bootanimation.zip /product/media/bootanimation-dark.zip"
adb logcat -d -s BootAnimation:* | Select-String -Pattern '/product/media/bootanimation'
adb shell "cmd overlay list android | grep com.miniivi.bootprogress.overlay"
adb logcat -d -s MiniIviBootHandoff:* | Select-String -Pattern 'visible|first frame|fail-safe'
```

The selected archive must load successfully, show the IVI intro once, and loop the horizontal shimmer without a circular loader. The RRO must be enabled, no `Android is starting…` title may appear, and the CarSystemUI handoff must remain visible until the Launcher first-frame signal removes it.

### Runtime errors

Clear stale logs, exercise the Launcher, Control Center, HVAC, brightness, audio, and Bluetooth screens, then inspect application errors:

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

The final check should return no MiniIVI crash, permission denial, or controller error.

## 8. Troubleshooting

### `adb root` is rejected

The image is probably a `user` build. Use a matching `userdebug` or `eng` AAOS image. A production `user` image cannot normally be remounted through ADB.

### `adb remount` fails or the partitions remain read-only

Stop the emulator and start it again with `-writable-system -no-snapshot`. Confirm that no Android Studio-launched instance of the same AVD is already running.

If remount requests a verity restart, follow the message reported by ADB, reboot, wait for the device, run `adb root`, and run `adb remount` again.

### Changes disappear after closing the emulator

This is expected for `-writable-system`. The writable system copy is temporary and is deleted when the emulator exits. Restart the AVD with the same flags and repeat the push procedure.

### The boot animation is rejected or must be restored

Verify that `/product` was remounted successfully, both archives use mode `0644`, and `BootAnimation` reports loading a file under `/product/media`. To restore the files captured before deployment:

```powershell
adb push (Join-Path $bootAnimationBackup 'bootanimation.zip') /product/media/bootanimation.zip
adb push (Join-Path $bootAnimationBackup 'bootanimation-dark.zip') /product/media/bootanimation-dark.zip
adb shell "chmod 0644 /product/media/bootanimation.zip /product/media/bootanimation-dark.zip; sync"
adb reboot
```

To restore the boot-progress overlay, push `$bootOverlayBackup` back when it
exists. If no overlay existed before deployment, remove only the MiniIVI RRO,
then sync and reboot:

```powershell
if (Test-Path -LiteralPath $bootOverlayBackup) {
    adb push $bootOverlayBackup /product/overlay/MiniIviBootProgressOverlay.apk
} else {
    adb shell "rm -f /product/overlay/MiniIviBootProgressOverlay.apk"
}
adb shell "chmod 0644 /product/overlay/MiniIviBootProgressOverlay.apk 2>/dev/null; sync"
adb reboot
```

### Package Manager reports a signature or shared UID mismatch

All APKs using `android.uid.system` must be signed with the platform certificate from the target system image. Recheck both `signing.properties` files and the keystore fingerprint.

Do not bypass this error with `adb install`, a debug certificate, or a different shared UID. If an older APK with incompatible signing or UID metadata has already populated the AVD data partition, wipe the AVD data or recreate the AVD, then repeat the full system deployment.

### CarService data directories have an old UID

This can occur when upgrading an emulator that previously ran CarService without `android.uid.system`. Reboot once and inspect Package Manager logs; a compatible system image can reconcile or recreate the package data.

If the package still fails to start because its data directories retain the old owner, use a clean AVD or wipe the AVD data. Do not recursively change ownership of shared Android data directories by hand.

### Bluetooth permissions are declared but unavailable

Confirm that both CarService permission XML files are under `/system_ext/etc`, that they have mode `0644`, and that the emulator was rebooted after copying them.

Inspect permission state for every running AAOS user:

```powershell
adb shell cmd user list
adb shell "dumpsys package com.miniivi.car.service | grep -E 'User [0-9]+:|BLUETOOTH_CONNECT:|BLUETOOTH_SCAN:|LOCAL_MAC_ADDRESS:|BLUETOOTH_PRIVILEGED:'"
```

For a retained development AVD only, if the new default-permission policy was not applied to an existing user, grant the runtime Bluetooth permissions to that specific user and reboot:

```powershell
adb shell pm grant --user 0 com.miniivi.car.service android.permission.BLUETOOTH_CONNECT
adb shell pm grant --user 0 com.miniivi.car.service android.permission.BLUETOOTH_SCAN
adb shell pm grant --user 10 com.miniivi.car.service android.permission.BLUETOOTH_CONNECT
adb shell pm grant --user 10 com.miniivi.car.service android.permission.BLUETOOTH_SCAN
```

Replace user `10` with the foreground AAOS user reported by `adb shell cmd user list`. These grants are a development recovery step, not a substitute for the system permission XML files.

### Bluetooth rejects operations for a non-active user

Verify that CarService is running with the system UID:

```powershell
adb shell "ps -A | grep com.miniivi.car.service"
adb shell "dumpsys package com.miniivi.car.service | grep sharedUser="
```

The process owner should be `system`, and Package Manager should report `android.uid.system`. If not, verify that the latest CarService APK was pushed and rebooted successfully.

### Package Manager reports a duplicate CarSystemUI package

Confirm that only the directory-based APK exists:

```powershell
adb shell "ls -l /system_ext/priv-app/CarSystemUI/CarSystemUI.apk /system_ext/priv-app/CarSystemUI.apk 2>/dev/null"
```

The valid path is `/system_ext/priv-app/CarSystemUI/CarSystemUI.apk`. Remove the flat duplicate only after confirming the valid file is present, then reboot.

### Permission XML is ignored

The XML must be on the same partition as its privileged APK:

- CarService and CarSystemUI APKs and allowlists belong under `/system_ext`.
- CarLauncher APK and permission files belong under `/system`.

Check XML syntax, file mode `0644`, partition placement, and boot logs from `SystemConfig` or `PackageManager`. Android does not reliably reload these files without a reboot.

## 9. Deployment Checklist

- [ ] The emulator was started with `-writable-system -no-snapshot`.
- [ ] The device completed boot and supports `adb root`.
- [ ] All projects were built sequentially with the matching platform key.
- [ ] All unit tests passed.
- [ ] Four APKs, five permission XML files, and two boot animation ZIP files exist locally.
- [ ] `/system`, `/system_ext`, and `/product` were remounted successfully.
- [ ] CarService, CarSystemUI, and CarLauncher were pushed to their exact package paths.
- [ ] All permission and default-permission XML files were pushed to the correct partition.
- [ ] Both MiniIVI boot animation archives were pushed to `/product/media`.
- [ ] The MiniIVI boot-progress RRO was pushed to `/product/overlay` and is enabled.
- [ ] Every pushed APK, XML, and ZIP has mode `0644`.
- [ ] No flat `/system_ext/priv-app/CarSystemUI.apk` duplicate exists.
- [ ] The emulator was synced and rebooted.
- [ ] Package paths, process owners, shared UID, permissions, and Binder clients were verified.
- [ ] Local and device APK hashes match.
- [ ] Local and device boot animation hashes match, and `BootAnimation` selected the product archive.
- [ ] The IVI handoff remains visible until the Launcher first-frame signal, with no Android boot title or circular loader.
- [ ] Runtime smoke tests produced no MiniIVI crash or permission error.
