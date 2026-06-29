# Roguelike 插件配置说明

Roguelike 是一个 Paper 服务端 Roguelike 玩法插件，支持自定义武器、武器券强化、玩家经验等级、怪物强化、掉落、数据展示和玩家自由交易。

插件不提供经济、货币、余额、商店或定价系统。武器、强化券和怪物掉落物都按玩家之间自行协商的方式交换。

## 基础要求

- Paper 1.21.11
- Java 25
- 插件 jar 放入 `plugins` 目录
- 首次启动后会生成 `plugins/Roguelike/config.yml`
- 默认武器、物品和怪物经验已内置，不需要手动维护 JSON 文件

## 构建

推荐使用项目自带脚本：

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

生成文件：

```text
build/libs/roguelike-mod-1.0.0.jar
```

也可以使用 Gradle：

```powershell
.\gradlew.bat build
```

如果 Gradle 提示找不到 Java 25 toolchain，需要在本机安装 JDK 25，或配置 Gradle toolchain 下载仓库。

## config.yml

默认配置：

```yaml
storage:
  type: json
  sqlite-file: roguelike.db

integrations:
  placeholderapi:
    enabled: true
  tab:
    enabled: false
  commandapi:
    enabled: true
  mythicmobs:
    enabled: true
  nova:
    enabled: true

scoreboard:
  enabled: true
  update-interval: 20
```

## SQLite

默认使用 JSON 文件保存玩家数据：

```yaml
storage:
  type: json
```

如果要启用 SQLite：

```yaml
storage:
  type: sqlite
  sqlite-file: roguelike.db
```

SQLite 数据库会生成在：

```text
plugins/Roguelike/roguelike.db
```

插件会自动创建表：

```sql
player_data(uuid, kills, deaths, total_exp)
```

注意：如果从 JSON 切换到 SQLite，当前代码不会自动迁移旧 JSON 数据，需要手动迁移或重新统计。

## PlaceholderAPI

插件已加入 PlaceholderAPI 兼容检测，并提供内部占位符解析入口。建议配合外部计分板插件使用。

可用占位符语义：

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
%roguelike_tickets_random%
%roguelike_tickets_boost%
%roguelike_tickets_reset%
```

配置：

```yaml
integrations:
  placeholderapi:
    enabled: true
```

如果安装了 PlaceholderAPI，插件启动时会检测并输出集成状态。

## TAB

推荐用 [TAB](https://github.com/NEZNAMY/TAB) 展示 Roguelike 数据，而不是使用插件内置 scoreboard。

配置：

```yaml
integrations:
  tab:
    enabled: true

scoreboard:
  enabled: false
```

启用 TAB 集成后，插件会自动关闭内置 scoreboard，避免与 TAB 抢占玩家计分板。

TAB 示例：

```yaml
scoreboard:
  enabled: true
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

当前版本会检测 CommandAPI 是否安装。未安装时会自动回退到 Bukkit 原生命令系统。

现有命令：

```text
/rl
/rl status
/rl weapon
/rl tickets
/rl trade
/rl help

/rw init
/rw export
/rw debug <on|off|status>
/rw affixes [hand]
/rw reload
/rw give weapon <id> [玩家] [数量]
/rw give item <id> [玩家] [数量]
/rw give ticket <ticket_a|ticket_b|ticket_c|weapon_development> [玩家] [数量]
/rw exp <数量> [玩家]
/rw list <weapons|items>
/rw stats [玩家]
/rw reset [玩家]
/rw monster spawn <类型>
/rw fixhand
/rw help
```

权限：

```text
roguelike.admin
```

## 内置默认内容与外部扩展

默认武器模板、自定义物品和怪物经验/强化规则已内置在插件中。正常部署只需要调整 `config.yml`。

如果需要自定义，可以手动创建以下目录并放入 JSON 文件：

```text
plugins/Roguelike/weapons/*.json
plugins/Roguelike/items/*.json
plugins/Roguelike/mobs/*.json
```

加载规则：

- 插件先加载内置默认内容。
- 再加载外部 JSON。
- 外部 JSON 使用相同 `id` 时会覆盖内置武器或物品。
- 外部 `mobs`/`modifiers` 会覆盖或扩展内置怪物经验和强化规则。

需要参考模板时执行：

```text
/rw export
```

示例会导出到：

```text
plugins/Roguelike/examples
```

这些示例文件不会自动生效，需要时再复制到对应的 Roguelike 扩展目录或第三方插件目录。

## MythicMobs

配置：

```yaml
integrations:
  mythicmobs:
    enabled: true
```

启用后，插件会尝试识别 MythicMobs 生成的怪物，并对其应用 Roguelike 怪物配置。

怪物经验和强化配置默认已内置。需要覆盖或扩展时创建：

```text
plugins/Roguelike/mobs/*.json
```

示例结构：

