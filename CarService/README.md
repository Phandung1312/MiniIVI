# MiniIVI Car Service

Privileged application-level Binder service for MiniIVI vehicle state and controls. It does not replace the Android framework car service.

## Flow

`BootReceiver` starts the service -> clients bind through `IMiniIviCarService` -> the Binder permission is enforced -> feature controllers read/write platform backends -> controller `StateFlow`s publish state through Binder callbacks to clients.

The service owns six controllers: brightness, audio, HVAC, vehicle status, quick controls, and Bluetooth. `car-service-client` exposes their remote states as Kotlin `StateFlow`s for CarSystemUI and CarLauncher.

## Implementation status

| Feature | Real implementation | Mock or fallback |
| --- | --- | --- |
| Brightness | `DisplayManager` and `Settings.System` | None; settings are a platform fallback when display APIs are unavailable. |
| Audio | `CarAudioManager` | `AudioManager` media-stream control when car audio is unavailable; this is a platform fallback, not a mock. |
| HVAC | AAOS `CarPropertyManager` for available temperature, A/C, and climate properties | Missing AAOS properties use persisted local state for extended controls and unavailable zones. |
| Vehicle status | AAOS `CarPropertyManager` for battery, range, outside temperature, and tire pressure | Missing individual properties use defaults: 78%, 30 C, 320 km, and 230 kPa. |
| Bluetooth | System `BluetoothAdapter` for adapter state, discovery, paired devices, and connections | None; reports unavailable when Bluetooth is unsupported. |
| Quick controls | System Wi-Fi, Bluetooth, hotspot, connectivity, and power APIs | Missing Wi-Fi/hotspot managers use persisted local state; valet mode is local persisted state only. |

The AIDL contract is defined in `api`. New Binder methods must be appended to preserve compatibility; current clients use API version 5 with minimum compatible version 3.
