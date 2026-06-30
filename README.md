# Roguelike Paper 插件

Roguelike 是一个 Paper 服务端玩法插件，提供 Roguelike 武器、词条强化、玩家等级经验、精英怪、怪物经验、自定义掉落展示和玩家自由交易规则。

插件不提供经济、货币、余额、商店或定价系统。武器、强化券和掉落物的交换方式由服务器规则和玩家自行决定。

## 基础要求

- Paper 1.21.11
- Java 25（Gradle 构建使用 Java toolchain 25）
- 插件 jar 放入服务器 `plugins` 目录
- 首次启动后生成 `plugins/Roguelike/config.yml`
- 首次加载后自动生成可编辑的 `weapons.yml`、`items.yml`、`mobs.yml`

## 构建

推荐使用 Gradle：

```powershell
.\gradlew.bat build
```

Gradle 产物命名包含当前 Git 提交短哈希：

```text
build/libs/roguelike-mod-1.0.0-<commit>.jar
```

也可以使用项目自带的本地脚本：

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

脚本产物：

```text
build/libs/roguelike-mod-1.0.0.jar
```

如果 Gradle 提示找不到 Java 25 toolchain，需要安装 JDK 25，或配置 Gradle toolchain 下载源。

## 快速部署

1. 构建插件 jar。
2. 将 jar 放入 `plugins`。
3. 启动 Paper 服务端。
4. 根据需要修改 `plugins/Roguelike/config.yml`、`weapons.yml`、`items.yml`、`mobs.yml`。
5. 游戏内执行 `/rw reload` 重载 Roguelike 配置。

## config.yml

默认配置：

```yaml
storage:
  type: json
  sqlite-file: roguelike.db

debug:
  enabled: false

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

说明：

- `storage.type` 可选 `json` 或 `sqlite`。
- `storage.sqlite-file` 是相对 `plugins/Roguelike` 的 SQLite 文件名。
- `debug.enabled` 开启后会在控制台输出开发调试日志。
- `integrations.*.enabled` 控制外部插件兼容检测与相关行为。
- `scoreboard.enabled` 控制插件内置侧边栏和 Tab 列表展示。
- `scoreboard.update-interval` 单位是 tick，最低会限制为 10 tick。

## 数据存储

默认使用 JSON 保存玩家数据。

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

插件会自动创建或补齐 `player_data` 表，字段包括：

```text
uuid
kills
deaths
total_exp
ticket_a_uses
super_ticket_a_uses
ticket_b_uses
ticket_c_uses
weapon_development_uses
```

注意：从 JSON 切换到 SQLite 时，当前代码不会自动迁移旧 JSON 数据，需要手动迁移或重新统计。

## 命令

玩家命令：

```text
/rl
/rl status
/rl tickets
/rl trade
/rl help
```

管理员命令：

```text
/rw export
/rw debug <on|off|status>
/rw affixes
/rw reload
/rw give weapon <id> [玩家] [数量]
/rw give item <id> [玩家] [数量]
/rw give ticket <ticket_a|super_ticket_a|ticket_b|ticket_c> [玩家] [数量]
/rw exp <数量> [玩家]
/rw list <weapons|items>
/rw stats [玩家]
/rw reset [玩家]
/rw monster spawn <自定义怪物>
/rw fixhand
/rw help
```

`/rw` 也可以使用别名 `/roguelike`。

管理员权限：

```text
roguelike.admin
```

## 配置文件

插件会加载内置默认内容，并在数据目录生成可编辑 YAML：

```text
plugins/Roguelike/weapons.yml
plugins/Roguelike/items.yml
plugins/Roguelike/mobs.yml
```

加载规则：

- 插件先加载内置默认武器、物品和怪物配置。
- 如果 YAML 文件存在，再读取 YAML 覆盖或扩展内置内容。
- `weapons.yml` 中相同武器 id 会覆盖内置武器模板。
- `items.yml` 中相同物品 id 会覆盖内置物品。
- `mobs.yml` 可覆盖内置精英怪、怪物经验和怪物强化规则。
- 修改后执行 `/rw reload` 生效。

导出当前可编辑 YAML 和示例文件：

```text
/rw export
```

导出位置：

```text
plugins/Roguelike/weapons.yml
plugins/Roguelike/items.yml
plugins/Roguelike/mobs.yml
plugins/Roguelike/examples
```

## 武器配置

`weapons.yml` 示例：

```yaml
weapons:
  flame_sword:
    item: minecraft:diamond_sword
    name: 烈焰之剑
    description: 燃烧敌人的剑
    base-damage: 10
    attack-speed: 1.4
    durability: 800
    rarity: epic
    effects:
      attack_range: 3.2
      fire_damage: 4.0
      fire_duration: 3.0
      crit_chance: 0.1
      crit_damage: 1.75
