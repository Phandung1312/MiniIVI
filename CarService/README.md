# MiniIVI Car Service

MiniIVI Car Service is a platform-signed privileged application that centralizes
HVAC, media-volume, and display-brightness control for MiniIVI system clients.
It is an application-level bound service and does not replace or register an
Android framework car service. The production backend connects to the
`android.car.Car` implementation already present in the target ROM.

## Modules

- `app` contains the privileged service and the AAOS/platform adapters.
- `car-service-api` contains the canonical AIDL and Parcelable contract.
- `car-service-client` exposes remote feature states as Kotlin `StateFlow`s.

CarSystemUI and CarLauncher include the two library modules directly from this
project. Contract changes must remain backward compatible and new AIDL methods
must be appended to the interface.

## Signing and framework dependencies

CarService intentionally reads `../CarSystemUI/signing.properties` and uses the
same platform keystore as CarSystemUI. The build fails when that file, its four
required properties, or the configured keystore is missing. There is no debug
key fallback because deploying a mismatched system APK can prevent Android from
booting.

The build creates a minimal compile-time framework stub JAR and a small runtime
adapter JAR for hidden display/current-user calls. Only the adapter is packaged
in the APK. The stub has no runtime behavior and must never be packaged. If the
matching ROM framework JAR becomes available, use it as `compileOnly` instead.

## Build

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

## System deployment

Deploy the APK and permission allowlist to `system_ext`; never use `adb install`:

```powershell
adb root
adb remount
adb shell "mkdir -p /system_ext/priv-app/MiniIVICarService"
adb push .\app\build\outputs\apk\debug\app-debug.apk /system_ext/priv-app/MiniIVICarService/MiniIVICarService.apk
adb shell "chmod 644 /system_ext/priv-app/MiniIVICarService/MiniIVICarService.apk"
adb push .\system_ext\etc\permissions\privapp-permissions-com.miniivi.car.service.xml /system_ext/etc/permissions/privapp-permissions-com.miniivi.car.service.xml
adb shell "sync"
adb reboot
```

The service is direct-boot aware, runs in the system user, and protects its
exported Binder with the signature permission
`com.miniivi.car.permission.CONTROL`.

The APK does not use `android.uid.system`. Do not add that shared UID: the
service only needs a platform certificate and its privileged-permission
allowlist, while a certificate mismatch on a package sharing UID 1000 causes a
fatal PackageManager error during boot.
