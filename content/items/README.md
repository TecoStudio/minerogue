# 物品模板：`content/items/*.yml`

一个 YAML 对应一个物品模板。文件名默认就是 ID，也可在文件内显式写 `id:`。

## 字段表

| 字段 | 类型/示例 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `id` | `burger` | 文件名 | 物品模板 ID。 |
| `item` | `minecraft:potion` | 按 `item-type` 推断 | 原版物品材质。`potion` / `tonic` 默认使用药水。 |
| `name` | `治疗药水` | `id` | 游戏内显示名称。 |
| `description` | `恢复生命值` | 空 | lore 描述。 |
| `item-type` | `potion` / `tonic` / `food` | `misc` | 物品类别，影响生成和使用逻辑。 |
| `rarity` | `common` | `common` | 品质文本。 |
| `effects.<key>` | `heal_amount: 10.0` | 无 | 数值效果。 |

## 常用效果

| 效果 key | 说明 |
| --- | --- |
| `heal_amount` | 固定回复生命值。 |
| `heal_percent` | 按最大生命百分比回复。 |
| `full_saturation` | 非 0 时补满饱食度和饱和度。 |
| `speed_level` | 速度效果等级。 |
| `resistance_level` | 抗性效果等级。 |
| `duration_seconds` | 药剂/补剂持续秒数。 |

## 示例

```yaml
id: burger
item: minecraft:player_head
name: 汉堡
description: 右键直接食用，回复30%最大生命值并补满饱食度
item-type: food
rarity: rare
effects:
  heal_percent: 0.30
  full_saturation: 1.0
```
