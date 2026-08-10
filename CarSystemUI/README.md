# CarSystemUI

This project is prepared to build a platform-signed, privileged SystemUI APK.
It does **not** include platform keys or proprietary framework files.

## 1. Platform signing key

The APK that replaces `com.android.car.systemui` must be signed with the same
platform certificate as the target system image. A newly generated key cannot
replace an APK signed by an OEM key.

Copy `signing.properties.example` to `signing.properties`, place the keystore
under `keys/`, and update its values. Both locations are ignored by Git. If the
four properties are absent, Gradle keeps the normal Android debug signing for
debug builds and produces an unsigned release build.

For an AOSP engineering build, create a JKS from that build's
`build/target/product/security/platform.pk8` and `platform.x509.pem`, or configure
your product build to sign the APK. Do not use public AOSP test keys on a product
device.

To regenerate the local PKCS#12 keystore from AOSP key files:

```powershell
& 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe' `
  tools/ImportPlatformKey.java keys/platform.pk8 keys/platform.x509.pem `
  keys/platform.p12 android
```

## 2. Framework compile dependencies

Put the framework stub JARs from the **same Android branch/product** in
`app/libs/framework/`. Gradle loads every JAR there as `compileOnly`, so none of
the platform classes are packaged into the APK.

Prefer JARs produced by the AOSP build, for example the relevant output from:

- `framework-minus-apex`
- `framework-res`/platform APIs
- `SystemUI-core`
- `car-system-ui-lib`
- the platform current-user API used by navigation

Exact output paths vary by Android branch. Use artifacts from `out/soong/.intermediates`
or your product SDK rather than random online JARs. Files pulled from `/system/framework`
may be optimized and may omit hidden-API compile stubs.

## 3. Install as a privileged system app

Signing alone is insufficient. Include the APK in the system image under
`/system_ext/priv-app/CarSystemUI` (the partition may differ by product), add its
privileged-permission allowlist XML, and disable/remove the original package in
the product configuration. Package name, certificate, permissions, and framework
JAR versions must all match the target ROM.

Build on Windows with Android Studio's bundled JDK:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

The climate controls require a privileged-permission allowlist. When the APK is
installed at `/system_ext/priv-app/CarSystemUI/CarSystemUI.apk`, also copy:

```text
system_ext/etc/permissions/privapp-permissions-com.android.car.systemui.xml
    -> /system_ext/etc/permissions/privapp-permissions-com.android.car.systemui.xml
```

The allowlist XML must be placed on the same partition as the privileged APK.
After copying both files, reboot the device; Android reads privapp allowlists
during system startup.

## 4. SystemUI architecture and Quick Control

The runtime UI is implemented with Kotlin and Jetpack Compose. A process-scoped
dependency container supplies platform repositories to AndroidX ViewModels,
while `BottomNavigationService` only owns the navigation and overlay windows.

The rightmost navigation action opens Quick Control. It provides manual display
brightness, media volume, dual-zone HVAC temperature, and A/C controls. These
features are supplied by the separately deployed MiniIVI Car Service through the
shared typed AIDL client. CarSystemUI no longer opens its own `android.car`
connection or owns car-control permissions.

The service reads HVAC capability and range information from the target VHAL at
runtime. Final validation must therefore be performed on the target AAOS build;
a host build cannot verify vehicle property area IDs or vendor permission
policy. If the service is unavailable, Quick Control keeps rendering and marks
the affected controls unavailable while the client retries the binding.
