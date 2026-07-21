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
  mana:
    max: 100.0
    regen-per-second: 1.0
  exp-multiplier: 1.0
  progression-exp-multiplier: 1.0
  weapon-drop-multiplier: 0.0
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
| `gameplay.mana.max` | `100.0` | 法力上限。插件会接管原版经验条显示当前法力百分比。 |
| `gameplay.mana.regen-per-second` | `1.0` | 每秒自然恢复法力值。 |
| `gameplay.exp-multiplier` | `1.0` | 怪物击杀经验倍率。`0` 表示关闭击杀经验。 |
| `gameplay.progression-exp-multiplier` | `1.0` | 挖矿/进食里程碑经验倍率。 |
| `gameplay.weapon-drop-multiplier` | `0.0` | 随机武器掉落倍率。默认 `0`，怪物不会掉落插件随机武器；改为 `1.0` 后按基础概率启用。 |
| `gameplay.weapon-drop-chances.*` | 见示例 | 各品质随机武器基础掉落概率，最终概率会乘以 `weapon-drop-multiplier` 并限制在 0-1。 |
| `integrations.placeholderapi.enabled` | `false` | 开启 PlaceholderAPI 集成检测。 |
| `integrations.tab.enabled` | `false` | 开启 TAB 集成检测。通常配合关闭内置侧边栏使用。 |
| `integrations.commandapi.enabled` | `false` | 开启 CommandAPI 兼容检测；未安装时自动回退 Bukkit 命令。 |
| `integrations.mythicmobs.enabled` | `false` | 开启 MythicMobs 兼容。启用后插件内置怪物不会自然生成。 |
| `integrations.nova.enabled` | `false` | 开启 Nova 物品支持，武器 `item` 可写 `nova:<namespace>:<id>`。 |
| `scoreboard.enabled` | `true` | 是否启用插件内置侧边栏。 |
| `scoreboard.update-interval` | `20` | 侧边栏更新间隔，单位 tick，代码中最低限制为 10 tick。 |

## 可编辑内容配置

插件代码本体默认不内置武器、物品、防具或怪物数值；这些内容推荐存放在独立的 `content/` 目录中，方便通过新增 YAML 扩展：

| 目录/文件 | 说明 |
| --- | --- |
| `content/weapons/*.yml` | 武器模板。一个 YAML 对应一个武器，文件名默认作为 ID，也可写 `id:` 覆盖。 |
| `content/items/*.yml` | 物品模板。一个 YAML 对应一个物品，效果配置写在该文件内的 `effects:`。 |
| `content/armor/*.yml` | 防具显示定义。一个 YAML 对应一个防具部件定义。 |
| `content/mobs/*.yml` | 怪物内容。一个 YAML 对应一个怪物经验、普通怪强化、内置精英怪/Boss 或全局怪物设置；怪物 ID、别名、实体模板和动作编排都写在 YAML 中。 |
| `weapons.yml` / `items.yml` / `mobs.yml` | 旧版单文件配置入口，仍会加载，用于兼容或本地覆盖 |
| `forge-recipes.yml` | 铸造台配方 |

这些文件的加载规则：

- 插件先加载空壳默认内容；首次启动会把 jar 内置 `content/` 内容包导出到 `plugins/Roguelike/content/`。
- 之后加载旧版 `weapons.yml`、`items.yml`、`mobs.yml`。
- 再按目录加载 `content/weapons/`、`content/items/`、`content/armor/`、`content/mobs/` 下所有 `.yml` / `.yaml` 文件。推荐新内容使用“一个 YAML 一个定义”的格式；旧的 `weapons:`、`items:`、`armor:`、`experience:`、`modifiers:` 聚合格式仍兼容。旧版 `internal.<id>` 聚合格式也可读取，但新配置推荐迁移到 `content/mobs/<id>.yml`。
- 相同 ID 的武器、物品、防具或怪物配置会由后加载的 YAML 覆盖先加载的内容。
- 修改配置后需要执行 `/rw reload`，或重启服务器。

### GitHub 内容同步

`config.yml` 可配置启动或 `/rw reload` 时从 GitHub raw 地址拉取内容 YAML。该开关默认开启，但 `base-url` 默认为空，因此不会发起网络请求；填入地址后才会同步。

