# Progress

## CPA Usage Android App MVP

### Current State

- Native Android MVP project lives in `cpa-android`.
- UI design artifacts and developer handoff live in `output/ui`.
- The app connects to user-configured CPA Usage Keeper and CLI Proxy API management endpoints.
- Default example addresses are placeholders only:
  - `http://your-host:8318`
  - `https://your-domain.example/management.html#/quota`
- Local secrets, API keys, passwords, build outputs, APK files, and machine-specific SDK config are excluded from Git.

### Latest Fixes
- Productized the app as `v0.4.0` (`versionCode 7`) with a four-tab `总览 / 账号 / 用量 / 更多` information architecture.
- Added a two-step onboarding flow for Usage Keeper and optional CLI Proxy API account-service configuration.
- Required a real, reachable `/api/v1/status` check before onboarding can save the Usage Keeper connection and proceed.
- Added a health-first overview with service, authentication, reliability, and account-service status plus direct account/log actions.
- Changed refresh to progressive loading: lightweight session/status/model/pricing responses render before the large usage payload completes.
- Added a lightweight dashboard cache that is explicitly labeled with its original range until online usage data replaces it.
- Consolidated cost trends, model usage, and credentials under `用量`, and logs/settings under `更多`.
- Added URL validation regression coverage for blank, placeholder, LAN, and real-domain service addresses.

- Preserved encrypted credentials after transient Android Keystore read failures and retry credential recovery before authenticated requests.
- Switched login state and endpoint saves to synchronous commits so reopening the app reliably restores the previous connection.
- Updated the app to version `0.3.2` (`versionCode 6`) for the session persistence fix.
- Synced Android price edits back to CPA Usage Keeper through `PUT /api/v1/pricing/{model}`; local prices now change only after the server accepts the update.
- Made successful `GET /api/v1/pricing` responses authoritative so stale Android-only price entries are removed instead of merged indefinitely.
- Updated the app to version `0.3.1` (`versionCode 5`) for the price-sync release.
- Added per-account Codex authentication file enable/disable controls backed by `PATCH /v0/management/auth-files/status`; disabled files remain visible and can be re-enabled.
- Fixed Codex `used_percent = 1` being incorrectly multiplied to 100%, which made an account with quota remaining appear as 0% remaining.
- Added one automatic quota retry and preserved the last valid quota when the upstream response is empty, unrecognized, or temporarily fails.
- Added quota percentage regression tests and updated the app to version `0.3.0` (`versionCode 4`).
- Replaced the repeated per-model price cards with one compact price editor driven by a model dropdown.
- Added a cost model filter so the cost trend and estimated cost can switch between all models and one selected model.
- Added tested shared cost calculation/filter helpers and updated the app to version `0.2.0` (`versionCode 3`).
- Added a tag-driven GitHub Actions release workflow that verifies and publishes versioned APK assets.
- Separated the CPA Usage Keeper login password from the CLI Proxy API management key; logging in no longer copies or overwrites the management key.
- Added a one-time upgrade migration that clears a management key when it matches the old automatically copied login password.
- Changed the management key field to a masked password input and added a real logout action that clears both encrypted credentials.
- Extracted URL normalization into a testable Java utility and added seven unit tests covering root domains, management page URLs, and management API URLs.
- Fixed all Android Lint findings, including invalid integer view IDs, accessibility click handling, draw-time allocations, RTL gravity, backup rules, and hardcoded `setText` strings.
- Added Windows GitHub Actions verification for unit tests, Android Lint, APK assembly, and debug APK artifact upload.
- Updated the app to version `0.1.1` (`versionCode 2`), Java 17, and Android compile/target SDK 36.
- Rechecked tracked CPA Android source, docs, manifest, resources, and generated debug APK for privacy leaks.
- Kept only placeholder service examples in source and docs; removed previously tracked private endpoint/path references from the published tree.
- Replaced plaintext `SharedPreferences` persistence for login password and management key with Android Keystore-backed encrypted storage, with cleanup of old legacy keys on logout.
- Reduced UI exposure of account identifiers by masking visible `authIndex` values and avoiding raw quota-response body display in account cards.
- Replaced the launcher icon with native Android adaptive icon resources built from in-repo vector assets.
- Fixed `CLI Proxy API` URL parsing so the account page refresh works when settings are filled with a root domain, `management.html#/quota`, or `/v0/management` API address.
- Added a Chinese repository homepage README clarifying that the login page is for `CPA Usage Keeper`, while `CLI Proxy API` link and management key are filled later in the in-app `设置` page.

