# 装备与防具

## 武器

武器模板位于 `content/weapons/*.yml`，包含 `id`、`name`、`item`、`base-damage`、`attack-speed`、`durability`、`rarity` 和 `effects`。

## 物品

| ID | 名称 | 类型 | 说明 |
| --- | --- | --- | --- |
| `healing_potion` | 治疗药水 | potion | 回复固定生命 |
| `greater_healing_potion` | 强效治疗药水 | potion | 更高回复 |
| `swift_tonic` | 迅捷药剂 | tonic | 速度效果 |
| `iron_skin_tonic` | 铁肤药剂 | tonic | 抗性效果 |
| `burger` | 汉堡 | food | 右键食用，回复最大生命 30% 并补满饱食度 |

## YAML 驱动防具

防具位于 `content/armor/*.yml`。防具显示、材质、套装、部位、自带词条、lore 和原版附魔都由 YAML 定义。

```yaml
id: explosive_chestplate
name: 炸药胸甲
description: 铜质炸药套部件
rarity: epic
set: explosive
piece: chestplate
material: minecraft:copper_chestplate
affix: explosive
affix-level: 1
lore:
  - "§5炸药: §f受击概率触发TNT爆炸"
  - "§71/2/3/4件: §f25% / 45% / 75% / 100%"
  - "§74件: §f爆炸后获得力量 III"
  - "§7原版自带: §f爆炸保护 IV"
enchantments:
  blast_protection: 4
```

物品信息会显示 `自带词条: 炸药 (explosive)`，然后显示 YAML 中的 lore 和 `套装: explosive / 部位: chestplate`。

## 防具套装

| 套装 | 自带词条 | 效果 |
| --- | --- | --- |
| 荆棘 | `thorns` | 受击反伤，4 件给速度、急迫、力量 II |
| 神速 | `swift` | 每件速度等级 +1，4 件强化 Dash |
| 炸药 | `explosive` | 受击概率爆炸，4 件爆炸后力量 III |
| 守护 | `guardian` | 每件 6% 插件减伤 |
| 猩红 | `vampire` | 击杀回血，4 件给生命恢复 |
| 雷暴 | `storm` | 攻击概率雷击 |

套装效果按装备身上已记录的插件防具词条计数，而不是只看名字。因此 YAML 中的 `affix` 是套装机制的核心字段。

## 原版附魔兼容

- Roguelike 武器保留并计入原版附魔和跳劈伤害。
- 防具 YAML 可以写 `enchantments`，例如 `unbreaking: 4`、`blast_protection: 4`。
- 与原版一一重复且没有自定义语义的插件防具词条不保留；例如弹射物保护、火焰保护、摔落保护。
- `protection` 保留为原版保护附魔入口；`guardian` 是插件自定义减伤套装词条，两者定位不同。
