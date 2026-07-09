# Project Instructions

This repository is shared between Codex Desktop on Windows and Codex CLI on Ubuntu.

## Handoff Workflow

Before switching machines or Codex surfaces:

1. Update `PROGRESS.md` with the current state and next action.
2. Update `TODO.md` if task priorities changed.
3. Commit the changes.
4. Push to the shared remote.

When starting work on either machine:

1. Pull the latest changes.
2. Read `PROGRESS.md`, `TODO.md`, and `DECISIONS.md`.
3. Continue from the documented next action.

## Git Rules

- Keep commits focused and descriptive.
- Do not rewrite shared history unless explicitly coordinated.
- Do not commit local credentials, API keys, or machine-specific Codex config.
- Prefer documenting task context in this repository instead of relying on Codex chat history.

## Verification

- Record any command used to verify work in `PROGRESS.md` when handing off.
- If verification cannot be run, note the reason in `PROGRESS.md`.
