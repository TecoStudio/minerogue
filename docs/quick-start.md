# 快速开始

[返回目录](README.md)

## 核心循环

1. 玩家通过击杀怪物、挖矿和吃东西获得 Roguelike 经验。
2. 玩家升级后获得强化券、开发券、移除券等奖励。
3. 使用开发券把普通物品开发成 Roguelike 武器，或给已有 Roguelike 武器随机添加词条。
4. 使用强化券提升武器或防具词条。
5. 使用移除券移除不想要的词条，并为武器下次普通强化提供成功率加成。
6. 使用铸造台合成插件防具、工具和武器。
7. 战斗时聊天栏显示彩色伤害公式，鼠标悬停可查看完整计算来源。
8. 持续刷怪、挖矿、获取经验、强化装备，形成 Roguelike 成长循环。

## 基础要求

- Paper 1.21.11
- Java 25
- 插件 jar 放入服务器 `plugins` 目录。
- 首次启动后生成 `plugins/Roguelike/config.yml`。
- 首次加载后可生成或导出 `weapons.yml`、`items.yml`、`mobs.yml`、`forge-recipes.yml`。

## 构建插件

在项目根目录运行：

```powershell
.\gradlew.bat build
```

构建产物位于：

```text
build/libs/minerogue-*.jar
```

也可以使用带超时的构建脚本：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 -TimeoutSeconds 180
```

## 安装与首次启动

1. 将 `build/libs/minerogue-*.jar` 复制到 Paper 服务端的 `plugins` 目录。
2. 启动服务器，确认控制台出现 Roguelike 启用日志且没有报错。
3. 进入服务器后执行 `/rl` 检查玩家数据是否初始化。
4. 管理员执行 `/rw help` 查看管理命令。
5. 修改配置后执行 `/rw reload`，或重启服务器。

## 常用入口

- 玩家状态：`/rl`
- 查看券统计：`/rl tickets`
- 管理员帮助：`/rw help`
- 重载配置：`/rw reload`
- 查询武器 ID：`/rw list weapons`
- 查询防具 ID：`/rw list armor`

`/rw` 也可以使用别名 `/roguelike`。管理员命令需要 `roguelike.admin` 权限。

## 第一次游玩建议

1. 玩家先通过普通怪、挖矿和吃东西积累经验。
2. 升级获得第一批强化券、开发券和移除券。
3. 使用开发券把常用武器或工具开发成 Roguelike 武器。
4. 通过强化券提升核心词条，通过移除券清理不需要的词条。
5. 建造铸造台，使用服务器配置的配方制作防具或特殊装备。
6. 中后期围绕内置武器、套装和契约词条形成构筑。

## 相关文档

- [游戏内容总览](game-content.md)
- [等级与经验](leveling.md)
- [券系统](tickets.md)
- [铸造台](forge.md)
- [词条数据](affixes.md)
- [命令](commands.md)
- [配置文件](configuration.md)