```yaml
content:
  github-sync:
    enabled: true
    base-url: "https://raw.githubusercontent.com/<owner>/<repo>/<branch>/content"
    files:
      - weapons/flame_sword.yml
      - items/healing_potion.yml
      - armor/thorns_helmet.yml
      - mobs/skeleton-elite.yml
    overwrite-existing: true
```

同步目标为 `plugins/Roguelike/content/<相对路径>`。例如 `weapons/flame_sword.yml` 会写入 `plugins/Roguelike/content/weapons/flame_sword.yml`。每新增一个内容 YAML，都需要把相对路径加入 `files:` 列表，除非你用外部脚本生成该列表。

也可以直接打开 [配置片段助手](config-tool.html) 生成武器、物品、精英怪/Boss 难度和普通怪强化 YAML 片段。该页面是随文档附带的静态网页，不需要后端或构建步骤；把生成内容复制进对应的 `plugins/Roguelike/content/<分类>/` 或旧版单文件 YAML 后再执行 `/rw reload`。

修改后执行：

```text
/rw reload
```

### 单文件物品模板

`content/items/<id>.yml` 的物品模板可配置以下字段：

| 字段 | 说明 |
| --- | --- |
| `item` | 原版物品材质 ID，例如 `minecraft:potion` 或 `minecraft:player_head`。`potion` 和 `tonic` 类型默认使用药水材质。 |
| `name` | 显示名称。 |
| `description` | lore 描述文本。 |
| `item-type` | 物品类型，例如 `potion`、`tonic`、`food` 或自定义分类。 |
| `rarity` | 品质文本。 |
| `effects.*` | 自定义效果数值，例如 `heal_amount`、`heal_percent`、`full_saturation`、`speed_level`、`resistance_level`、`duration_seconds`。 |

示例：

```yaml
id: swift_tonic
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

`item-type: food` 可用于右键直接使用的自定义食物。默认汉堡示例使用 `minecraft:player_head`，并通过 `heal_percent: 0.30` 与 `full_saturation: 1.0` 实现回复最大生命值 30%、补满饱食度和饱和度。

### 单文件怪物强化规则

`content/mobs/<id>.yml` 中写 `type: modifier` 可配置普通怪强化规则：

| 字段 | 说明 |
| --- | --- |
| `health-multiplier` | 最大生命倍率。 |
| `damage-multiplier` | 攻击伤害倍率。 |
| `speed-multiplier` | 移动速度倍率。 |
| `weapon-template` | 生成时给予的 Roguelike 武器模板 ID。 |
| `equipment.helmet/chestplate/leggings/boots` | 生成时穿戴的原版装备材质，例如 `minecraft:iron_helmet`。 |
| `equipment.main-hand/off-hand` | 生成时手持的原版物品材质。 |
| `equipment.main-hand-weapon-template/off-hand-weapon-template` | 生成时手持的 Roguelike 武器模板 ID，会覆盖同槽位原版物品。 |
| `equipment.drop-chances.*` | 原版装备槽位掉落率，支持 `helmet`、`chestplate`、`leggings`、`boots`、`main-hand`、`off-hand`。 |
| `drops.held-item-chance` | 死亡时按概率掉落怪物实际主手/副手物品。 |
| `drops.items[]` | 死亡时按概率掉落指定 `weapon-template`、`item-template` 或原版 `material`。 |

示例：

```yaml
type: modifier
id: husk
health-multiplier: 1.25
damage-multiplier: 1.10
speed-multiplier: 0.95
weapon-template: frost_cleaver
equipment:
  helmet: minecraft:iron_helmet
  chestplate: minecraft:iron_chestplate
  leggings: minecraft:chainmail_leggings
  boots: minecraft:iron_boots
  main-hand-weapon-template: frost_cleaver
  drop-chances:
    helmet: 0.01
    chestplate: 0.01
    leggings: 0.01
    boots: 0.01
    main-hand: 0.0
    off-hand: 0.0
drops:
  held-item-chance: 0.0
  items: []
