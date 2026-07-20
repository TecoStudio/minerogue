# 防具显示定义：`content/armor/*.yml`

一个 YAML 对应一个防具部件的 Roguelike 显示定义。防具材质由生成/铸造逻辑决定，这里只维护名称、描述和品质。

## 字段表

| 字段 | 类型/示例 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `id` | `thorns_helmet` | 文件名 | 防具定义 ID。 |
| `name` | `荆棘头盔` | `id` | 游戏内显示名称。 |
| `description` | `受击反伤` | 空 | lore 描述。 |
| `rarity` | `common` | `common` | 品质文本。 |

## 示例

```yaml
id: thorns_helmet
name: 荆棘头盔
description: 受击反伤
rarity: common
```
