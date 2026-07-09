# Agent Guide

This file is the canonical guide for coding agents working in this repository. The lowercase `agent.md` file is kept as a compatibility pointer for tools that look for that name.

## Project Overview

- This is a Paper plugin for Minecraft `1.21.11`.
- Runtime plugin name: `Roguelike`.
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
- Keep Paper/Minecraft behavior changes minimal and verify them in game when possible.
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

## Documentation Rules

- Treat `plugin.yml`, `config.yml`, `build.gradle`, and current Java source as source of truth.
- Use `Roguelike` as the user-facing plugin name.
- Mention `minerogue-*.jar` only as the build artifact filename pattern.
- Do not document economy, currency, shop, pricing, external hosting, or required external services; the plugin intentionally does not provide those systems.
- Keep command docs aligned with `/rl`, `/rw`, alias `/roguelike`, and permission `roguelike.admin`.
- Keep optional integrations described as optional soft dependencies.

## Local Paper Test Server

The local Paper server, when present, lives at:

```text
server/
```

Important local settings normally used for smoke tests:

- Minecraft address: `127.0.0.1:25565`
- RCON address: `127.0.0.1:25575`
- `server-ip=127.0.0.1`
- `server-port=25565`
- `online-mode=false` for localhost bot testing.
- `enforce-secure-profile=false` for offline-mode bot compatibility.
- `enable-rcon=true` for command automation.
- `rcon.port=25575`
- `white-list=false`
- `gamemode=survival`
- `difficulty=easy`
- `level-name=world`
- `view-distance=10`
- `simulation-distance=10`
- `spawn-protection=16`

The RCON password is stored only in ignored local `server/server.properties`. Read it from that file when needed; do not copy it into tracked files.

If `server/server.properties` is missing or regenerated, configure the keys above before running automated smoke tests. Keep `rcon.password` local-only and never paste it into tracked files, logs, docs, or commit messages.

## Deploy And Start Server

From the repository root, build first:

```powershell
.\gradlew.bat build
```

Then start the local server through the server script if it exists:

```powershell
Push-Location -LiteralPath ".\server"
.\start.bat
Pop-Location
```

`server/start.bat` is expected to copy the newest `build/libs/minerogue-*.jar` into `server/plugins`, verify the SHA256 hash, then start `server.jar` in the foreground.

Expected startup evidence in console or `server/logs/latest.log`:

```text
[Roguelike] Enabling Roguelike v1.0.0
[Roguelike] Roguelike plugin enabled.
RCON running on 127.0.0.1:25575
Done (...s)! For help, type "help"
```

Because the server is a long-running process, stop it cleanly with:

```text
stop
```

## Manual In-Game Smoke Tests

After the server reaches `Done`, connect a local client or bot to:

```text
127.0.0.1:25565
```

Useful smoke commands:

```text
/plugins
/rl
/rl status
/rl tickets
/rw help
/rw reload
/rw debug status
/rw list weapons
/rw list armor
/rw list items
/rw give weapon wooden_sword <player> 1
/rw give ticket ticket_a <player> 1
/rw stats <player>
```

When testing player-facing behavior, verify both chat output and server log errors.

## Future Automation Direction

Preferred local automation path:

1. Build with `./gradlew.bat build`.
2. Deploy the newest `minerogue-*.jar` through `server/start.bat` or a dedicated smoke script.
3. Wait for `Done (` in `server/logs/latest.log`.
4. Use RCON for server commands such as `plugins`, `list`, `/rw reload`, and setup commands.
5. Use a Minecraft bot for true in-game actions such as joining, chatting, moving, mining, eating, attacking, opening GUIs, and checking inventory.
6. Stop the server cleanly through RCON or console `stop`.

Do not introduce external hosting for tests unless the user explicitly asks. Keep all game tests local and reproducible.

## Verification Checklist

Before reporting completion after code changes:

- Run `./gradlew.bat build`.
- If build script behavior changed, run `tests/build-script-check.ps1`.
- If plugin runtime behavior changed, start `server/start.bat` and confirm the plugin enables.
- For gameplay changes, perform an in-game or bot/RCON smoke test that exercises the changed feature.
- Check `server/logs/latest.log` for exceptions or plugin errors.
- Confirm no ignored `server/` files or secrets are staged.