```

内置怪物使用 `type: internal`。代码只提供原版实体模板兜底和可复用 action block；具体怪物不再通过 Java preset, legacy logic fields, or legacy combat scripts 定义。每个怪物 YAML 直接用：

- `template:`：选择原版实体模板，例如 `skeleton`、`zombie`、`spider`。模板负责兜底原版 AI，例如骷髅的弓箭远程逻辑。
- `weapon-template:`：可选，生成时直接给予指定 Roguelike 武器模板。若武器模板自身已经配置 `effects.poison_chance`、`effects.poisoned_target_damage_percent` 等效果，内置怪物直接使用武器内容，不再在怪物 YAML 中额外附加 `poison-chance` 之类的 legacy combat 字段。
- `equipment:`：显式配置防具、主手、副手以及原版槽位掉落率；怪物手持物品不再由 Java 按模板隐式填充。
- `potion-effects:`：显式配置隐身等药水效果；例如蜘蛛精英的隐身也在 YAML 中声明。
- `drops:`：死亡时可按概率掉落实际手持物品或 YAML 指定的武器/物品/原版材质。
- `bossbar:`：是否显示 Boss 血条。精英怪默认不需要 BossBar，Boss 可设置为 `true`。
- `actions:`：按条件组装额外动作块。动作块是通用能力，任何怪物 YAML 都可以复用。

常用 action 条件和动作：

| 字段 | 可用值 | 含义 |
| --- | --- | --- |
| `when` | `target_close` | 目标进入 `skill-range`。 |
| `when` | `target_far` | 目标在 `skill-range` 外、`detect-range` 内。 |
| `when` | `target_detected` | 目标在 `detect-range` 内。 |
| `when` | `after melee-burst` | 用于串接近战连击后的动作。 |
| `do` | `melee-burst` | 近战连击；可用 `hits:` 控制次数。 |
| `do` | `retreat` | 与目标拉开距离，回到模板原版逻辑。 |
| `do` | `leap` | 向目标跃进。 |
| `do` | `shockwave` | 范围伤害和击退。 |
| `do` | `blink` | Teleport behind the target. |
| `do` | `blade-storm` | 近距离范围伤害并附加缓慢。 |
| `do` | `slow-on-hit` | 命中时附加缓慢，可用 `chance`、`duration-seconds`、`level` 控制。 |

动作还可按需填写 `cooldown-ticks`、`speed`、`damage` 覆盖该动作的冷却、位移速度和技能伤害；未写时分别使用怪物的默认技能字段。

骷髅精英示例：完整内容由 YAML 组装。它使用原版骷髅模板兜底；玩家靠近时触发三连砍，然后 `retreat` 拉开距离，让它回到原版骷髅远程逻辑：

```yaml
type: internal
id: skeleton-elite
template: skeleton
aliases:
  - skeleton_elite
enabled: true
spawnable: true
spawn-chance: 0.12
name: '&c骷髅精英'
health: 30.0
damage: 5.0
weapon-template: rusty_iron_sword
equipment:
  helmet: minecraft:chainmail_helmet
  chestplate: minecraft:diamond_chestplate
  leggings: minecraft:chainmail_leggings
  boots: minecraft:diamond_boots
  main-hand: minecraft:bow
  off-hand-weapon-template: rusty_iron_sword
  drop-chances:
    helmet: 0.01
    chestplate: 0.01
    leggings: 0.01
    boots: 0.01
    main-hand: 0.0
    off-hand: 0.0
drops:
  held-item-chance: 0.05
  items:
    - weapon-template: rusty_iron_sword
      chance: 0.15
      amount: 1
detect-range: 18.0
skill-range: 3.2
skill-cooldown-ticks: 100
skill-damage: 5.0
bossbar: false
actions:
  - when: target_close
    do: melee-burst
    hits: 3
  - when: after melee-burst
    do: retreat
```

沸血僵尸示例：同样只用 YAML 组装，使用僵尸模板，远距离跃进，近距离震地：

```yaml
type: internal
id: blood-zombie
template: zombie
aliases:
  - blood_zombie
enabled: true
spawnable: true
name: '&4沸血僵尸'
health: 180.0
damage: 9.0
equipment:
  helmet: minecraft:netherite_helmet
  chestplate: minecraft:netherite_chestplate
  leggings: minecraft:diamond_leggings
  boots: minecraft:netherite_boots
  main-hand: minecraft:diamond_axe
  drop-chances:
    helmet: 0.01
    chestplate: 0.01
    leggings: 0.01
    boots: 0.01
    main-hand: 0.0
    off-hand: 0.0
drops:
  held-item-chance: 0.05
  items: []
detect-range: 24.0
skill-range: 5.0
skill-cooldown-ticks: 120
skill-damage: 8.0
bossbar: true
actions:
  - when: target_far
    do: leap
  - when: target_close
    do: shockwave
```

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
