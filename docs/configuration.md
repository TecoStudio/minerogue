# 配置文件

[返回目录](README.md)

## 主配置

`plugins/Roguelike/config.yml`

```yaml
storage:
  type: json
  sqlite-file: roguelike.db
  save-interval: 1200
  async-save: true
  backup:
    enabled: true
    interval-minutes: 60
    keep: 24

debug:
  enabled: false

gameplay:
  exp-multiplier: 1.0
  progression-exp-multiplier: 1.0
  weapon-drop-multiplier: 1.0
  weapon-drop-chances:
    legendary: 0.002
    epic: 0.005
    rare: 0.010
    common: 0.020

integrations:
  placeholderapi:
    enabled: false
  tab:
    enabled: false
  commandapi:
    enabled: false
  mythicmobs:
    enabled: false
  nova:
    enabled: false

scoreboard:
  enabled: true
  update-interval: 20
```

字段说明：

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `storage.type` | `json` | 玩家数据存储方式，可选 `json` 或 `sqlite`。 |
| `storage.sqlite-file` | `roguelike.db` | SQLite 文件名，相对于 `plugins/Roguelike/`。仅在 `storage.type: sqlite` 时使用。 |
| `storage.save-interval` | `1200` | 定期保存间隔，单位 tick，最低 600 tick。 |
| `storage.async-save` | `true` | 是否将定期保存放到异步线程执行。关服保存仍会同步执行。 |
| `storage.backup.enabled` | `true` | 是否启用自动玩家数据备份。 |
| `storage.backup.interval-minutes` | `60` | 自动备份间隔，最低 10 分钟。 |
| `storage.backup.keep` | `24` | 保留最近多少份 `player-data-*.zip` 备份。 |
| `debug.enabled` | `false` | 是否输出更多开发调试日志，也可通过 `/rw debug` 切换。 |
| `gameplay.exp-multiplier` | `1.0` | 怪物击杀经验倍率。`0` 表示关闭击杀经验。 |
| `gameplay.progression-exp-multiplier` | `1.0` | 挖矿/进食里程碑经验倍率。 |
| `gameplay.weapon-drop-multiplier` | `1.0` | 随机武器掉落倍率。`0` 表示关闭随机武器掉落。 |
| `gameplay.weapon-drop-chances.*` | 见示例 | 各品质随机武器基础掉落概率，最终概率会乘以 `weapon-drop-multiplier` 并限制在 0-1。 |
| `integrations.placeholderapi.enabled` | `false` | 开启 PlaceholderAPI 集成检测。 |
| `integrations.tab.enabled` | `false` | 开启 TAB 集成检测。通常配合关闭内置侧边栏使用。 |
| `integrations.commandapi.enabled` | `false` | 开启 CommandAPI 兼容检测；未安装时自动回退 Bukkit 命令。 |
| `integrations.mythicmobs.enabled` | `false` | 开启 MythicMobs 兼容。启用后插件内置怪物不会自然生成。 |
| `integrations.nova.enabled` | `false` | 开启 Nova 物品支持，武器 `item` 可写 `nova:<namespace>:<id>`。 |
| `scoreboard.enabled` | `true` | 是否启用插件内置侧边栏。 |
| `scoreboard.update-interval` | `20` | 侧边栏更新间隔，单位 tick，代码中最低限制为 10 tick。 |

## 可编辑内容配置

| 文件 | 说明 |
| --- | --- |
| `weapons.yml` | 武器模板 |
| `items.yml` | 物品模板 |
| `mobs.yml` | 怪物经验、精英怪、怪物强化规则 |
| `forge-recipes.yml` | 铸造台配方 |

这些文件的加载规则：

- 插件先加载内置默认内容。
- 若数据目录下存在对应 YAML，则读取 YAML 覆盖或扩展默认内容。
- 相同 ID 的武器、物品或怪物配置会覆盖内置模板。
- 修改配置后需要执行 `/rw reload`，或重启服务器。

也可以直接打开 [配置片段助手](config-tool.html) 生成武器、物品、精英怪/Boss 难度和普通怪强化 YAML 片段。该页面是随文档附带的静态网页，不需要后端或构建步骤；把生成内容复制进对应的 `plugins/Roguelike/*.yml` 后再执行 `/rw reload`。

修改后执行：

```text
/rw reload
```

### `items.yml` 物品模板

`items.yml` 的物品模板使用 `items.<物品 ID>`，可配置以下字段：

