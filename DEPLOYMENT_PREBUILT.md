# Deploy the Prebuilt Bundle

## Prerequisites

Use an AOSP/AAOS `userdebug` or `eng` emulator image without Google services,
Google Play Services, Google APIs, or the Play Store. Google-enabled images may
not allow `adb root`/`adb remount`, which is required for system-app deployment.

Make sure `emulator` and `adb` are available. List the installed AVDs:

```bat
emulator -list-avds
```

Start the target AVD with a writable system image and snapshots disabled:

```bat
emulator -avd <AVD_NAME> -writable-system -no-snapshot
```

On the first boot of a new AVD, wait for Android to finish booting and perform
the writable-system setup before running the deployment script:

```bat
adb wait-for-device
adb shell getprop sys.boot_completed
adb root
adb wait-for-device
adb remount
adb shell getprop sys.boot_completed
```

Continue only when `sys.boot_completed` is `1` and `adb remount` succeeds. If
the emulator reboots during `adb root` or `adb remount`, wait for
`sys.boot_completed=1` again. Keep the prepared emulator open. In a second
terminal, run from the release root:

```bat
deploy_prebuilt.bat
```

The script validates `demo/`, migrates a legacy Map installation when needed, pushes every system artifact and both sample songs, reboots Android, and verifies the deployed packages. Never use `adb install`.
