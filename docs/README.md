# Roguelike 文档

本文档描述当前 `minerogue` / `Roguelike` 插件实现。文档按“先部署能跑、再理解玩法、最后改内容”的顺序组织。

## 推荐阅读顺序

1. [快速开始](quick-start.md)：部署、重载、首个验证命令。
2. [游戏内容总览](game-content.md)：服务器循环、装备成长、怪物和 Boss。
3. [装备与防具](equipment.md)：武器、物品、YAML 驱动防具套装。
4. [词条数据](affixes.md)：所有可用词条和原版附魔重复规则。
5. [券系统](tickets.md)：强化/开发/移除如何改变装备。
6. [命令](commands.md)：玩家与管理员命令速查。
7. [配置文件](configuration.md)：所有 YAML 和运行时配置。

## 导航

| 文档 | 内容 |
| --- | --- |
| [quick-start.md](quick-start.md) | 最短部署路径、首玩流程、验证命令 |
| [game-content.md](game-content.md) | 完整玩法总览、内容数量、设计边界 |
| [leveling.md](leveling.md) | 经验来源、升级奖励、死亡惩罚 |
| [equipment.md](equipment.md) | 武器、物品、防具、套装、YAML 字段 |
| [affixes.md](affixes.md) | 武器/弓/工具/防具词条与显示说明 |
| [tickets.md](tickets.md) | 强化券、超级强化券、开发券、工具开发券、移除券 |
| [forge.md](forge.md) | 铸造台结构、GUI、配方格式 |
| [mobs.md](mobs.md) | 普通怪强化、精英怪、Boss、经验与掉落 |
| [boss-events.md](boss-events.md) | 周期 Boss 事件、结构、区域保护 |
| [commands.md](commands.md) | 命令与权限 |
| [configuration.md](configuration.md) | `config.yml` 与 `content/` YAML |

## 当前设计原则

- 插件只负责 Roguelike 成长、装备、怪物和 Boss；经济、菜单、传送、领地等由外部插件组合。
- 任务只做轻量引导，不作为强制主线。
- 内容尽量 YAML 驱动：武器、物品、防具、怪物和 Boss 事件都可在运行时文件中调整。
- 正式服部署 jar 文件名固定为 `minerogue.jar`，Bukkit 插件名仍为 `Roguelike`。