```json
{
  "default_exp": 10,
  "mobs": {
    "zombie": 15,
    "skeleton": 15
  },
  "modifiers": {
    "zombie": {
      "health_multiplier": 1.5,
      "damage_multiplier": 1.2,
      "weapon_template": "wooden_sword"
    }
  }
}
```

说明：

- `mobs` 控制击杀经验。
- `modifiers` 控制血量、伤害、速度和装备武器。
- `weapon_template` 对应武器模板 id。

## Nova

配置：

```yaml
integrations:
  nova:
    enabled: true
```

武器模板中的 `item` 字段支持 [Nova](https://github.com/xenondevs/Nova) 物品 id：

```json
{
  "id": "flame_sword",
  "item": "nova:customitems:flame_sword",
  "name": "烈焰之剑",
  "description": "燃烧敌人的剑",
  "base_damage": 10,
  "attack_speed": 1.4,
  "durability": 800,
  "rarity": "epic",
  "effects": {
    "fire_damage": 4.0,
    "fire_duration": 3.0
  }
}
```

Nova 物品 id 会去掉前缀后传给 Nova 物品注册表，例如：

```json
"item": "nova:customitems:flame_sword"
```

会尝试读取 Nova 物品：

```text
customitems:flame_sword
```

如果 Nova 未安装，或找不到对应物品，插件会回退到原版材质解析。

## Adventure LegacyComponentSerializer

插件消息系统已经改为 Adventure 官方 `LegacyComponentSerializer`。

支持：

```text
&6金色文本
§c红色文本
&#ff0000十六进制颜色
```

相比手写 MiniMessage 转换，LegacyComponentSerializer 对传统颜色码更稳定，也更适合 Bukkit/Paper 插件配置。

## 内置 Scoreboard

默认开启内置 scoreboard，不安装外部展示插件也可以直接显示 Roguelike 数据。

默认配置：

```yaml
scoreboard:
  enabled: true
  update-interval: 20
```

`update-interval` 单位是 tick，最低会被限制为 10 tick。

如果使用 TAB 或其他计分板插件，建议关闭内置 scoreboard，避免抢占玩家计分板：

```yaml
scoreboard:
  enabled: false
```

管理员可以执行 `/rw init` 自动关闭内置 scoreboard。需要 TAB/MythicMobs 示例时再执行 `/rw export`。

## 自由交易

Roguelike 不维护金币、余额、价格或商店数据，也不会限制玩家如何交换物品。

玩家可以交易的核心物品：

- Roguelike 武器实例
- 强化券 A、B、C
- 怪物掉落的品质武器
- 配置中定义的自定义物品

玩家可用命令查看说明：

```text
/rl trade
```

实际交换方式交给服务器规则决定，例如原版投掷、箱子交付，或服主另行安装的安全交易插件。本插件不会给这些物品附加价格，也不会接入 Vault 类经济系统。

## 武器配置

武器模板默认已内置。需要覆盖或扩展时创建：

```text
plugins/Roguelike/weapons/*.json
```

常用字段：

```json
{
  "id": "wooden_sword",
  "item": "minecraft:wooden_sword",
  "name": "木剑",
  "description": "最基础的武器",
  "base_damage": 4,
  "attack_speed": 1.6,
  "durability": 59,
  "rarity": "common",
  "effects": {
    "crit_chance": 0.05,
    "crit_damage": 1.5
  }
}
```

支持效果字段：

```text
attack_range
lifesteal_percent
lifesteal_flat
slow_duration
slow_level
chain_targets
chain_range
chain_damage_percent
damage_store_percent
damage_store_max
crit_chance
crit_damage
bleed_chance
bleed_damage
bleed_duration
fire_damage
fire_duration
lightning_chance
stun_duration
```

## 武器开发券

`ticket_b` 的显示名为“开发券”。它有两种用途：对普通物品使用时开发成 `special_weapon` 模板对应的特殊品质武器；对已有 Roguelike 武器使用时，三选一添加一个新词条。

`weapon_development` 仍作为兼容旧发放命令的专用武器开发券保留，也可以把任意非 Roguelike 武器、非强化券物品开发成特殊品质武器。

管理员发放：

```text
/rw give ticket weapon_development [玩家] [数量]
/rw give ticket ticket_b [玩家] [数量]
```

使用方法：

- 一只手拿开发券或武器开发券。
- 另一只手拿要开发的任意物品。
- 右键使用后，目标物品保留原材质，变成特殊品质 Roguelike 武器。
- 开发后的武器可以继续使用开发券添加词条，再用强化券强化或重置券重置。

默认特殊武器模板：

```json
{
  "id": "special_weapon",
  "item": "minecraft:wooden_sword",
  "name": "特殊武器",
  "description": "由武器开发券唤醒的武器胚子",
  "base_damage": 3,
  "attack_speed": 1.6,
  "durability": 250,
  "rarity": "special",
  "effects": {
    "attack_range": 3.0
  }
}
```

## 推荐部署组合

### 小型服务器

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

### 中大型服务器

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
