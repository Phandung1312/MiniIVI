# MiniIVI deployment

Build or copy the three platform-signed APKs and five permission XML files into `demo/`.

For an emulator, use a writable image:

```powershell
emulator -avd <AVD_NAME> -writable-system -no-snapshot
```

Edit the environment paths at the top of `deploy_prebuilt.bat`, then run:

```bat
deploy_prebuilt.bat
```

The script validates, pushes, replaces, sets permissions, syncs, reboots, and verifies all system apps. Do not use `adb install`.
