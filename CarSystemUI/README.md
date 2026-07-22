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
- `android.car`

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

The current source is still an Android application template; it has no SystemUI
services or components yet. The setup above supplies signing and compile-time
infrastructure, but replacing the running SystemUI also requires porting the
matching CarSystemUI source/components from the target Android branch.
