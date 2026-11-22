# Contributing to CASERO

Thanks for taking the time to contribute. This document explains how to propose
changes to the app.

## Ground rules

- Everything — code, comments, identifiers, documentation — is written in
  **English**.
- Be respectful. All participation is governed by our
  [Code of Conduct](CODE_OF_CONDUCT.md).
- Never commit credentials, signing keys, keystores, provisioning profiles, API
  tokens, or real guest data. If you ever paste a sample request, strip the
  username, password, cookies and any personal data first.

## Getting set up

1. Fork the repo on GitHub
2. Clone your fork:
   ```bash
   git clone https://github.com/<your-username>/casero.cu.git
   cd casero.cu
   ```

You need Android Studio or IntelliJ IDEA with the Kotlin Multiplatform plugin.
Building and running on iOS also needs macOS and a recent Xcode.

## Branching

- `main` is always releasable.
- Branch from `main` using a descriptive prefix:
  - `feat/guest-list-paging`
  - `fix/session-expiry-redirect`
  - `docs/api-contract`

## Commits

This project uses [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(optional scope): <description>

[optional body]

[optional footer]
```

Common types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`,
`build`, `ci`, `chore`.

Examples:

```
feat(guests): parse .NET /Date(ms)/ timestamps as UTC
fix(auth): pin the casero.rem.cu certificate on Android
docs: document the ViasComunicacion endpoint
```

Keep the subject line at 50 characters or fewer, in the imperative mood. Do not
add tool or AI attribution trailers.

## Pull requests

1. Make sure both apps build (`make build`).
2. Run the checks (`make check`).
3. Fill in the [pull request template](.github/PULL_REQUEST_TEMPLATE.md).
4. Link any issue the PR closes (`Closes #123`).
5. Keep PRs focused — one logical change per PR.

The exact build and test commands are in [AGENTS.md](AGENTS.md#commands).

## Reporting bugs and requesting features

- [Open an issue](https://github.com/albertolicea00/casero.cu/issues/new) and
  say which platform it affects (Android, iOS or both)
- Use the issue templates under [.github/ISSUE_TEMPLATE](.github/ISSUE_TEMPLATE)
- For anything touching the portal API, describe the request/response without including real credentials or guest data
