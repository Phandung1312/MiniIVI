# Deploy the Prebuilt Bundle

Make sure `emulator` and `adb` are available. List the installed AVDs:

```bat
emulator -list-avds
```

Start the target AVD with a writable system image and snapshots disabled:

```bat
emulator -avd <AVD_NAME> -writable-system -no-snapshot
```

Keep the emulator open. In a second terminal, run from the release root:

```bat
deploy_prebuilt.bat
```

The script validates `demo/`, pushes every system artifact and both sample songs, reboots Android, and verifies the deployed packages. Never use `adb install`.