| 字段 | 说明 |
| --- | --- |
| `item` | 原版物品材质 ID，例如 `minecraft:potion`。`potion` 和 `tonic` 类型默认使用药水材质。 |
| `name` | 显示名称。 |
| `description` | lore 描述文本。 |
| `item-type` | 物品类型，例如 `potion`、`tonic` 或自定义分类。 |
| `rarity` | 品质文本。 |
| `effects.*` | 自定义效果数值，例如 `heal_amount`、`speed_level`、`resistance_level`、`duration_seconds`。 |

示例：

```yaml
items:
  swift_tonic:
    item: minecraft:potion
    name: 迅捷药剂
    description: 短时间提升移动节奏
    item-type: tonic
    rarity: rare
    effects:
      speed_level: 1.0
      duration_seconds: 12.0
```

通过 `/rw give item <id> [玩家] [数量]` 发放物品时，插件会按 `item` 生成对应材质。药水和药剂模板会显示为药水物品，并在 lore 中显示配置效果；`speed_level`、`resistance_level` 会写入药水元数据，`heal_amount` 会以恢复量说明和药水颜色呈现。

`mobs.yml` 的普通怪强化规则使用 `modifiers.<怪物 ID>`，可配置以下字段：

| 字段 | 说明 |
| --- | --- |
| `health-multiplier` | 最大生命倍率。 |
| `damage-multiplier` | 攻击伤害倍率。 |
| `speed-multiplier` | 移动速度倍率。 |
| `weapon-template` | 生成时给予的 Roguelike 武器模板 ID。 |

示例：

```yaml
modifiers:
  husk:
    health-multiplier: 1.25
    damage-multiplier: 1.10
    speed-multiplier: 0.95
    weapon-template: frost_cleaver
```

## 导出配置

执行：

```text
/rw export
```

导出位置：

```text
plugins/Roguelike/weapons.yml
plugins/Roguelike/items.yml
plugins/Roguelike/mobs.yml
plugins/Roguelike/forge-recipes.yml
plugins/Roguelike/examples
```

导出内容包括：

| 路径 | 内容 |
| --- | --- |
| `plugins/Roguelike/weapons.yml` | 当前可编辑武器模板。 |
| `plugins/Roguelike/items.yml` | 当前可编辑物品模板。 |
| `plugins/Roguelike/mobs.yml` | 当前可编辑怪物配置。 |
| `plugins/Roguelike/forge-recipes.yml` | 当前可编辑铸造配方。 |
| `plugins/Roguelike/examples/weapons.yml` | 武器配置示例。 |
| `plugins/Roguelike/examples/items.yml` | 物品配置示例。 |
| `plugins/Roguelike/examples/mobs.yml` | 怪物配置示例。 |
| `plugins/Roguelike/examples/tab-scoreboard.yml` | TAB 侧边栏示例。 |
| `plugins/Roguelike/examples/sidebar.yml` | 内置侧边栏占位示例。 |
| `plugins/Roguelike/examples/mythicmobs.yml` | MythicMobs 示例。 |

导出不会自动把示例应用到服务器，只用于参考和复制配置字段。

## 铸造配方

铸造配方详见 [铸造台](forge.md)。

## 数据存储

默认使用 JSON 保存玩家数据。

插件会按 `storage.save-interval` 定期保存在线玩家数据，并按 `storage.backup.*` 自动生成 zip 备份。也可以手动执行：

```text
/rw backup
```

备份文件位于：

```text
plugins/Roguelike/backups/player-data-*.zip
```

SQLite 模式会先生成一致性快照，再写入备份 zip。

启用 SQLite：

```yaml
storage:
  type: sqlite
  sqlite-file: roguelike.db
```

SQLite 数据库会生成在：

```text
plugins/Roguelike/roguelike.db
```

当前不会自动从 JSON 迁移到 SQLite。

玩家数据包含：

- UUID。
- 击杀数。
- 死亡数。
- 总经验。
- 强化券使用次数。
- 超级强化券使用次数。
- 开发券使用次数。
- 移除券使用次数。
- 挖矿累计。
- 吃东西累计。

## 推荐配置组合

小型服务器可以直接使用默认 JSON 和内置侧边栏：

```yaml
storage:
  type: json

scoreboard:
  enabled: true
  update-interval: 20
```

使用 TAB 展示数据时，建议关闭内置侧边栏，避免多个计分板互相覆盖：

```yaml
integrations:
  placeholderapi:
    enabled: true
  tab:
    enabled: true

scoreboard:
  enabled: false
```

使用 MythicMobs 管理怪物生态时：

```yaml
integrations:
  mythicmobs:
    enabled: true
```

启用后，本插件内置精英怪不会自然生成，但仍可保留 Roguelike 经验、装备、词条和战斗系统。
