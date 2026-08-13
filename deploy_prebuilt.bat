@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem MiniIVI prebuilt system deployment.
rem Edit only these values for the target environment.
set "ADB=adb"
set "EMULATOR=emulator"
set "AVD_NAME=DreamCar"
set "SERIAL="
set "START_EMULATOR=0"
set "BOOT_TIMEOUT_SECONDS=180"
set "MEDIA_TIMEOUT_SECONDS=60"
set "MEDIA_USER=10"
set "DEMO_DIR=%~dp0demo"
set "MEDIA_DIR=%~dp0demo\media"
set "STATUS_FILE=%TEMP%\miniivi-deploy-status-%RANDOM%.txt"

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
    MiniIviMaps.apk
    MiniIviBootProgressOverlay.apk
    privapp-permissions-com.miniivi.car.service.xml
    default-permissions-com.miniivi.car.service.xml
    privapp-permissions-com.android.car.systemui.xml
    privapp-permissions-com.android.car.launcher.xml
    default-permissions-com.android.car.launcher.xml
    default-permissions-com.miniivi.maps.xml
    bootanimation.zip
    bootanimation-dark.zip
) do (
    if not exist "%DEMO_DIR%\%%F" (
        echo Missing demo file: %%F
        goto :fail
    )
)

for %%F in (example-track-01.mp3 example-track-02.mp3) do (
    if not exist "%MEDIA_DIR%\%%F" (
        echo Missing media file: %%F
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
"%ADB%" %ADB_SERIAL% shell mkdir -p ^
    /system_ext/priv-app/MiniIVICarService ^
    /system_ext/priv-app/CarSystemUI ^
    /system_ext/etc/permissions ^
    /system_ext/etc/default-permissions ^
    /system/priv-app ^
    /system/app/MiniIviMaps ^
    /system/etc/permissions ^
    /system/etc/default-permissions ^
    /product/media ^
    /product/overlay || goto :fail

echo Pushing APKs...
call :push MiniIVICarService.apk /system_ext/priv-app/MiniIVICarService/MiniIVICarService.apk || goto :fail
call :push CarSystemUI.apk /system_ext/priv-app/CarSystemUI/CarSystemUI.apk || goto :fail
call :push CarLauncher.apk /system/priv-app/CarLauncher.apk || goto :fail
call :push MiniIviMaps.apk /system/app/MiniIviMaps/MiniIviMaps.apk || goto :fail
call :push MiniIviBootProgressOverlay.apk /product/overlay/MiniIviBootProgressOverlay.apk || goto :fail

echo Pushing permission files...
call :push privapp-permissions-com.miniivi.car.service.xml /system_ext/etc/permissions/privapp-permissions-com.miniivi.car.service.xml || goto :fail
call :push default-permissions-com.miniivi.car.service.xml /system_ext/etc/default-permissions/default-permissions-com.miniivi.car.service.xml || goto :fail
call :push privapp-permissions-com.android.car.systemui.xml /system_ext/etc/permissions/privapp-permissions-com.android.car.systemui.xml || goto :fail
call :push privapp-permissions-com.android.car.launcher.xml /system/etc/permissions/privapp-permissions-com.android.car.launcher.xml || goto :fail
call :push default-permissions-com.android.car.launcher.xml /system/etc/default-permissions/default-permissions-com.android.car.launcher.xml || goto :fail
call :push default-permissions-com.miniivi.maps.xml /system/etc/default-permissions/default-permissions-com.miniivi.maps.xml || goto :fail

echo Pushing boot artifacts...
call :push bootanimation.zip /product/media/bootanimation.zip || goto :fail
call :push bootanimation-dark.zip /product/media/bootanimation-dark.zip || goto :fail

echo Pushing sample media for Android user %MEDIA_USER%...
"%ADB%" %ADB_SERIAL% shell mkdir -p /data/media/%MEDIA_USER%/Music || goto :fail
call :push_media example-track-01.mp3 || goto :fail
call :push_media example-track-02.mp3 || goto :fail
"%ADB%" %ADB_SERIAL% shell chown media_rw:media_rw ^
    /data/media/%MEDIA_USER%/Music ^
    /data/media/%MEDIA_USER%/Music/example-track-01.mp3 ^
    /data/media/%MEDIA_USER%/Music/example-track-02.mp3 || goto :fail
"%ADB%" %ADB_SERIAL% shell chmod 0770 /data/media/%MEDIA_USER%/Music || goto :fail
"%ADB%" %ADB_SERIAL% shell chmod 0660 ^
    /data/media/%MEDIA_USER%/Music/example-track-01.mp3 ^
    /data/media/%MEDIA_USER%/Music/example-track-02.mp3 || goto :fail
"%ADB%" %ADB_SERIAL% shell restorecon ^
    /data/media/%MEDIA_USER%/Music ^
    /data/media/%MEDIA_USER%/Music/example-track-01.mp3 ^
    /data/media/%MEDIA_USER%/Music/example-track-02.mp3 || goto :fail

"%ADB%" %ADB_SERIAL% shell "if [ -f /system_ext/priv-app/CarSystemUI/CarSystemUI.apk ] && [ -e /system_ext/priv-app/CarSystemUI.apk ]; then rm -f /system_ext/priv-app/CarSystemUI.apk; fi" || goto :fail
"%ADB%" %ADB_SERIAL% shell sync || goto :fail
echo Rebooting Android so system policies and sample media are loaded...
"%ADB%" %ADB_SERIAL% reboot || goto :fail
"%ADB%" %ADB_SERIAL% wait-for-device || goto :fail
call :wait_boot || goto :fail

echo Verifying deployed packages...
"%ADB%" %ADB_SERIAL% shell pm path com.miniivi.car.service || goto :fail
"%ADB%" %ADB_SERIAL% shell pm path com.android.car.systemui || goto :fail
"%ADB%" %ADB_SERIAL% shell pm path com.android.car.launcher || goto :fail
"%ADB%" %ADB_SERIAL% shell pm path com.miniivi.maps || goto :fail
"%ADB%" %ADB_SERIAL% shell pm path com.miniivi.bootprogress.overlay || goto :fail

echo Verifying sample media...
"%ADB%" %ADB_SERIAL% root || goto :fail
"%ADB%" %ADB_SERIAL% wait-for-device || goto :fail
"%ADB%" %ADB_SERIAL% shell ls -lZ ^
    /data/media/%MEDIA_USER%/Music/example-track-01.mp3 ^
    /data/media/%MEDIA_USER%/Music/example-track-02.mp3 || goto :fail
call :wait_media example-track-01.mp3 || goto :fail
call :wait_media example-track-02.mp3 || goto :fail

echo MiniIVI prebuilt deployment completed.
del /q "%STATUS_FILE%" >nul 2>&1
exit /b 0

:push
"%ADB%" %ADB_SERIAL% push "%DEMO_DIR%\%~1" "%~2" || exit /b 1
"%ADB%" %ADB_SERIAL% shell chmod 0644 "%~2" || exit /b 1
exit /b 0

:push_media
"%ADB%" %ADB_SERIAL% push "%MEDIA_DIR%\%~1" "/data/media/%MEDIA_USER%/Music/%~1" || exit /b 1
exit /b 0

:wait_boot
set /a WAITED_SECONDS=0
:wait_boot_loop
"%ADB%" %ADB_SERIAL% shell getprop sys.boot_completed >"%STATUS_FILE%" 2>nul
findstr /x "1" "%STATUS_FILE%" >nul
if not errorlevel 1 exit /b 0
if !WAITED_SECONDS! GEQ %BOOT_TIMEOUT_SECONDS% (
    echo Android did not complete boot within %BOOT_TIMEOUT_SECONDS% seconds.
    exit /b 1
)
ping 127.0.0.1 -n 3 >nul
set /a WAITED_SECONDS+=2
goto :wait_boot_loop

:wait_media
set /a WAITED_SECONDS=0
:wait_media_loop
"%ADB%" %ADB_SERIAL% shell content query --user %MEDIA_USER% --uri content://media/external/audio/media --projection _display_name >"%STATUS_FILE%" 2>nul
findstr /c:"%~1" "%STATUS_FILE%" >nul
if not errorlevel 1 exit /b 0
if !WAITED_SECONDS! GEQ %MEDIA_TIMEOUT_SECONDS% (
    echo MediaStore did not index %~1 within %MEDIA_TIMEOUT_SECONDS% seconds.
    exit /b 1
)
ping 127.0.0.1 -n 3 >nul
set /a WAITED_SECONDS+=2
goto :wait_media_loop

:find_tool
if exist "%~1" exit /b 0
where "%~1" >nul 2>&1 && exit /b 0
echo Executable not found: %~1
exit /b 1

:fail
del /q "%STATUS_FILE%" >nul 2>&1
echo Deployment failed.
exit /b 1