```

常用字段：

```text
item
name
description
base-damage
attack-speed
durability
rarity
effects
```

内置武器模板包括：

```text
wooden_sword
flame_sword
vampire_dagger
thunder_axe
whirlwind_blade
inferno_greatsword
phase_scythe
special_weapon
rusty_iron_sword
excited_stone_sword
```

可用基础词条：

```text
damage
attack_speed
attack_range
```

可用效果词条可通过命令查看：

```text
/rw affixes
```

当前内置效果词条包括：

```text
lifesteal_percent
lifesteal_flat
chain_targets
chain_range
chain_damage_percent
crit_chance
crit_damage
fire_damage
fire_duration
lightning_chance
slow_duration
slow_level
damage_store_percent
damage_store_hit_reduction
burning_target_damage_percent
poisoned_target_damage_percent
poison_chance
explosion_chance
big_explosion_chance
smash
bomb
hyper
gift
dash
```

## 自定义物品

`items.yml` 示例：

```yaml
items:
  healing_potion:
    name: 治疗药水
    description: 恢复生命值
    item-type: potion
    rarity: common
    effects:
      heal_amount: 10
```

管理员发放：

```text
/rw give item healing_potion [玩家] [数量]
```

## 怪物配置

`mobs.yml` 控制插件内置精英怪、怪物经验和怪物强化规则。

示例：

```yaml
internal:
  enabled: true
  skeleton-elite:
    enabled: true
    spawn-chance: 0.12
    name: '&c骷髅精英'
    health: 30.0
    damage: 5.0
    poison-chance: 0.30
    poisoned-damage-bonus: 0.10
    poison-duration-seconds: 5.0
    weapon-template: rusty_iron_sword
  zombie-elite:
    enabled: true
    spawn-chance: 0.12
    name: '&2僵尸精英'
    health: 35.0
    damage: 5.0
    weapon-template: excited_stone_sword

default-experience: 10
experience:
  zombie: 15
  skeleton: 15
  creeper: 20

modifiers:
  zombie:
    health-multiplier: 1.5
    damage-multiplier: 1.2
    speed-multiplier: 1.0
    weapon-template: wooden_sword
```

说明：

- `internal.enabled` 控制插件内置精英怪系统。
- `internal.skeleton-elite` 控制骷髅精英。
- `internal.zombie-elite` 控制僵尸精英。
- `default-experience` 是未单独配置怪物的默认击杀经验。
- `experience` 按实体类型配置击杀经验。
- `modifiers` 按实体类型配置血量、伤害、速度倍率和武器模板。

手动生成内置怪物：

```text
/rw monster spawn skeleton_elite
/rw monster spawn zombie_elite
```

## 强化券

插件内置四类当前可发放券：

```text
ticket_a        强化券：选择并强化一个已有词条，成功率随使用次数递减
super_ticket_a  超级强化券：必定成功的强化券
ticket_b        开发券：开发普通物品为特殊武器，或给已有武器三选一添加新词条
ticket_c        重置券：将一个词条重置到初始状态
```

管理员发放：

```text
/rw give ticket ticket_a [玩家] [数量]
/rw give ticket super_ticket_a [玩家] [数量]
/rw give ticket ticket_b [玩家] [数量]
/rw give ticket ticket_c [玩家] [数量]
```

使用方法：

- 一只手拿强化券，另一只手拿目标 Roguelike 武器。
- `ticket_b` 对普通物品使用时，会将目标物品开发成 `special_weapon` 模板对应的特殊品质武器。
- `ticket_b` 对已有 Roguelike 武器使用时，会打开三选一界面添加新词条。
- `ticket_c` 会打开重置界面，选择要恢复初始值的词条。

玩家升级会自动获得强化券：

- 每升 1 级获得 1 张 `ticket_a`。
- 到达 2 级或 3 的倍数等级时获得 1 张 `ticket_b`。
- 到达 2 级或 5 的倍数等级时获得 1 张 `ticket_c`。

## PlaceholderAPI

配置：

```yaml
integrations:
  placeholderapi:
    enabled: true
