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

The user manually starts and stops the local server by default. Agents must not start, stop, restart, kill, or otherwise control the Paper server process unless the user explicitly asks in that turn. When the user explicitly asks the agent to run server-side tests, use the psmux workflow below instead of opening an unmanaged terminal window.

The RCON password is stored only in ignored local `server/server.properties`. Do not copy it into tracked files, logs, docs, or commit messages. Prefer not to use RCON when the user is manually operating the server.

### psmux Server Test Sessions

When the user explicitly asks the agent to start or manage the local test server for automation, run it in a detached psmux session so RCON/MCP commands and server console output stay separated:

```text
server/start.bat
```

Expected session details:

```text
Session: minerogue-server
Window:  server
Attach:  psmux attach -t minerogue-server
Output:  psmux capture-pane -p -t minerogue-server:server -S -200
```

Rules for psmux testing:

- Use `server/start.bat` only when the user explicitly authorizes agent-controlled server startup/testing in the current turn.
- Keep the server console in psmux; use RCON for console commands and Minecraft MCP for player/bot actions.
- If `psmux` was just installed and is not yet on `PATH`, use the installed WinGet binary path shown by `server/start.bat` rather than editing tracked files with a machine-specific absolute path.
- Before deploying a rebuilt plugin jar, remove duplicate stale `server/plugins/minerogue-*.jar` files so Paper/PlugManX cannot load an older jar.
- After each automated test run, clean up test state: kill temporary hostile/test entities near the bot, restore the bot to creative mode when appropriate, clear short-lived effects/items created only for the test, and check `server/logs/latest.log` for new Roguelike errors.
- At the end of testing, leave the psmux session running only if the user asked to keep the server available; otherwise ask the user before stopping/killing the session.

## Deploy And Hot Reload

From the repository root, build first:

```powershell
.\gradlew.bat build
```

The user has installed PlugManX for local hot-reload testing. After a successful build, ask the user to copy or deploy the newest `build/libs/minerogue-*.jar` into `server/plugins` and run the server manually if it is not already running.

Use PlugManX for manual in-server reload checks. Suggested console or in-game commands:

```text
plugman reload Roguelike
plugins
rw reload
```

If PlugManX command syntax differs on the installed version, use its help command and adapt only the command spelling, not the test intent.

Expected hot-reload evidence in console or `server/logs/latest.log`:

```text
[Roguelike] Enabling Roguelike v1.0.0
[Roguelike] Roguelike plugin enabled.
```

Do not use `server/start.bat`, `java -jar server.jar`, RCON `stop`, task killing, or process-control commands as part of normal verification. Server lifecycle is user-operated.

## Manual In-Game Smoke Tests

After the user confirms the server is running and the plugin has been hot-reloaded, connect a local client or bot to:

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

Preferred local verification path:

1. Build with `./gradlew.bat build`.
2. Have the user deploy the newest `minerogue-*.jar` into `server/plugins`.
3. Have the user run or keep running the local server.
4. Use PlugManX manual commands such as `plugman reload Roguelike`, `plugins`, and `/rw reload` to verify plugin reload behavior.
5. Use a Minecraft client or bot only when the user has the server running and the test requires true in-game actions such as joining, chatting, moving, mining, eating, attacking, opening GUIs, and checking inventory.
6. Ask the user to stop the server when needed; do not stop it yourself.

Do not introduce external hosting for tests unless the user explicitly asks. Keep all game tests local and reproducible.

## Verification Checklist

Before reporting completion after code changes:

- Run `./gradlew.bat build`.
- If build script behavior changed, run `tests/build-script-check.ps1`.
- If plugin runtime behavior changed, ask the user to run the server and hot-reload `Roguelike` with PlugManX, then confirm the plugin enables.
- For gameplay changes, perform or request an in-game/client/bot smoke test that exercises the changed feature on the user-run server.
- Ask the user to check `server/logs/latest.log` or paste relevant errors if the server log is needed; do not rely on direct server process control.
- Confirm no ignored `server/` files or secrets are staged.
