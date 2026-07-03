# 配置文件

[返回目录](README.md)

## 主配置

`plugins/Roguelike/config.yml`

```yaml
storage:
  type: json
  sqlite-file: roguelike.db

debug:
  enabled: false

integrations:
  placeholderapi:
    enabled: false
  tab:
    enabled: false
  commandapi:
    enabled: false
  mythicmobs:
    enabled: false
  nova:
    enabled: false

scoreboard:
  enabled: true
  update-interval: 20
```

## 可编辑内容配置

| 文件 | 说明 |
| --- | --- |
| `weapons.yml` | 武器模板 |
| `items.yml` | 物品模板 |
| `mobs.yml` | 怪物经验、精英怪、怪物强化规则 |
| `forge-recipes.yml` | 铸造台配方 |

修改后执行：

```text
/rw reload
```

## 导出配置

执行：

```text
/rw export
```

导出位置：

```text
plugins/Roguelike/weapons.yml
plugins/Roguelike/items.yml
plugins/Roguelike/mobs.yml
plugins/Roguelike/forge-recipes.yml
plugins/Roguelike/examples
```

## 铸造配方

铸造配方详见 [铸造台](forge.md)。

## 数据存储

默认使用 JSON 保存玩家数据。

启用 SQLite：

```yaml
storage:
  type: sqlite
  sqlite-file: roguelike.db
```

SQLite 数据库会生成在：

```text
plugins/Roguelike/roguelike.db
```

当前不会自动从 JSON 迁移到 SQLite。
