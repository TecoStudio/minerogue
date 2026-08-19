# GitHub 配方内容迁移实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将铸造配方迁移到仓库 `content/recipes/forge-recipes.yml`，由 GitHub 内容同步下载并由 `ForgeRecipeManager` 加载，不再依赖或生成独立的 `plugins/*/forge-recipes.yml`，且不把配方文件打包进 jar。

**Architecture:** 配方源文件位于 `content/recipes/forge-recipes.yml`，但 Gradle 仅打包现有内容资源时排除该路径。`config.yml` 将配方列为 GitHub 同步文件，`ConfigManager` 下载到运行时 `plugins/minerogue/content/recipes/forge-recipes.yml`；`ForgeRecipeManager` 从该内容路径读取，并在缺少文件时保持空配方而不导出旧路径文件。默认配方全部使用 2×2 shape。

**Tech Stack:** Java 25、Paper API、Gradle、JUnit 5、Bukkit `YamlConfiguration`。

**Spec:** 用户已确认的 GitHub 下载式配方内容迁移方案（本会话已批准）。

## Global Constraints

- 不将 `content/recipes/forge-recipes.yml` 打包进 jar。
- GitHub 同步目标必须是 `plugins/minerogue/content/recipes/forge-recipes.yml`。
- 不把 `server/` 文件夹作为源码或提交内容。
- 配方格式只能是 2 行、每行 2 个字符；旧版 3×3 配方跳过并记录警告。
- 保持现有 `/rw reload` 重载入口和中文用户文档风格。
- 不修改用户已有的无关脏文件。

---

### Task 1: 添加独立 GitHub 配方源文件并配置同步

**Files:**
- Create: `content/recipes/forge-recipes.yml`
- Modify: `src/main/resources/config.yml:23-108`
- Modify: `build.gradle:52-62`
- Test: `src/test/java/com/roguelike/config/DefaultContentTest.java` 或新增针对配置/打包规则的测试

**Interfaces:**
- Produces the canonical repository recipe source at `content/recipes/forge-recipes.yml`.
- Produces the relative sync path `recipes/forge-recipes.yml` in `content.github-sync.files`.

- [ ] **Step 1: Write the failing test**
  - Assert `content/recipes/forge-recipes.yml` exists, contains a `recipes` section, and every shape has exactly 2 rows of width 2.
  - Assert `src/main/resources/config.yml` lists `recipes/forge-recipes.yml` under `content.github-sync.files`.
  - Assert the Gradle resource configuration excludes `content/recipes/forge-recipes.yml` from jar resources.

- [ ] **Step 2: Run the focused test to verify it fails**

Run: `./gradlew.bat test --tests com.roguelike.config.DefaultContentTest`

Expected: FAIL because the recipe source and sync entry do not exist yet.

- [ ] **Step 3: Write the minimal content/config/build changes**
  - Copy the 10 current default recipes into `content/recipes/forge-recipes.yml`, using 2×2 shapes.
  - Add `- recipes/forge-recipes.yml` to `content.github-sync.files`.
  - Change `processResources` so the existing `from('content') { into 'content' }` excludes `recipes/forge-recipes.yml` while preserving all other content packaging.

- [ ] **Step 4: Run focused validation**

Run: `./gradlew.bat test --tests com.roguelike.config.DefaultContentTest`

Expected: PASS, with all source shape assertions green.

- [ ] **Step 5: Commit**

```bash
git add content/recipes/forge-recipes.yml src/main/resources/config.yml build.gradle src/test/java/com/roguelike/config/DefaultContentTest.java
git commit -m "feat: move forge recipes to synced content"
```

---

### Task 2: Load recipes from the runtime content directory

**Files:**
- Modify: `src/main/java/com/roguelike/forge/ForgeRecipeManager.java:21-182`
- Modify: `src/main/java/com/roguelike/RoguelikePlugin.java:35-52` only if lifecycle ordering requires it
- Test: `src/test/java/com/roguelike/forge/ForgeRecipeManagerTest.java`