### Privacy Review

- Working-tree scan found no committed passwords, bearer tokens, GitHub tokens, API keys, private LAN hosts, reverse-proxy domains, or personal filesystem paths in the tracked CPA Android files.
- APK scan of `cpa-android/app/build/outputs/apk/debug/app-debug.apk` found no reviewed private strings.
- The previous private history and release/tag entry were replaced on `master` before publication.

### Verification

- Ran from repository root:
  - `rg -n -i 'private-lan-host|private-reverse-proxy|personal-email|local-windows-path|shared-linux-path|legacy-project-name' .gitignore AGENTS.md DECISIONS.md PROGRESS.md TODO.md cpa-android output/ui/cpa-android-developer-spec.md`
  - Result: no matches in tracked CPA Android files after rewriting `PROGRESS.md`.
- Ran from repository root:
  - `rg -n -i 'ghp_[A-Za-z0-9]+|github_pat_[A-Za-z0-9_]+|AKIA[0-9A-Z]{16}|AIza[0-9A-Za-z\-_]{35}|xox[baprs]-[A-Za-z0-9-]+' .gitignore AGENTS.md DECISIONS.md PROGRESS.md TODO.md cpa-android output/ui/cpa-android-developer-spec.md`
  - Result: no matches.
- Ran from `cpa-android`:
  - `$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"; $env:ANDROID_SDK_ROOT=$env:ANDROID_HOME; .\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon`
  - Result: `BUILD SUCCESSFUL`; 13 unit tests passed; Android Lint reported no issues.
- Re-ran the same Gradle verification after adding server-backed price synchronization.
  - Result: `BUILD SUCCESSFUL`; unit tests, Android Lint, and debug APK assembly all passed.
- Ran a clean `v0.3.1` release verification with `clean testDebugUnitTest lintDebug assembleDebug --no-daemon`.
  - Result: `BUILD SUCCESSFUL`; 13 unit tests passed, Android Lint reported no issues, and the APK metadata matched version `0.3.1` (`versionCode 5`).
- Ran a clean `v0.3.2` verification with the same Gradle command.
  - Result: `BUILD SUCCESSFUL`; 13 unit tests passed, Android Lint reported no issues, and debug APK assembly succeeded.
- Compared APK signer certificates: `v0.3.2` matches the published `v0.3.1` certificate SHA-256, so an in-place upgrade can preserve app data.
- Ran a clean `v0.4.0` verification with `clean testDebugUnitTest lintDebug assembleDebug --no-daemon`.
  - Result: `BUILD SUCCESSFUL`; 15 unit tests passed; the Lint XML contains zero issues; APK metadata is `0.4.0` (`versionCode 7`).
- Confirmed the `v0.4.0` signer SHA-256 remains `ad0312960d166300a9b6c259bbc46e4ce9e500d87a5abba9fc1584c01a63f105`, matching `v0.3.2` for in-place upgrades.
- Ran APK privacy scan after copying the APK to a `.zip` and extracting it under `out/cpa-android-apk-scan`:
  - `rg -n -a -i 'private-lan-host|private-reverse-proxy|personal-email|local-windows-path|shared-linux-path|ghp_[A-Za-z0-9]+|github_pat_[A-Za-z0-9_]+' out/cpa-android-apk-scan`
  - Result: no matches.

### GitHub

- Source repository: `https://github.com/bigfeng09/cpa-android`
- Current branch: `master`
- Current source version: `0.4.0` (`versionCode 7`)
- Repository visibility: `public`
- Old remote release/tag `apk-20260709-192327` was deleted before publication.
- Public APK release before this local work: `v0.3.2`
- Next action: install the verified `v0.4.0` APK over `v0.3.2` and validate onboarding, cache labeling, progressive loading, and credential persistence on a real phone.
