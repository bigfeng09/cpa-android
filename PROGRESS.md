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
  - `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon`
  - Result: `BUILD SUCCESSFUL`; 7 unit tests passed; Android Lint reported no issues.
- Ran APK privacy scan after copying the APK to a `.zip` and extracting it under `out/cpa-android-apk-scan`:
  - `rg -n -a -i 'private-lan-host|private-reverse-proxy|personal-email|local-windows-path|shared-linux-path|ghp_[A-Za-z0-9]+|github_pat_[A-Za-z0-9_]+' out/cpa-android-apk-scan`
  - Result: no matches.

### GitHub

- Source repository: `https://github.com/bigfeng09/cpa-android`
- Current branch: `master`
- Current source version: `0.1.1` (`versionCode 2`)
- Repository visibility: `public`
- Old remote release/tag `apk-20260709-192327` was deleted before publication.
- Public APK release: `apk-20260709-public`
- Next action: install and smoke-test the latest APK on a real Android phone against user-supplied endpoints, then publish a replacement APK release from the latest `master`.
