# Agent Guide

This lowercase file is a compatibility pointer for tools that look for `agent.md`.

Use the canonical guide in [AGENTS.md](AGENTS.md) for all coding-agent instructions, project rules, build commands, documentation rules, local server safety rules, and verification checklist.

## User Documentation-First Workflow

When the user says they have modified docs and asks to implement the plugin from those docs, treat the latest repository documentation as the product specification.

Workflow:

1. Read the changed or relevant docs first, usually under `docs/`, then read the matching Java/config/test files.
2. Implement plugin behavior to match the documented design, while preserving existing code style and safety rules.
3. If docs conflict with current code, prefer the user's latest doc intent, but keep `plugin.yml`, `config.yml`, `build.gradle`, and source reality aligned before finishing.
4. Update defaults, command help, tests, and cross-linked docs when the implementation changes behavior.
5. Verify with focused tests plus `./gradlew.bat build`; for gameplay features, state what still needs in-game smoke testing on the user-run local server.

Suggested user trigger phrase: “按文档实现”.
