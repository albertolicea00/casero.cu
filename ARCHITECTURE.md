# ARCHITECTURE.md — CASERO

How the app is built and released. The portal API it talks to, the SMS contract,
findings and open questions are in [AGENTS.md](AGENTS.md). Update this file in
the same commit as any change to the structure described here.

The app is a port of the native iOS client. Its flows (session handling,
registration, saved credentials, pinned TLS certificate, release checklist) are
described in detail in the [iOS architecture](docs/reference/ios-architecture.md);
the behavior here is the same unless this file says otherwise.

---

## Repository map

| Path | What lives there |
|---|---|
| `shared/src/commonMain/kotlin/com/casero/cu/` | Everything shared by both apps |
| `…/App.kt` | Tabs, banners, `AuthGate`, login sheet, per-tab navigation stacks |
| `…/app/` | `AppState` (session, saved-credential sign-in, banners), `GuestPhotoService` |
| `…/network/` | `CaseroClient` (every portal request, token handling, session recovery), `PortalError`, `PortalServing`, `AntiForgery`, `PinnedCertificate` |
| `…/model/` | `Guest`, `PersonaValidation` + `GuestRegistration` + `CompanionRegistration` (`personas=` wire formats), `CommunicationChannel`, `PortalProfile` (profile HTML parser), `DotNetDate`, `PortalDates` (Cuba calendar and time zone) |
| `…/persistence/` | `AppSettings`, `GuestCache`, `PhotoCache` |
| `…/reporting/` | `SmsReporter` (7344 / HC message rules) |
| `…/export/` | `ExportService` (CSV, guest PDF, registration PDF) |
| `…/platform/` | Interfaces the platforms implement: preferences, secure store, device owner check, connectivity, SMS composer, file sharing, PDF renderer |
| `…/feature/` | Screens and view models: `sms`, `register`, `guests`, `settings`, `login`, `help` |
| `…/ui/` | Theme, shared components, `UiText` (portal text or localized string), error texts |
| `shared/src/commonMain/composeResources/` | Strings (`values`, `values-es`), the pinned certificate (`files/`), portal tiles (`drawable/`) |
| `shared/src/androidMain/` | Android implementations (`AndroidPlatform`, OkHttp pinning, date formats) |
| `shared/src/iosMain/` | iOS implementations (`IosPlatform`, `KeychainStore`, Darwin pinning, date formats), `MainViewController` |
| `shared/src/commonTest/` | Tests ported from the iOS suite: MockEngine, fabricated fixtures, in-memory files |
| `androidApp/` | `MainActivity`, manifest (no backups, `FileProvider`), icons |
| `iosApp/` | `project.yml` (XcodeGen), the Swift entry point, `Info.plist`, app icon, privacy manifest |
| `docs/reference/` | Documentation of the archived native clients |

## State and screens

- View models are plain classes holding Compose state (`mutableStateOf`) and
  exposing `suspend` functions, like the iOS `@MainActor` view models with
  `async` methods. Screens launch them from `rememberCoroutineScope`, and tests
  call them directly.
- `AppState` is created once in `App()`. Per-tab state that must survive pushing
  a screen (the Guests list, the Settings export form) is held by `MainScreen`.
- Navigation is a small stack per tab (`Route` in `App.kt`). Android's back
  gesture pops it; iOS screens show a back arrow.
- User-facing errors are `UiText`: either the portal's own message or one of the
  app's strings, resolved when shown. `PortalError.toUiText()` holds the texts.

## Platform-specific pieces

Everything else lives in `commonMain`.

| Piece | Android | iOS |
|---|---|---|
| TLS pinning for `casero.rem.cu` | OkHttp with a trust manager that accepts only the pinned leaf | Darwin engine challenge handler (`SecTrust` anchored to the pinned leaf) |
| Saved credentials | AES-GCM key in the Android Keystore (unlocked device only), backups off | Keychain, `WhenUnlockedThisDeviceOnly`, same service and accounts as the native app |
| Device owner check | `BiometricPrompt` (biometrics or screen lock) | `LAContext.deviceOwnerAuthentication` |
| SMS reports | `ACTION_SENDTO` to the default SMS app; the result is unknown | `MFMessageComposeViewController` |
| Share files | `FileProvider` + share sheet | `UIActivityViewController` |
| PDF | `PdfDocument` + `StaticLayout` | `UIGraphicsPDFRenderer` |
| Connectivity | `ConnectivityManager` callback | `nw_path_monitor` |

### Differences from the native iOS app

- **Android SMS:** Android does not report whether the user sent the message, so
  the result is always "unknown" and the app says to check the SMS app.
- **Companions load on their own:** opening a guest asks the portal for the
  companions (when none are cached and the host is signed in), like the photo.
  The native app waited for "Load companions".
- **Cache files:** the guest cache uses new file names (`*-v2.json`). A cache
  written by the native iOS app is ignored; the next reload rebuilds it. Saved
  credentials and settings carry over because the keys did not change.

## Builds and signing

| Build | Command | Output |
|---|---|---|
| Android debug APK | `make build` (or `./gradlew :androidApp:assembleDebug`) | `androidApp/build/outputs/apk/debug/casero-v<versionName>-<versionCode>-debug.apk` |
| Android release APK | `make android-release-apk` | `androidApp/build/outputs/apk/release/casero-v<versionName>-<versionCode>-release.apk` |
| Android release bundle | `make android-release` | `androidApp/build/outputs/bundle/release/casero-v<versionName>-<versionCode>-release.aab` |
| iOS | `make run ios`, `make install ios` | see [Commands](AGENTS.md#commands) |

The name comes from `base.archivesName` in `androidApp/build.gradle.kts`, built
from `versionName` and `versionCode`; AGP appends the build type. Bump both there
before a release.

Android release signing reads `keys/release.properties` (git-ignored; copy
`keys/release.properties.example`):

```properties
storeFile=release.jks
storePassword=...
keyAlias=key0
keyPassword=...
```

- `storeFile` is relative to `keys/`.
- The file is read as UTF-8 with Java `Properties`: a literal backslash in a
  password is written `\\`. Other characters (`= : # ! $` and quotes) go as they
  are, without quotes. Leading spaces of a value are dropped, trailing ones kept.
- Keystores made by `keytool` since Java 9 are PKCS12, where the key password is
  always the store password.
- Without the file, any Gradle run that names a `Release` task stops with a
  message; debug builds do not need it.
- Create a keystore once, back it up (with its password) outside the repository
  and never regenerate it: an app signed with another key cannot update the
  installed one.

  ```bash
  keytool -genkeypair -v -keystore keys/release.jks -storetype PKCS12 \
    -alias key0 -keyalg RSA -keysize 4096 -validity 36525
  ```

The first release build downloads the Android lint tools for
`lintVitalAnalyzeRelease`; on a slow connection that takes a while.

## Release checklist

Start from the [iOS release checklist](docs/reference/ios-architecture.md#release-checklist)
and add, before each release:

1. `make check` passes.
2. Both apps install and open: `make run`, `make run PLATFORM=ios`.
3. Sign in, list guests, register a test lookup up to the review sheet (do not
   send), prepare an SMS without sending, export CSV and PDF — on both platforms.
4. The pinned certificate is not expired (`openssl x509 -inform der -in
   shared/src/commonMain/composeResources/files/casero_rem_cu.cer -noout -enddate`).
