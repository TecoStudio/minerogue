# Roguelike Paper 插件

Roguelike 是一个 Paper 服务端玩法插件，提供 Roguelike 武器、词条强化、玩家等级经验、精英怪、铸造台、怪物随机武器掉落和自由交易规则。

插件不提供经济、货币、余额、商店或定价系统。武器、强化券和掉落物的交换方式由服务器规则和玩家自行决定。

> 说明：插件在 `plugin.yml` 中的运行时名称是 `Roguelike`；Gradle 构建出的 jar 文件名使用 `minerogue-<version>-<gitCommit>.jar`。

## 基础要求

- Paper 1.21.11
- Java 25
- 插件 jar 放入服务器 `plugins` 目录
- 首次启动后生成 `plugins/Roguelike/config.yml`
- 首次加载后自动生成可编辑的 `weapons.yml`、`items.yml`、`mobs.yml`、`forge-recipes.yml`

## 构建

推荐使用 Gradle：

```powershell
.\gradlew.bat build
```

构建产物：

```text
build/libs/minerogue-*.jar
```

也可以使用项目自带脚本：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 -TimeoutSeconds 180
```

## 快速部署

1. 构建插件 jar。
2. 将 jar 放入 `plugins`。
3. 启动 Paper 服务端。
4. 根据需要修改 `plugins/Roguelike/config.yml`、`weapons.yml`、`items.yml`、`mobs.yml`、`forge-recipes.yml`。
5. 游戏内执行 `/rw reload` 重载配置。

## 首次使用

- 玩家执行 `/rl` 或 `/rl status` 查看等级、经验、击杀和死亡。
- 玩家通过击杀怪物、挖矿、吃东西获得 Roguelike 经验。
- 升级会获得强化券、开发券或移除券，用于开发和强化装备。
- 管理员执行 `/rw export` 可导出可编辑 YAML 和示例配置。
- 管理员执行 `/rw list weapons`、`/rw list items`、`/rw list armor` 查询可发放 ID。

## 当前主要内容

- Roguelike 武器和词条系统。
- 强化券按武器品质调整成功率和强化量。
- 开发券可把普通物品开发为特殊武器，并保留原物品伤害、攻击速度和已有攻击距离属性。
- 开发券给装备添加词条时使用单按钮随机开发 GUI。
- 移除券可移除武器词条，并提升下次普通强化成功率。
- Roguelike 武器兼容原版 MC 附魔。
- 玩家可通过击杀怪物、挖矿和吃东西获得 Roguelike 经验。
- 伤害聊天显示使用彩色公式，乘法倍率直接显示，鼠标悬停显示完整计算来源。
- 铸造台由“铁砧 + 下方白色羊毛”组成，配方存储在 `forge-recipes.yml`。
- 怪物死亡会按品质低概率掉落内置武器。
- 内置精英怪、普通怪强化和可手动生成的内置 Boss，可在 `mobs.yml` 调整。
- 骷髅精英保留原版骷髅仇恨判定，不主动锁定创造/旁观玩家。

## 可选集成

插件声明了 PlaceholderAPI、TAB、CommandAPI、MythicMobs、Nova 软依赖。未安装这些插件时，Roguelike 会使用自身默认逻辑；启用对应配置后才会尝试集成。

## 文档

完整文档已拆分到 `docs/`：

| 文档 | 内容 |
| --- | --- |
| [文档目录](docs/README.md) | 全部文档入口 |
| [游戏内容总览](docs/game-content.md) | 核心玩法循环、成长、战斗、装备、券、铸造、怪物、掉落与配置扩展 |
| [快速开始](docs/quick-start.md) | 核心循环、基础部署、常用入口 |
| [等级与经验](docs/leveling.md) | 经验需求、升级奖励、死亡惩罚 |
| [装备与武器](docs/equipment.md) | 内置武器、内置物品、防具套装、原版附魔兼容 |
| [券系统](docs/tickets.md) | 强化券、超级强化券、开发券、移除券 |
| [铸造台](docs/forge.md) | 铸造台结构、GUI、`forge-recipes.yml` 配方格式 |
| [词条数据](docs/affixes.md) | 当前全部词条、强化规则、原版附魔重复项检查 |
| [怪物系统](docs/mobs.md) | 怪物经验、内置精英怪、内置 Boss、普通怪强化、随机武器掉落、仇恨说明 |
| [命令](docs/commands.md) | 玩家命令、管理员命令、权限 |
| [配置文件](docs/configuration.md) | `config.yml`、`weapons.yml`、`items.yml`、`mobs.yml`、`forge-recipes.yml` |
| [配置片段助手](docs/config-tool.html) | 静态网页工具，用于生成武器、物品、精英怪/Boss 难度和普通怪强化 YAML 片段 |

面向代码代理和维护者的工作说明见 [AGENTS.md](AGENTS.md)。

## 常用命令

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
/rw reload
/rw export
/rw backup
/rw debug <on|off|status>
/rw affixes
/rw affixes held [玩家]
/rw give
/rw give weapon <id> [玩家] [数量]
/rw give item <id> [玩家] [数量]
/rw give armor <id> [玩家] [数量]
/rw give ticket <ticket_a|super_ticket_a|ticket_b|ticket_c> [玩家] [数量]
/rw exp <数量> [玩家]
/rw list <weapons|items|armor>
/rw stats [玩家]
/rw stats top <level|kills|deaths> [数量]
/rw reset [玩家]
/rw monster spawn <skeleton_elite|zombie_elite|spider_elite|concierge|time_keeper>
/rw fixhand
/rw help
```

管理员权限：

```text
roguelike.admin
```

## 维护与验证

代码或文档变更后，至少运行：

```powershell
.\gradlew.bat build
```

如果修改了 `build.ps1`，再运行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tests\build-script-check.ps1
```
