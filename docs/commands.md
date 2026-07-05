# 命令

[返回目录](README.md)

## 玩家命令

```text
/rl
/rl status
/rl tickets
/rl trade
/rl help
```

| 命令 | 说明 |
| --- | --- |
| `/rl` | 打开玩家状态概览，等同于 `/rl status`。 |
| `/rl status` | 查看当前等级、当前等级经验、击杀数和死亡数。 |
| `/rl tickets` | 查看已经使用过的强化券、超级强化券、开发券和移除券数量。 |
| `/rl trade` | 查看自由交易说明。插件本身不提供金币、余额、价格或商店系统。 |
| `/rl help` | 查看玩家命令帮助。 |

## 管理员命令

```text
/rw export
/rw debug <on|off|status>
/rw affixes
/rw reload
/rw give weapon <id> [玩家] [数量]
/rw give item <id> [玩家] [数量]
/rw give armor <id> [玩家] [数量]
/rw give ticket <ticket_a|super_ticket_a|ticket_b|ticket_c|weapon_development> [玩家] [数量]
/rw exp <数量> [玩家]
/rw list <weapons|items|armor>
/rw stats [玩家]
/rw reset [玩家]
/rw monster spawn <自定义怪物>
/rw fixhand
/rw help
```

`/rw` 也可以使用别名 `/roguelike`。

| 命令 | 说明 |
| --- | --- |
| `/rw export` | 导出当前可编辑 YAML 配置，并导出 TAB、MythicMobs、侧边栏等示例文件。 |
| `/rw debug <on|off|status>` | 开启、关闭或查看开发调试日志状态。 |
| `/rw affixes` | 查看当前可用的基础属性、武器词条和防具词条。 |
| `/rw reload` | 重载 `config.yml`、`weapons.yml`、`items.yml`、`mobs.yml`、`forge-recipes.yml` 等配置，并刷新侧边栏。 |
| `/rw give weapon <id> [玩家] [数量]` | 给予指定武器模板。不给玩家名时，若执行者是玩家则默认给自己。 |
| `/rw give item <id> [玩家] [数量]` | 给予指定自定义物品。 |
| `/rw give armor <id> [玩家] [数量]` | 给予指定防具套装部件。 |
| `/rw give ticket <id> [玩家] [数量]` | 给予指定券。可用 ID 见下方券 ID 表。 |
| `/rw exp <数量> [玩家]` | 给予玩家 Roguelike 经验，可能触发升级奖励。 |
| `/rw list <weapons|items|armor>` | 列出当前加载的武器、物品或防具 ID。 |
| `/rw stats [玩家]` | 查看玩家等级、经验、击杀和死亡。 |
| `/rw reset [玩家]` | 卸载并重新初始化玩家 Roguelike 数据。 |
| `/rw monster spawn <自定义怪物>` | 在执行者位置生成内置自定义怪物。 |
| `/rw fixhand` | 刷新手持 Roguelike 武器的属性和显示。 |
| `/rw help` | 查看管理员命令帮助。 |

## 券 ID

| ID | 显示名 | 用途 |
| --- | --- | --- |
| `ticket_a` | 强化券 | 强化武器或防具词条，成功率随使用次数递减。 |
| `super_ticket_a` | 超级强化券 | 必定成功强化词条。 |
| `ticket_b` | 开发券 | 开发普通物品为特殊武器，或给已有装备随机添加词条。 |
| `ticket_c` | 移除券 | 移除一个插件词条，并可提升武器下次普通强化成功率。 |
| `weapon_development` | 武器开发券 | 将任意非空气、非券、非 Roguelike 武器的物品开发为特殊品质武器。 |

## 常用 ID 查询

| 目标 | 推荐命令 |
| --- | --- |
| 查看武器 ID | `/rw list weapons` |
| 查看物品 ID | `/rw list items` |
| 查看防具 ID | `/rw list armor` |
| 查看词条 ID | `/rw affixes` |
| 查看可生成怪物 | 输入 `/rw monster spawn ` 后使用补全，或查看 [怪物系统](mobs.md)。 |

## 权限

```text
roguelike.admin
```
