# Roguelike 文档

本文档目录按玩法模块拆分，内容基于当前代码实现。

插件运行时名称为 `Roguelike`；构建产物文件名使用 `minerogue-*.jar`。

## 导航

| 文档 | 内容 |
| --- | --- |
| [游戏内容总览](game-content.md) | 核心玩法循环、成长、战斗、装备、券、铸造、怪物、掉落与配置扩展 |
| [快速开始](quick-start.md) | 插件核心循环、基础部署、常用入口 |
| [等级与经验](leveling.md) | 经验来源、经验需求、升级奖励、死亡惩罚 |
| [装备与武器](equipment.md) | 内置武器、内置物品、防具套装、原版附魔兼容 |
| [券系统](tickets.md) | 强化券、超级强化券、开发券、移除券 |
| [铸造台](forge.md) | 铸造台结构、GUI、`forge-recipes.yml` 配方格式 |
| [词条数据](affixes.md) | 当前所有武器/工具/防具词条、重复原版附魔检查 |
| [怪物系统](mobs.md) | 怪物经验、内置精英怪、内置 Boss、普通怪强化、仇恨说明、随机武器掉落 |
| [命令](commands.md) | 玩家命令、管理员命令、权限 |
| [配置文件](configuration.md) | `config.yml`、`weapons.yml`、`items.yml`、`mobs.yml`、`forge-recipes.yml` |
| [配置片段助手](config-tool.html) | 静态网页工具，用于生成武器、物品、精英怪/Boss 难度和普通怪强化 YAML 片段 |

## 快速链接

- 开始游玩：见 [快速开始](quick-start.md)
- 了解完整玩法：见 [游戏内容总览](game-content.md)
- 查看所有词条：见 [词条数据](affixes.md)
- 修改铸造配方：见 [铸造台](forge.md)
- 配置文件说明：见 [配置文件](configuration.md)
- 生成配置片段：打开 [配置片段助手](config-tool.html)
- 维护和本地测试：见根目录 [AGENTS.md](../AGENTS.md)

## 阅读顺序

新服主建议先读 [快速开始](quick-start.md)，再读 [配置文件](configuration.md) 和 [命令](commands.md)。玩家可以先读 [游戏内容总览](game-content.md)、[等级与经验](leveling.md) 和 [券系统](tickets.md)。需要改数值或做发布检查时，再进入对应模块文档。
