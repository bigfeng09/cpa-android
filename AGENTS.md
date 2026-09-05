# Project Instructions

This directory is a shared Windows/Ubuntu working area whose root Git repository currently publishes the CPA Android project. It also contains other project copies, generated artifacts, and local workspaces. These instructions apply to the root repository and do not automatically make every child directory part of the root release.

## Scope and source of truth

- Keep the root repository's source, documentation, and release metadata in Git.
- Treat a child directory as an independent project when it has its own Git repository, deployment process, or project-specific instructions. Follow its local rules for that work.
- Do not add, commit, or publish unrelated untracked project copies, generated output, credentials, build directories, or machine-specific files.
- This repository currently has no project-local `SKILL.md`. Load global Skills only when the task matches their trigger conditions; do not treat every available Skill as a mandatory checklist.

## Read information as needed

- Read this file before making repository changes.
- Read `PROGRESS.md` when the task needs recent implementation context, a cross-machine handoff, or the current verification state.
- Read `TODO.md` when prioritizing or changing outstanding work.
- Read `DECISIONS.md` when a design, workflow, repository-boundary, or release decision depends on prior context.
- Do not require a full read of all project documents for a small, isolated, or read-only task.
- Pull from the remote only when remote synchronization is needed. Inspect the working tree first and preserve unrelated local work.

## Git and publication

- Keep commits focused and descriptive.
- Do not rewrite shared history, force-push, delete remote data, or change repository visibility without separate explicit authorization.
- Never commit real credentials, API keys, signing material, tokens, private filesystem paths, machine-specific configuration, or generated build output.
- Place secrets in the appropriate local or deployment secret mechanism. Place distributable binaries in the repository's established release channel rather than Git history.
- A read-only review, plan, exploration, or unfinished experiment does not require a commit or push.
- For cross-machine handoff, update `PROGRESS.md` with the current state, next action, and relevant verification only when another machine or surface needs to continue the work. Commit and push the handoff when the other machine needs the changes.
- For a completed update to this existing GitHub repository, follow the global publication rules: inspect the scoped diff, run appropriate verification, commit only intended changes, integrate remote updates without rewriting shared history, and push the source branch. Do not publish incomplete or ambiguous work.

## Risk-based verification

Choose verification that matches the changed surface and record what was run when it matters for handoff or release.

- Low risk: documentation, comments, formatting, and non-behavioral metadata. Check formatting, links or paths as relevant, and obvious secret leakage.
- Medium risk: a feature module, parser, API adapter, UI behavior, or configuration used by code. Run the affected unit tests and the project's focused lint, typecheck, or smoke checks.
- High risk: authentication, credential storage, database migration, deployment, release metadata, APKs, or remote production changes. Run the native tests and build checks, plus privacy/secret scanning and any required deployment or artifact checks.
- Expand beyond the focused checks when the change crosses module boundaries, changes the build system, affects a public contract, or is being released.
- If a relevant check cannot run, state the reason and the residual risk. Do not block unrelated progress merely because an optional or environment-specific check is unavailable.

For Android changes that affect behavior, build configuration, credentials, or releases, retain the established `testDebugUnitTest`, `lintDebug`, and `assembleDebug` verification. Do not run the full Android release suite for unrelated documentation-only changes.

## Completion criteria

A task is complete when:

- The requested scope is implemented or the remaining work and blocker are explicit.
- Verification appropriate to the risk has passed, or skipped checks and residual risk are documented.
- The scoped diff and working-tree status have been reviewed and unrelated files are excluded.
- No real secrets, signing material, or machine-local configuration were introduced.
- Handoff, deployment, or release documentation is updated only when that task actually requires it.
