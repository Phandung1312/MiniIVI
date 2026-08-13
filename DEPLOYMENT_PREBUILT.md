# Deploy the Prebuilt Bundle

Start `DreamCar` with `-writable-system -no-snapshot` and make sure `adb` is available. If needed, edit the tool, serial, or emulator values at the top of `deploy_prebuilt.bat`.

Run from the release root:

```bat
deploy_prebuilt.bat
```

The script validates `demo/`, pushes every system artifact and both sample songs, reboots Android, and verifies the deployed packages. Never use `adb install`.
