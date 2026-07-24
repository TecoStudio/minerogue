# 快速开始

## 构建

```bash
./gradlew.bat build
```

产物：`build/libs/minerogue.jar`。

## 安装

1. 将 `minerogue.jar` 放入 Paper 服务器 `plugins/`。
2. 启动服务器，首次启动会生成 `plugins/Roguelike/`。
3. 修改配置后执行 `/rw reload`。

## 首次验证

控制台或 OP 玩家执行：

```text
/rw list weapons
/rw list items
/rw list armor
/rw affixes
```

玩家执行：`/rl` 和 `/rl tickets`。

## 给自己一件装备

```text
/rw give armor explosive_chestplate <玩家>
/rw give weapon rusty_iron_sword <玩家>
/rw give ticket ticket_b <玩家>
```

也可以执行 `/rw give` 打开图形发放界面。

## 正式服热更新检查

```text
plugman reload Roguelike
rw reload
rw list armor
rw affixes
```

如果改了 `content/armor/*.yml` 这类运行时内容，复制 jar 不会自动覆盖已存在的 `plugins/Roguelike/content/`，需要同步对应 YAML。
