# 🏠 CASERO.cu
Cross-platform app (iOS + Android) for Cuban lodging hosts to submit guest reports to the official portal. 🇨🇺

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Platforms: iOS | Android](https://img.shields.io/badge/Platforms-iOS%20%7C%20Android-555555)](#-tech-stack)
[![Language: Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![UI: Compose Multiplatform](https://img.shields.io/badge/UI-Compose%20Multiplatform-4285F4?logo=jetpackcompose)](https://www.jetbrains.com/compose-multiplatform/)
[![PRs: Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen)](CONTRIBUTING.md)

[Mira la versión en español](README.es.md)

## ⚠️ Disclaimer

> [!WARNING]
> **Unofficial project.** Not affiliated with CIDP-MININT or any government entity. Talks to the official portal using the host's own credentials, the same way a browser does. We are not responsible for changes to `casero.rem.cu`, USSD/SMS codes, or any issues arising from using this software. Use at your own risk. Always verify guest registrations through official channels.

## 🚧 Status

Feature-complete port, not yet released. This repository replaces the native clients
[casero.cu-ios](https://github.com/albertolicea00/casero.cu-ios) (Swift, feature-complete)
and [casero.cu-apk](https://github.com/albertolicea00/casero.cu-apk) (Kotlin, partial)
with a single Kotlin Multiplatform codebase. The iOS app is the reference: the port
must match its features and pass its tests before the first release.

## ✨ Features

- Portal sign-in with scoped certificate pinning, cookies, and anti-forgery tokens.
- Guest lookups by passport, name, CI and temporary CI; batch registration with a
  review step, stay IDs in the result, PDF records (before, after and on failure)
  and retention of unconfirmed registrations. See the
  [registration contract](AGENTS.md#registration--confirmed).
- Guest lists with all pages, date selection, search, active/history filters, and
  offline access to retained data.
- Guest details, optional photo caching, companions, and check-out updates.
- Read-only portal profile and registered phones/emails; changes open the official
  portal page.
- In-app help: the official portal's help and an offline summary of the
  Cubatramite reporting guide.
- CSV/Excel and PDF exports.
- Device-only credential storage, sign-in recovery behind biometrics/passcode,
  and local data deletion on sign-out. See [SECURITY.md](SECURITY.md#saved-credentials-and-automatic-sign-in).
- SMS reporting to **7344** with the **HC** prefix, without portal login. See the
  [SMS contract](AGENTS.md#sms-reporting-contract).
- English and Spanish UI. The privacy policy, terms and EULA are on the
  [website](https://casero-cu.vercel.app/privacy.html).

## 🛠 Tech Stack

| Layer | Choice |
|-------|--------|
| Language | Kotlin |
| Shared code | Kotlin Multiplatform |
| UI | Compose Multiplatform |
| Targets | Android, iOS 16+ |
| Networking | ASP.NET MVC portal (anti-forgery token + session cookies) |

## 🏗 Why one codebase?

The native iOS and Android clients did the same thing and every portal change had
to be fixed twice. Kotlin Multiplatform keeps one codebase for logic and UI while
still compiling to native code on each platform. The pieces that need the OS
(TLS pinning, secure credential storage, biometrics, SMS, USSD on Android) are
small platform modules behind a common interface, so Android keeps the deeper OS
integration it allows. See [ARCHITECTURE.md](ARCHITECTURE.md).

## 🚀 Getting Started

You need JDK 17, the Android SDK, and for iOS a Mac with Xcode and XcodeGen
(`brew install xcodegen`).

```bash
git clone https://github.com/albertolicea00/casero.cu.git
cd casero.cu
make                      # list every task
make run android          # Android (device or emulator)
make run ios              # iOS simulator
make install ios          # your own iPhone (`make install android` for Android)
make test                 # shared tests on the JVM and the iOS simulator
```

Android Studio or IntelliJ IDEA with the Kotlin Multiplatform plugin open the
project directly. For Xcode, run `make ios-project` and open
`iosApp/CaseroCu.xcodeproj`.

Signed Android releases: `make android-release-apk` (APK) or `make android-release`
(Google Play bundle), named `casero-v<version>-<versionCode>-release.apk`. They need
`keys/release.properties`; see [ARCHITECTURE.md](ARCHITECTURE.md#builds-and-signing).

> 🔐 Signing material (`*.jks`, `*.keystore`, `*.p12`, `*.mobileprovision`, `*.p8`) is never committed.

## 🔒 TLS Note

The portal serves a certificate that fails standard validation. The app pins
that certificate for `casero.rem.cu` only; TLS validation is never disabled
globally. See [AGENTS.md](AGENTS.md#portal-api-reference) for the
reverse-engineered request flow.

## 🤝 Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md), [SECURITY.md](SECURITY.md), and [Code of Conduct](CODE_OF_CONDUCT.md). Uses [Conventional Commits](https://www.conventionalcommits.org/).

### 🐛 Reporting Bugs

Open an issue in [this repository](https://github.com/albertolicea00/casero.cu/issues).
The native repositories are archived and no longer take issues.

## 📦 Related

- [casero.cu-web](https://github.com/albertolicea00/casero.cu-web) — Website
- [casero.cu-ios](https://github.com/albertolicea00/casero.cu-ios) — Native iOS client (archived)
- [casero.cu-apk](https://github.com/albertolicea00/casero.cu-apk) — Native Android client (archived)

## 📄 License

[MIT](LICENSE) © 2026 Alberto Licea
