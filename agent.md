# Agent Guide

This lowercase file is a compatibility pointer for tools that look for `agent.md`. The runtime plugin name is `minerogue`.

Use the canonical guide in [AGENTS.md](AGENTS.md) for all coding-agent instructions, project rules, build commands, documentation rules, safety rules, and verification checklist.

## Task Completion Workflow

After finishing each requested task:

1. Review the diff and commit the task changes before building.
2. Run `./gradlew.bat build` after the commit.
3. If the build fails, fix the failure in a follow-up commit and run the build again.
4. Do not include unrelated pre-existing changes in the task commit.


When the user says they have modified docs and asks to implement the plugin from those docs, treat the latest repository documentation as the product specification.

Workflow:

1. Read the changed or relevant docs first, usually under `docs/`, then read the matching Java/config/test files.
2. Implement plugin behavior to match the documented design, while preserving existing code style and safety rules.
3. If docs conflict with current code, prefer the user's latest doc intent, but keep `plugin.yml`, `config.yml`, `build.gradle`, and source reality aligned before finishing.
4. Update defaults, command help, tests, and cross-linked docs when the implementation changes behavior.
5. Verify with focused tests plus `./gradlew.bat build`.

Suggested user trigger phrase: “按文档实现”.
