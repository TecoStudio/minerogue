# Roguelike / minerogue

> 一个面向 Paper 1.21.11 的自由生存肉鸽成长插件：主城提供服务，野外交给玩家，成长交给装备、词条、套装、怪物和 Boss。

Roguelike 不是任务服框架，也不是经济插件。它专注于把 Minecraft 生存服里的“刷怪、探索、换装、强化、冒险”做成一条可持续成长的循环：玩家从主城出发，在野外圈地、生存、刷怪和交易；插件负责提供等级经验、YAML 内容、防具套装、武器词条、券系统、铸造台、内置怪物和周期 Boss 事件。

| 项目 | 当前状态 |
| --- | --- |
| 插件名 | `Roguelike` |
| 生产部署 jar | `minerogue.jar` |
| 服务端 | Paper 1.21.11 |
| Java | Java 25 |
| 内容形态 | YAML 驱动 |
| 存储 | JSON / SQLite |
| 软依赖 | PlaceholderAPI、TAB、CommandAPI、MythicMobs、Nova |

---

## 核心体验

```text
主城服务 / 菜单 / 商店 / 引导
        ↓
出城探索 / 圈地 / 生存 / 刷怪
        ↓
Roguelike 经验 / 升级 / 券奖励
        ↓
开发装备 / 强化词条 / 移除词条
        ↓
防具套装 / 铸造配方 / 精英怪 / 周期 Boss
        ↓
玩家交易 / 自由成长 / 更高风险目标
```

设计边界很明确：任务只是轻量引导和日常目标，不做强制主线；经济、领地、传送、菜单、商店等交给服务器插件栈组合；Roguelike 只负责装备成长和冒险压力。

---

## 已有内容

| 模块 | 数量 / 说明 |
| --- | --- |
| 武器模板 | 27 个，位于 `content/weapons/*.yml` |
| 物品 | 5 个，位于 `content/items/*.yml` |
| 防具 | 24 件，6 套，每套 4 件，位于 `content/armor/*.yml` |
| 防具套装 | 荆棘、神速、炸药、守护、猩红、雷暴 |
| 怪物内容 | 19 个 YAML，覆盖经验、普通怪强化、内置精英怪/Boss |
| 券系统 | 强化券、超级强化券、开发券、工具开发券、移除券 |
| 管理 GUI | `/rw give` 图形化发放武器、物品、防具和券 |

### 防具套装

| 套装 | ID 前缀 | 核心词条 | 定位 |
| --- | --- | --- | --- |
| 荆棘 | `thorns_` | `thorns` | 受击反伤；4 件获得进攻增益 |
| 神速 | `swift_` | `swift` | 每件速度；4 件强化防具 Dash |
| 炸药 | `explosive_` | `explosive` | 受击概率爆炸；4 件爆炸后力量 III |
| 守护 | `guardian_` | `guardian` | 每件 6% 插件减伤；减伤定位由它承担 |
| 猩红 | `vampire_` | `vampire` | 击杀回血；4 件额外生命恢复 |
| 雷暴 | `storm_` | `storm` | 攻击概率雷击 |

防具现在是 YAML 驱动。一个防具文件可以直接决定材质、套装 ID、部位、自带插件词条、词条等级、展示 lore 和原版自带附魔：

```yaml
id: explosive_chestplate
name: 炸药胸甲
description: 铜质炸药套部件
rarity: epic
set: explosive
piece: chestplate
material: minecraft:copper_chestplate
affix: explosive
affix-level: 1
lore:
  - "§5炸药: §f受击概率触发TNT爆炸"
  - "§71/2/3/4件: §f25% / 45% / 75% / 100%"
  - "§74件: §f爆炸后获得力量 III"
  - "§7原版自带: §f爆炸保护 IV"
enchantments:
  blast_protection: 4
```

---

## 构建与部署

```bash
./gradlew.bat build
```

构建产物：

```text
build/libs/minerogue.jar
```

正式服部署约定：

```text
D:\LIPis\Documents\Minecraft\roguelike-production\plugins\minerogue.jar
```

推荐更新流程：

1. 构建 `build/libs/minerogue.jar`。
2. 删除生产服 `plugins/` 下旧的 `minerogue-*.jar` 重复文件。
3. 复制新 jar 为 `plugins/minerogue.jar`。
4. 如果修改了 `content/`，同步到 `plugins/Roguelike/content/...`。
5. RCON 或控制台执行：
   ```text
   plugman reload Roguelike
   rw reload
   ```
6. 检查 `logs/latest.log`，确认没有新的异常。

---

## 命令速查

玩家命令：

```text
/rl
/rl status
/rl tickets
/rl trade
/rl help
```

管理员命令需要 `roguelike.admin`：

```text
/rw reload
/rw backup
/rw debug <on|off|status>
/rw affixes
/rw affixes held [玩家]
/rw give
/rw give weapon <id> [玩家] [数量]
/rw give item <id> [玩家] [数量]
/rw give armor <id> [玩家] [数量]
/rw give ticket <ticket_a|super_ticket_a|ticket_b|tool_ticket_b|ticket_c> [玩家] [数量]
/rw exp <数量> [玩家]
/rw list <weapons|items|armor>
/rw stats [玩家]
/rw stats top <level|kills|deaths> [数量]
/rw reset [玩家]
/rw monster spawn <id>
/rw boss ...
/rw fixhand
/rw help
```

常用验证组合：

```text
/rw list armor
/rw affixes
/rw give armor explosive_chestplate <玩家>
```

---

## 文档

| 文档 | 内容 |
| --- | --- |
| [docs/README.md](docs/README.md) | 文档导航与阅读顺序 |
| [docs/quick-start.md](docs/quick-start.md) | 部署、首玩、常用检查 |
| [docs/game-content.md](docs/game-content.md) | 完整玩法与内容总览 |
| [docs/equipment.md](docs/equipment.md) | 武器、物品、防具套装、YAML 防具字段 |
| [docs/affixes.md](docs/affixes.md) | 武器/工具/弓/防具词条与重复附魔规则 |
| [docs/tickets.md](docs/tickets.md) | 强化券、开发券、工具开发券、移除券 |
| [docs/commands.md](docs/commands.md) | 玩家/管理员命令 |
| [docs/configuration.md](docs/configuration.md) | 配置文件与 YAML 内容格式 |
| [docs/leveling.md](docs/leveling.md) | 经验、升级、死亡惩罚 |
| [docs/forge.md](docs/forge.md) | 铸造台结构和配方 |
| [docs/mobs.md](docs/mobs.md) | 怪物、精英怪、掉落、经验 |
| [docs/boss-events.md](docs/boss-events.md) | 周期 Boss 事件 |

维护者说明见 [AGENTS.md](AGENTS.md)。
