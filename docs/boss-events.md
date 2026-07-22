# 周期 Boss 事件

> 源自桌面文档：`C:\Users\LIPis\Desktop\Roguelike周期Boss事件插件开发指导书.md`

目标：在 `Roguelike` 插件内部实现周期 Boss 事件，不依赖 MythicMobs、DungeonsTower 或 GriefPrevention。

## 核心规则

- Boss 不自然生成。
- Boss 由 Roguelike 插件以 48 小时现实时间周期生成。
- 只在主世界 `world` 生成。
- 生成点必须距离锚点玩家至少 3 区块（48 格）。
- Boss 生成时同步生成结构/竞技场。
- Boss 存活期间区域内不能破坏/放置方块，爆炸不破坏区域。
- Boss 不会离开区域，越界会被传回。
- Boss 事件为全服高风险主世界事件，不是地牢副本。

## 已实现模块

```text
src/main/java/com/roguelike/boss/
  BossEventManager.java
  BossEventConfig.java
  BossEventState.java
  ActiveBossArena.java
  BossSpawnPlanner.java
  BossStructureService.java
  BossArenaProtectionListener.java
  BossLeashService.java
  BossEventStorage.java
  BossEventCommand.java
  BossEventListener.java
```

## 建议新增配置

`plugins/Roguelike/boss-events.yml`

```yaml
boss-events:
  enabled: true
  world: world
  interval-hours: 48
  require-online-player: true
  spawn:
    min-distance-chunks: 3
    max-distance-blocks: 192
    max-attempts: 32
    avoid-spawn-radius-blocks: 128
  arena:
    radius: 32
    protect-blocks-while-active: true
    block-break: true
    block-place: true
    block-explosions: true
    block-buckets: true
    keep-structure-after-death: true
  leash:
    enabled: true
    check-interval-ticks: 20
    max-distance-from-center: 32
    teleport-back-distance: 40
  bosses:
    - id: blood-zombie
      weight: 60
      mob: blood-zombie
      structure:
        type: builtin
        id: blood_altar
      drops:
        items:
          - material: minecraft:diamond
            amount: 1
            chance: 0.35
          - weapon-template: crimson_oath
            amount: 1
            chance: 0.05
    - id: vagrant
      weight: 40
      mob: vagrant
      structure:
        type: builtin
        id: bone_ruins
      drops:
        items:
          - item-template: greater_healing_potion
            amount: 2
            chance: 0.45
```

`boss-events.yml` 是周期 Boss 的总配置文件：每个 `bosses[]` 条目同时定义生成权重、实际生成的内置怪物 ID、结构来源和击杀产出。旧写法 `structure: blood_altar` 仍兼容，等价于 `type: builtin`。

### Boss 条目字段

| 字段 | 说明 |
| --- | --- |
| `id` | 周期 Boss 配置 ID，也是 `/rw boss spawn <id>` 使用的 ID。 |
| `weight` | 周期事件随机选择该 Boss 的权重，最低为 1。 |
| `mob` | 可选。实际生成的 `content/mobs/*.yml` 内置怪物 ID；不写时默认等于 `id`。 |
| `structure` | 结构配置；支持旧版字符串和新版 map。 |
| `drops.items[]` / `loot.items[]` | Boss 死亡时的额外掉落，支持 `material`、`weapon-template`、`item-template`、`amount`、`chance`。概率为 0.0-1.0。 |

### 结构来源

Minecraft Java 原版确实有结构文件：结构方块保存/加载的结构模板是 `.nbt` 文件。Paper/Bukkit 暴露了 `StructureManager` / `Structure` API，Roguelike 可以从 `plugins/Roguelike/<file>` 加载 `.nbt` 并放置到 Boss 场地。

```yaml
structure:
  type: vanilla
  file: structures/boss/blood_altar.nbt
  offset:
    x: -8
    y: 0
    z: -8
  rotation: none # none / clockwise_90 / clockwise_180 / counterclockwise_90
```

投影 Mod 常见的 Litematica `.litematic` 文件不是原版结构文件。本插件当前只识别该配置并给出警告，不会直接粘贴 `.litematic`；请先在游戏内或外部工具中把投影导出/转换为原版 `.nbt` 后再配置为 `type: vanilla`。

```yaml
structure:
  type: litematic
  file: schematics/boss_arena.litematic
```

## 管理命令

- `/rw boss spawn <blood-zombie|vagrant>`
- `/rw boss event status`
- `/rw boss event force`
- `/rw boss event clear`
- `/rw boss event next <hours>`

## 当前第一版行为

- 插件启动时会在 `plugins/Roguelike/boss-events.yml` 导出默认配置；这个文件集中管理所有周期 Boss 的生成、结构和击杀产出。
- 状态保存到 `plugins/Roguelike/boss-events-state.yml`。
- 默认每 48 小时检查并生成一次全服 Boss 事件。
- 已有活动 Boss 时不会生成第二个事件。
- 计划生成失败时会延迟 30 分钟重试，不会强制在不安全位置生成。
- `/rw boss spawn <blood-zombie|vagrant>` 可在管理员当前位置直接召唤已加入事件池的 Boss，用于快速测试 Boss 个体配置。
- `/rw boss event force` 会立即尝试围绕在线玩家生成一次事件。
- `/rw boss event clear` 会移除当前 Boss 实体并解除区域保护。
- Boss 死亡后事件结束，结构默认保留为废墟。
- 区域保护会阻止活动区域内挖掘、放置、水桶/岩浆桶操作，并从爆炸破坏列表移除区域内方块。
- Boss 超出 leash 配置距离时会被传送回竞技场中心。

## 验证命令

```text
/rw boss spawn blood-zombie
/rw boss event status
/rw boss event force
/rw boss event clear
/rw boss event next 0.1
```

## 第一版完成标准

- `/rw boss event force` 能生成 Roguelike 内置 Boss。
- 生成位置距离玩家至少 48 格。
- 生成结构出现。
- 区域不能挖、不能放、爆炸不破坏。
- Boss 越界被传回。
- Boss 死亡后事件结束。
- 状态可持久化。
- 无 MythicMobs 依赖。
