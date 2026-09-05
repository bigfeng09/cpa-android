# Decisions

## 2026-06-09: Share Codex project progress through Git

- Use Git as the source of truth for code and project files.
- Use repository documents for Codex handoff context.
- Do not attempt to sync Codex internal chat/session history between Desktop and CLI.
- Do not sync the full `.codex` user configuration directory between machines.

## 2026-09-05: Use task-triggered project guidance

- Read project context documents when the task needs them; do not require a full read before every small or read-only task.
- Select verification by changed surface and risk. Keep full Android build, privacy, deployment, and artifact checks for changes that actually need them.
- Separate cross-machine handoff and release publication from ordinary development, review, and exploratory work.
- Treat the root repository as the publication boundary. Child project copies and generated/local workspaces remain out of scope unless explicitly selected.
