# 命令

## 玩家命令 `/rl`

| 命令 | 说明 |
| --- | --- |
| `/rl` | 查看自己的状态 |
| `/rl status` | 查看等级、当前等级经验、击杀、死亡 |
| `/rl tickets` | 查看已使用武器券数量 |
| `/rl trade` | 查看自由交易说明 |
| `/rl affixes` | 打开词条图鉴（武器/防具/基础属性词条，悬停查看具体效果） |
| `/rl help` | 玩家帮助 |

## 管理员命令 `/rw` / `/roguelike`

需要权限：`roguelike.admin`。

`/rw debug` 只管理 Roguelike 插件自身登记的调试记录，不是全服事件捕获器。默认排除高频 `PlayerMoveEvent`（对应事件名 `player_move`）；如需记录可在 `debug.exclude-events` 中移除该事件。记录会经过采样率（`sample-rate`）和最小耗时（`min-duration-ms`）过滤，默认分别为 `1.0` 和 `0.0`。每条记录包含时间、`category`、`event`、`message`、`duration_ms` 以及插件按事件提供的标量字段；内存记录默认保留 1000 条，文件默认写入 `plugins/minerogue/debug/`（仅开启 `debug.file.enabled` 后）。

| 命令 | 说明 |
| --- | --- |
| `/rw reload` | 重载配置、内容 YAML、侧边栏等 |
| `/rw backup` | 手动备份玩家数据 |
| `/rw debug <status|on|off|reload|tail [1-50]|stats|clear|flush>` | 管理调试日志、内存记录和统计；`on/off` 也支持 `true/false/enable/disable` 别名，`tail` 数量限制为 1-50（默认 10） |
| `/rw affixes` | 打开词条图鉴 GUI（控制台为聊天列表） |
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
| `/rw boss spawn <id>` | 在当前位置直接生成指定 Boss（仅玩家可用） |
| `/rw boss event status` | 查看周期 Boss 事件状态 |
| `/rw boss event force` | 立即触发周期 Boss 事件 |
| `/rw boss event clear` | 清除当前活动区域并解除区域保护 |
| `/rw boss event next <小时>` | 调整下次 Boss 事件时间（支持小数，如 `0.1`） |
| `/rw fixhand` | 刷新手持武器属性 |
| `/rw help` | 管理员帮助 |

弓箭伤害保留 Bukkit/Paper 原版箭矢伤害，并只应用弓专属链（如散射、连发、蓄能）；弓不会进入近战词条执行链，因此近战暴击、吸血、雷击、爆炸等词条不会因弓可用或通用配置而执行。`Target.ALL` 仍可能作用于弓的非命中属性、被动或受击契约；它不表示弓会执行近战命中效果。手工或旧数据若把仅适用于近战的击杀后置词条配置到弓上，运行时也会跳过该词条。

券 ID：`ticket_a`、`super_ticket_a`、`ticket_b`、`super_ticket_b`、`ticket_c`、`super_ticket_c`。
