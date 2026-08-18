# Agent Guide

This file is the canonical guide for coding agents working in this repository. The lowercase `agent.md` file is kept as a compatibility pointer for tools that look for that name.

## Project Overview

- This is a Paper plugin for Minecraft `1.21.11`.
- Runtime plugin name: `minerogue`.
- Java target and toolchain: `25`.
- Build system: Gradle wrapper.
- Main plugin class: `com.roguelike.RoguelikePlugin`.
- Main source root: `src/main/java/com/roguelike`.
- Runtime resources: `src/main/resources/plugin.yml` and `src/main/resources/config.yml`.
- Public documentation lives in `README.md` and `docs/`.

## Safety Rules

- Do not commit or force-add anything under `server/`; it is local runtime data and is ignored by git.
- Do not commit RCON passwords, generated worlds, logs, plugin runtime config, caches, or jars copied into `server/plugins`.
- Do not delete worlds or reset player data unless the user explicitly asks.
- Do not change unrelated dirty files. This repository may already contain user edits.
- Keep Paper/Minecraft behavior changes minimal.
- Preserve Chinese user-facing documentation style unless the user asks for English or bilingual docs.

## Build

Use the Gradle wrapper from the repository root:

```powershell
.\gradlew.bat build
```

The plugin jar is written to:

```text
build/libs/minerogue-*.jar
```

The project also has a bounded build helper:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 -TimeoutSeconds 180
```

If build script behavior changes, run the regression check:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tests\build-script-check.ps1
```

## Project-Specific User Preferences

- Use docs-first, root-cause fixes. If the user updated docs, treat those docs as the intended spec before editing code.
- UX constraints: no VIP/supporter rank; main GUI title is “菜单”; compass opens the menu on right-click only; Quests are removed; teleport center includes `/delhome home`.
- Boss events: no buildings or marker blocks; use a red particle-only beam; announce `"<boss> 已经苏醒，在 x y z 位置。"`.
- Armor design: armor set names should reflect built-in armor affixes such as thorns/swift/explosive; remove random armor affixes that merely duplicate vanilla enchants; keep both vanilla Protection and custom `damage_reduction` because they are not considered conflicting.

## Documentation Rules

- Treat `plugin.yml`, `config.yml`, `build.gradle`, and current Java source as source of truth.
- Use `Roguelike` as the user-facing plugin name.
- Mention `minerogue-*.jar` only as the build artifact filename pattern.
- Do not document economy, currency, shop, pricing, external hosting, or required external services; the plugin intentionally does not provide those systems.
- Keep command docs aligned with `/rl`, `/rw`, alias `/roguelike`, and permission `roguelike.admin`.
- Keep optional integrations described as optional soft dependencies.

## Documentation-First Implementation Workflow

The user may intentionally edit `docs/` first and then ask agents to implement the plugin from the documented design. When they say “按文档实现” or otherwise ask to build from docs:

1. Read the changed or relevant docs before editing code.
2. Treat the latest docs as the product intent, then inspect the matching Java/config/test files to map that intent onto the current implementation.
3. If docs and current code conflict, implement the documented behavior and keep source defaults, command help, tests, and public docs synchronized.
4. Do not invent undocumented systems; ask only when the docs leave a behavior-changing ambiguity that cannot be resolved from surrounding docs/code.
5. Verify with focused tests plus `./gradlew.bat build`.

## Verification Checklist

Before reporting completion after code changes:

- Run `./gradlew.bat build`.
- If build script behavior changed, run `tests/build-script-check.ps1`.
