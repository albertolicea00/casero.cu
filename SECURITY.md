# Security Policy

## Reporting a Vulnerability

Report security issues privately through
[GitHub security advisories](https://github.com/albertolicea00/casero.cu/security/advisories/new),
not as public issues.

Do **not** post credentials, session cookies, or real guest data in any issue.

## What to report

- Leaked credentials or tokens in the codebase
- Weak TLS / certificate handling
- Session fixation or replay issues
- Insecure storage of cookies or tokens
- **Changes to the portal** (`casero.rem.cu`) or **USSD/SMS codes** that break the app — these are critical, report them immediately
- Any other vulnerability that could expose a host's account or guest data

## Saved credentials and automatic sign-in

The portal account is an official government account. Every report made from it
is attributed to the host. The app's design follows from that: whoever can use the
saved credentials can view guests' personal data (passports, carnés, birth dates,
photos) and **register guests in the host's name**.

| Setting | Default | Why |
|---|---|---|
| Remember credentials | **On** | Without it the host types the password on every launch and every session expiry. Opt-out is acceptable because the storage below is device-bound and every use is gated. |
| Auto re-authenticate | **Off** | It signs in without typing anything. That is a deliberate opt-in, in line with privacy-by-default practice for personal data of third parties. |

How the credentials are protected on both platforms (details in
[ARCHITECTURE.md](ARCHITECTURE.md#platform-specific-pieces)):

- **Device-bound storage.** Readable only while the device is unlocked and never
  restored to another device from a backup: Keychain with
  `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` on iOS, Android Keystore with
  backup excluded on Android.
- **Device owner check before every use.** Biometrics or the device passcode
  (`BiometricPrompt` / `LAContext`) before signing in with saved credentials. Without it, anyone holding an unlocked
  phone could open the app and act as the host. If the device has no lock screen,
  or the user cancels, there is no silent sign-in and the sign-in banner is shown
  instead.
- **Two triggers, same gate:** at launch and when a request comes back as
  expired. Concurrent expiries share one recovery, so the user sees one prompt.
- **Sign out deletes them,** so an explicit sign-out is never undone by the next
  launch.
- Credentials are saved only at a successful sign-in. Turning on "Remember
  credentials" while already signed in stores nothing until the next sign-in.

Silent sign-in without a device owner check was considered and rejected for the
reason at the top of this section.

Legal notes (not legal advice): Cuba's personal data protection law (Ley 149/2022)
applies to the guests' data this app handles; the requirements above follow
general privacy-by-default practice, not a reviewed reading of that law. This is an
unofficial client; the defaults do not change that, but they reduce what can go
wrong with a host's account and guests' data if a device is lost or shared.

## Test data and personal data in the repository

The app handles personal data of third parties (guests' names, passports, CIs,
birth dates, photos). None of it belongs in this repository.

- **All tests use fabricated data.** Fixtures are made-up people and documents
  (`TEST`, `X0000001`, `00000000001`, `JUAN PEREZ GOMEZ`, …) built in code, and
  network tests answer through a mocked HTTP engine. No test contacts the real
  portal or needs credentials.
- **Docs and comments use made-up examples** that keep the real shape of the
  portal's requests and answers, never captured values.
- **Real browser captures stay local** in `.ignore/` (git-ignored). They contain
  live session cookies and real guest data: never commit them, never paste them
  into issues or pull requests, and sign out of the portal after capturing.
- **Before pushing,** search the diff for names, document numbers, stay IDs or
  cookies taken from a capture. If one slipped into a local commit, rewrite that
  commit before it is pushed; once pushed, treat it as published.

How captures are taken and documented: [AGENTS.md](AGENTS.md#capturing-portal-traffic).

## Scope

This is an unofficial client that talks to `casero.rem.cu` using the host's own credentials. Vulnerabilities in the official portal itself should be reported to CIDP-MININT, not here.
