@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem MiniIVI prebuilt system deployment.
rem Edit only these values for the target environment.
set "ADB=adb"
set "EMULATOR=emulator"
set "AVD_NAME=DreamCar"
set "SERIAL="
set "START_EMULATOR=0"
set "DEMO_DIR=%~dp0demo"

if not "%SERIAL%"=="" set "ADB_SERIAL=-s %SERIAL%"

call :find_tool "%ADB%" || goto :fail
if "%START_EMULATOR%"=="1" (
    call :find_tool "%EMULATOR%" || goto :fail
    start "MiniIVI emulator" "%EMULATOR%" -avd "%AVD_NAME%" -writable-system -no-snapshot
)

for %%F in (
    MiniIVICarService.apk
    CarSystemUI.apk
    CarLauncher.apk
    privapp-permissions-com.miniivi.car.service.xml
    default-permissions-com.miniivi.car.service.xml
    privapp-permissions-com.android.car.systemui.xml
    privapp-permissions-com.android.car.launcher.xml
    default-permissions-com.android.car.launcher.xml
) do (
    if not exist "%DEMO_DIR%\%%F" (
        echo Missing demo file: %%F
        goto :fail
    )
)

echo Waiting for Android...
"%ADB%" %ADB_SERIAL% wait-for-device || goto :fail
call :wait_boot || goto :fail

echo Enabling root and writable system partitions...
"%ADB%" %ADB_SERIAL% root || goto :fail
"%ADB%" %ADB_SERIAL% wait-for-device || goto :fail
"%ADB%" %ADB_SERIAL% remount || goto :fail
"%ADB%" %ADB_SERIAL% shell mkdir -p /system_ext/priv-app/MiniIVICarService /system_ext/priv-app/CarSystemUI /system_ext/etc/permissions /system_ext/etc/default-permissions /system/priv-app /system/etc/permissions /system/etc/default-permissions || goto :fail

echo Pushing APKs...
call :push MiniIVICarService.apk /system_ext/priv-app/MiniIVICarService/MiniIVICarService.apk || goto :fail
call :push CarSystemUI.apk /system_ext/priv-app/CarSystemUI/CarSystemUI.apk || goto :fail
call :push CarLauncher.apk /system/priv-app/CarLauncher.apk || goto :fail

echo Pushing permission files...
call :push privapp-permissions-com.miniivi.car.service.xml /system_ext/etc/permissions/privapp-permissions-com.miniivi.car.service.xml || goto :fail
call :push default-permissions-com.miniivi.car.service.xml /system_ext/etc/default-permissions/default-permissions-com.miniivi.car.service.xml || goto :fail
call :push privapp-permissions-com.android.car.systemui.xml /system_ext/etc/permissions/privapp-permissions-com.android.car.systemui.xml || goto :fail
call :push privapp-permissions-com.android.car.launcher.xml /system/etc/permissions/privapp-permissions-com.android.car.launcher.xml || goto :fail
call :push default-permissions-com.android.car.launcher.xml /system/etc/default-permissions/default-permissions-com.android.car.launcher.xml || goto :fail

"%ADB%" %ADB_SERIAL% shell "if [ -f /system_ext/priv-app/CarSystemUI/CarSystemUI.apk ] && [ -e /system_ext/priv-app/CarSystemUI.apk ]; then rm -f /system_ext/priv-app/CarSystemUI.apk; fi" || goto :fail
"%ADB%" %ADB_SERIAL% shell sync || goto :fail
echo Rebooting Android...
"%ADB%" %ADB_SERIAL% reboot || goto :fail
"%ADB%" %ADB_SERIAL% wait-for-device || goto :fail
call :wait_boot || goto :fail

echo Deployed package paths:
"%ADB%" %ADB_SERIAL% shell pm path com.miniivi.car.service || goto :fail
"%ADB%" %ADB_SERIAL% shell pm path com.android.car.systemui || goto :fail
"%ADB%" %ADB_SERIAL% shell pm path com.android.car.launcher || goto :fail
echo MiniIVI prebuilt deployment completed.
exit /b 0

:push
"%ADB%" %ADB_SERIAL% push "%DEMO_DIR%\%~1" "%~2" || exit /b 1
"%ADB%" %ADB_SERIAL% shell chmod 0644 %~2 || exit /b 1
exit /b 0

:wait_boot
ping 127.0.0.1 -n 31 >nul
exit /b 0

:find_tool
if exist "%~1" exit /b 0
where "%~1" >nul 2>&1 && exit /b 0
echo Executable not found: %~1
exit /b 1

:fail
echo Deployment failed.
exit /b 1
