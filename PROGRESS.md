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

- Rechecked tracked CPA Android source, docs, manifest, resources, and generated debug APK for privacy leaks.
- Kept only placeholder service examples in source and docs; removed previously tracked private endpoint/path references from the published tree.
- Replaced plaintext `SharedPreferences` persistence for login password and management key with Android Keystore-backed encrypted storage, with cleanup of old legacy keys on logout.
- Reduced UI exposure of account identifiers by masking visible `authIndex` values and avoiding raw quota-response body display in account cards.
- Replaced the launcher icon with native Android adaptive icon resources built from in-repo vector assets.

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
  - `.\gradlew.bat :app:assembleDebug`
  - Result: `BUILD SUCCESSFUL`.
- Ran APK privacy scan after copying the APK to a `.zip` and extracting it under `out/cpa-android-apk-scan`:
  - `rg -n -a -i 'private-lan-host|private-reverse-proxy|personal-email|local-windows-path|shared-linux-path|ghp_[A-Za-z0-9]+|github_pat_[A-Za-z0-9_]+' out/cpa-android-apk-scan`
  - Result: no matches.

### GitHub

- Source repository: `https://github.com/bigfeng09/cpa-android`
- Current branch: `master`
- Current published commit: `6916fef`
- Repository visibility: `public`
- Old remote release/tag `apk-20260709-192327` was deleted before publication.
- Next action: install and test the latest APK on a real Android phone against user-supplied endpoints.
