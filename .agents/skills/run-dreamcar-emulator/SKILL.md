---
name: run-dreamcar-emulator
description: Start and verify the MiniIVI project's DreamCar Android Virtual Device with writable system access and snapshots disabled. Use when Codex needs to launch, boot, restart, or prepare the DreamCar emulator before pushing or smoke-testing CarSystemUI, CarLauncher, CarService, or another MiniIVI system app.
---

# Run DreamCar Emulator

Start the project emulator with the required AVD and system-image options.

## Launch

1. Check whether the `DreamCar` AVD exists:

   ```powershell
   emulator -list-avds
   ```

2. Launch it with this exact emulator invocation:

   ```powershell
   emulator -avd DreamCar -writable-system -no-snapshot
   ```

   When launching through a non-interactive Codex shell, keep the emulator UI visible and avoid blocking the shell:

   ```powershell
   Start-Process -FilePath "emulator" -ArgumentList "-avd", "DreamCar", "-writable-system", "-no-snapshot"
   ```

3. Wait for Android to finish booting before pushing system files:

   ```powershell
   adb wait-for-device
   adb shell getprop sys.boot_completed
   ```

   Continue only when `sys.boot_completed` returns `1`.

## Resolve the Emulator Executable

If `emulator` is not available in `PATH`, resolve `emulator.exe` from the first existing location below and use it with the same arguments:

1. `$env:ANDROID_HOME\emulator\emulator.exe`
2. `$env:ANDROID_SDK_ROOT\emulator\emulator.exe`
3. `$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe`

Do not omit `-writable-system` or `-no-snapshot`. Do not replace this workflow with the Android Studio Device Manager. Do not install MiniIVI apps with `adb install`; they are system apps and must be pushed into the system image using the relevant project deployment workflow.