**Interfaces:**
- `ForgeRecipeManager.init(RoguelikePlugin plugin)` resolves `new File(plugin.getDataFolder(), "content/recipes/forge-recipes.yml")`.
- `ForgeRecipeManager.reload()` loads that file and never exports `forge-recipes.yml` at the plugin data root.
- `saveDefaultsForTest` is removed or replaced by a test fixture writer that does not represent production export behavior.

- [ ] **Step 1: Write the failing test**
  - Add a test that verifies the production path is `content/recipes/forge-recipes.yml` and that initialization/reload does not create a root-level `forge-recipes.yml`.
  - Add a test that loads a temporary runtime content recipe file and verifies 2×2 recipes are parsed while a 3×3 recipe is skipped.

- [ ] **Step 2: Run the focused test to verify it fails**

Run: `./gradlew.bat test --tests com.roguelike.forge.ForgeRecipeManagerTest`

Expected: FAIL because the manager currently points at the plugin root and exports defaults there.

- [ ] **Step 3: Implement the minimal manager migration**
  - Replace `recipesFile` initialization with the content subpath.
  - Remove `exportDefaultsIfMissing`, `saveDefaults`, and production default recipe generation.
  - Keep parser validation and warning behavior for 3×3 files.
  - If the content file is absent, clear recipes and return without creating a file.
  - Keep the existing default recipe test helper only if it is needed as an isolated fixture helper, and rename it so it cannot be confused with production export.

- [ ] **Step 4: Run focused tests**

Run: `./gradlew.bat test --tests com.roguelike.forge.ForgeRecipeManagerTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/roguelike/forge/ForgeRecipeManager.java src/test/java/com/roguelike/forge/ForgeRecipeManagerTest.java
git commit -m "refactor: load forge recipes from content sync path"
```

---

### Task 3: Align reload behavior and documentation

**Files:**
- Modify: `src/main/java/com/roguelike/command/AdminInfoCommands.java` if reload ordering needs explicit content synchronization
- Modify: `docs/forge.md:34-60`
- Modify: `docs/configuration.md:1-60`
- Modify: `content/README.md:4-14`
- Test: relevant command/config tests if reload ordering is changed

**Interfaces:**
- `/rw reload` continues to call configuration/content reload before recipe reload.
- Documentation identifies `content/recipes/forge-recipes.yml` as the repository source and `plugins/minerogue/content/recipes/forge-recipes.yml` as the runtime downloaded file.

- [ ] **Step 1: Inspect and test current reload ordering**
  - Confirm `AdminInfoCommands` reloads `ConfigManager` before `ForgeRecipeManager`.
  - Add a regression assertion only if the existing tests do not cover this order.

- [ ] **Step 2: Implement only required ordering changes**
  - Do not add a second downloader in `ForgeRecipeManager`; content sync remains owned by `ConfigManager`.
  - Update docs to remove instructions about deleting root `forge-recipes.yml` and explain that GitHub sync must provide the recipe file.

- [ ] **Step 3: Run focused tests**

Run: `./gradlew.bat test --tests com.roguelike.forge.ForgeRecipeManagerTest --tests com.roguelike.config.DefaultContentTest`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add docs/forge.md docs/configuration.md content/README.md src/main/java/com/roguelike/command/AdminInfoCommands.java
 git commit -m "docs: document synced forge recipe content"
```

---

### Task 4: Full verification and jar exclusion check

**Files:**
- No source changes unless verification finds a regression.

- [ ] **Step 1: Run all tests**

Run: `./gradlew.bat test`

Expected: PASS.

- [ ] **Step 2: Build the jar**

Run: `./gradlew.bat build`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Verify jar contents**

Run: `jar tf build/libs/minerogue-*.jar | grep 'content/recipes/forge-recipes.yml'`

Expected: no output and a non-zero grep status, proving the recipe is not packaged.

- [ ] **Step 4: Verify other content remains packaged**

Run: `jar tf build/libs/minerogue-*.jar | grep 'content/weapons/ember_knife.yml'`

Expected: the weapon content path is present.

- [ ] **Step 5: Report exact runtime migration**
  - State that the root-level `plugins/*/forge-recipes.yml` is no longer used by source code.
  - State that existing test-server copies are not modified as source deliverables and may be removed manually if desired.
