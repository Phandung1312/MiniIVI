# MiniIVI

MiniIVI is split into independent Android projects that are built and deployed as system applications.

| Project | APK | Role |
| --- | --- | --- |
| `CarService` | `MiniIVICarService.apk` | Privileged backend for vehicle state, HVAC, audio, and Bluetooth control. |
| `CarSystemUI` | `CarSystemUI.apk` | System navigation, overlays, status surfaces, and the control center. |
| `CarLauncher` | `CarLauncher.apk` | Home dashboard, application entry points, and local media playback. |
| `MiniIviMaps` | `MiniIviMaps.apk` | Standalone maps application and remote map-preview provider for the launcher. |
| `CarSystemUI/boot-progress-overlay` | `MiniIviBootProgressOverlay.apk` | Framework resource overlay for the MiniIVI boot-progress screen. |

See [DEPLOYMENT.md](DEPLOYMENT.md) for source build, system push, and verification instructions.
