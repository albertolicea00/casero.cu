# ARCHITECTURE.md — CASERO

How the app is built and released. The portal API it talks to, the SMS contract,
findings and open questions are in [AGENTS.md](AGENTS.md). Update this file in
the same commit as any change to the structure described here.

The behavior to port (session handling, registration flow, saved credentials,
pinned TLS certificate, release checklist) is described in the
[iOS architecture](docs/reference/ios-architecture.md). Move each part into this
file as it is implemented here.

---

## Planned layout

Not scaffolded yet.

| Path | What lives there |
|---|---|
| `shared/src/commonMain/` | Portal client, anti-forgery and session handling, models, registration rules, Cuba dates, cache, exports, Compose UI, strings (en/es) |
| `shared/src/commonTest/` | Unit tests ported from the iOS `Tests/`, with fabricated fixtures and a mocked HTTP engine |
| `shared/src/androidMain/` | Android `actual`s: OkHttp certificate pinning, Keystore credentials, BiometricPrompt, SMS intent, USSD |
| `shared/src/iosMain/` | iOS `actual`s: Darwin engine certificate pinning, Keychain credentials, LocalAuthentication, message composer |
| `androidApp/` | Android entry point (`Activity`), manifest, icons, signing config |
| `iosApp/` | Xcode project with a minimal Swift entry point that hosts the shared Compose UI |
| `docs/reference/` | Documentation copied from the archived native clients |

## Platform-specific pieces

Everything else lives in `commonMain`.

| Piece | Android | iOS |
|---|---|---|
| TLS pinning for `casero.rem.cu` | OkHttp `CertificatePinner` / trust manager | Darwin engine challenge handler |
| Saved credentials | Android Keystore | Keychain, `WhenUnlockedThisDeviceOnly` |
| Device owner check | `BiometricPrompt` (with device credential) | `LAContext.deviceOwnerAuthentication` |
| SMS reports | `ACTION_SENDTO` intent | `MFMessageComposeViewController` |
| USSD | `TelephonyManager` | Not available |
| Share / save files | Share intent | Share sheet |

## Release checklist

To be written. Start from the
[iOS release checklist](docs/reference/ios-architecture.md#release-checklist).