```

当前插件提供内部占位符解析入口，并在检测到 PlaceholderAPI 时输出集成提示。实际通过外部计分板使用时，需要配合可调用这些标识符的 PlaceholderAPI 扩展构建。

当前解析标识符：

```text
%roguelike_level%
%roguelike_total_exp%
%roguelike_exp%
%roguelike_exp_next%
%roguelike_kills%
%roguelike_deaths%
%roguelike_weapon_name%
%roguelike_weapon_id%
%roguelike_weapon_damage%
%roguelike_weapon_speed%
%roguelike_tickets_a%
%roguelike_tickets_b%
%roguelike_tickets_c%
```

## TAB

推荐用 [TAB](https://github.com/NEZNAMY/TAB) 展示 Roguelike 数据，而不是同时使用多个计分板插件。

配置：

```yaml
integrations:
  tab:
    enabled: true

scoreboard:
  enabled: false
```

TAB 示例可通过 `/rw export` 导出到：

```text
plugins/Roguelike/examples/tab-scoreboard.yml
```

示例内容：

```yaml
scoreboards:
  roguelike:
    title: '&6&lRoguelike'
    lines:
      - '&7等级: &e%roguelike_level%'
      - '&7经验: &e%roguelike_exp%/%roguelike_exp_next%'
      - '&7击杀: &c%roguelike_kills%'
      - '&7死亡: &4%roguelike_deaths%'
      - '&7武器: &f%roguelike_weapon_name%'
```

## CommandAPI

配置：

```yaml
integrations:
  commandapi:
    enabled: true
```

当前版本会检测 CommandAPI 是否安装；未安装时使用 Bukkit 原生命令系统。

## MythicMobs

配置：

```yaml
integrations:
  mythicmobs:
    enabled: true
```

启用后，插件会尝试识别 MythicMobs 生成的怪物，并对其应用 Roguelike 怪物经验和 `mobs.yml` 中的 `modifiers` 规则。

MythicMobs 示例可通过 `/rw export` 导出到：

```text
plugins/Roguelike/examples/mythicmobs.yml
```

## Nova

配置：

```yaml
integrations:
  nova:
    enabled: true
```

武器模板中的 `item` 字段支持 [Nova](https://github.com/xenondevs/Nova) 物品 id：

```yaml
weapons:
  flame_sword:
    item: nova:customitems:flame_sword
    name: 烈焰之剑
    description: 燃烧敌人的剑
    base-damage: 10
    attack-speed: 1.4
    durability: 800
    rarity: epic
```

`nova:customitems:flame_sword` 会去掉 `nova:` 前缀后传给 Nova 物品注册表，即尝试读取：

```text
customitems:flame_sword
```

如果 Nova 未安装，或找不到对应物品，插件会回退到原版材质解析。

## 内置 Scoreboard

默认开启内置 scoreboard，不安装外部展示插件也可以显示 Roguelike 数据。

```yaml
scoreboard:
  enabled: true
  update-interval: 20
```

如果使用 TAB 或其他计分板插件，建议关闭内置 scoreboard，避免抢占玩家计分板：

```yaml
scoreboard:
  enabled: false
```

## 颜色代码

插件消息系统使用 Adventure `LegacyComponentSerializer`。

支持：

```text
&6金色文本
§c红色文本
&#ff0000十六进制颜色
```

## 自由交易

Roguelike 不维护金币、余额、价格或商店数据，也不会限制玩家如何交换物品。

玩家可以交易的核心物品：

- Roguelike 武器实例
- 强化券、超级强化券、开发券、重置券
- 怪物掉落的品质武器
- 配置中定义的自定义物品

玩家可用命令查看说明：

```text
/rl trade
```

实际交换方式交给服务器规则决定，例如原版投掷、箱子交付，或服主另行安装的安全交易插件。本插件不会给这些物品附加价格，也不会接入 Vault 类经济系统。

## 推荐部署组合

小型服务器：

```yaml
storage:
  type: json

scoreboard:
  enabled: true

integrations:
  placeholderapi:
    enabled: false
  tab:
    enabled: false
```

中大型服务器：

```yaml
storage:
  type: sqlite

integrations:
  placeholderapi:
    enabled: true
  tab:
    enabled: true
  mythicmobs:
    enabled: true
  nova:
    enabled: true

scoreboard:
  enabled: false
```

建议把展示交给 TAB，把怪物生态交给 MythicMobs，把物品外观交给 Nova，Roguelike 插件专注于武器实例、强化券、经验、战斗效果和无货币的自由交易规则。
