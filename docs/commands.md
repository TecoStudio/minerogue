# 命令

## 玩家命令 `/rl`

| 命令 | 说明 |
| --- | --- |
| `/rl` | 查看自己的状态 |
| `/rl status` | 查看等级、当前等级经验、击杀、死亡 |
| `/rl tickets` | 查看已使用武器券数量 |
| `/rl trade` | 查看自由交易说明 |
| `/rl help` | 玩家帮助 |

## 管理员命令 `/rw` / `/roguelike`

需要权限：`roguelike.admin`。

| 命令 | 说明 |
| --- | --- |
| `/rw reload` | 重载配置、内容 YAML、侧边栏等 |
| `/rw backup` | 手动备份玩家数据 |
| `/rw debug <on|off|status>` | 调试日志开关 |
| `/rw affixes` | 列出武器/防具词条 |
| `/rw affixes held [玩家]` | 查看玩家手持 Roguelike 武器词条 |
| `/rw give` | 打开发放 GUI |
| `/rw give weapon <id> [玩家] [数量]` | 发放武器 |
| `/rw give item <id> [玩家] [数量]` | 发放物品 |
| `/rw give armor <id> [玩家] [数量]` | 发放防具 |
| `/rw give ticket <id> [玩家] [数量]` | 发放券 |
| `/rw exp <数量> [玩家]` | 增加 Roguelike 经验 |
| `/rw list <weapons|items|armor>` | 列出可发放 ID |
| `/rw stats [玩家]` | 查看统计 |
| `/rw stats top <level|kills|deaths> [数量]` | 查看排行榜数据 |
| `/rw reset [玩家]` | 重置玩家 Roguelike 数据 |
| `/rw monster spawn <id>` | 生成内置怪物 |
| `/rw boss ...` | 周期 Boss 事件管理 |
| `/rw fixhand` | 刷新手持武器属性 |
| `/rw help` | 管理员帮助 |

券 ID：`ticket_a`、`super_ticket_a`、`ticket_b`、`tool_ticket_b`、`ticket_c`。
