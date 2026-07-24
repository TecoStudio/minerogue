# 武器模板：`content/weapons/*.yml`

一个 YAML 对应一个武器模板。文件名默认就是 ID，也可在文件内显式写 `id:`。

## 字段表

| 字段 | 类型/示例 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `id` | `flame_sword` | 文件名 | 武器模板 ID。 |
| `item` | `minecraft:diamond_sword` | `id` | 生成的物品材质。剑、斧、三叉戟、弓、工具等都可配置。 |
| `name` | `烈焰之剑` | `id` | 武器显示名称。 |
| `description` | `燃烧敌人的剑` | 空 | lore 描述。 |
| `base-damage` | `10` | `5.0` | 武器基础伤害。实际伤害还会受等级/品质和词条影响。 |
| `attack-speed` | `1.4` | `1.6` | 攻击速度。 |
| `durability` | `800` | `250` | 耐久值。 |
| `rarity` | `epic` | `common` | 品质文本，也影响部分武器开发逻辑。`special` 默认允许突破普通随机词条限制。 |
| `bonus-affix-slots` | `2` | `0` | 额外随机词条槽位数。 |
| `allow-overflow-affixes` | `true` | `rarity: special` 时为 `true`，否则 `false` | 是否允许超出普通随机词条槽限制。 |
| `legendary-affix` | `smash` | 空 | 预留/指定传奇词条 ID。 |
| `effects.<key>` | `effects.fire_damage: 4.0` | 无 | 武器内置效果。怪物 `weapon-template` 会直接使用这里的效果，不再额外附加怪物 YAML 的 legacy poison 字段。 |

## 常用武器效果

| 效果 key | 数值示例 | 适用 | 说明 |
| --- | --- | --- | --- |
| `attack_range` | `3.2` | 武器/弓/工具 | 攻击距离，未写时通常按 `3.0` 处理。 |
| `lifesteal_percent` | `0.10` | 武器/工具 | 按造成伤害百分比吸血。 |
| `lifesteal_flat` | `2.0` | 武器/工具 | 命中固定吸血。 |
| `chain_targets` | `4.0` | 武器/工具 | 连锁伤害目标数。 |
| `chain_range` | `3.5` | 武器/工具 | 连锁搜索范围。 |
| `chain_damage_percent` | `0.45` | 武器/工具 | 连锁伤害倍率。 |
| `crit_chance` | `0.12` | 武器/工具 | 暴击概率。 |
| `crit_damage` | `1.7` | 武器/工具 | 暴击倍率。 |
| `fire_damage` | `4.0` | 武器/工具 | 点燃时追加火焰伤害。 |
| `fire_duration` | `3.0` | 武器/工具 | 燃烧秒数。 |
| `lightning_chance` | `0.10` | 武器/工具 | 命中触发雷击概率。 |
| `slow_duration` | `2.5` | 武器/工具 | 减速秒数。 |
| `slow_level` | `1.0` | 武器/工具 | 减速等级，游戏内显示等级从 I 开始。 |
| `damage_store_percent` | `0.20` | 武器/工具 | 储存本次伤害的一部分，达到次数后爆发。 |
| `damage_store_hit_reduction` | `3.0` | 武器/工具 | 降低伤害储存所需命中次数。 |
| `burning_target_damage_percent` | `0.30` | 武器/工具 | 对燃烧目标增伤。 |
| `poison_chance` | `0.30` | 近战武器 | 命中附加中毒概率。 |
| `poisoned_target_damage_percent` | `0.10` | 武器/工具 | 对中毒目标增伤。 |
| `bleed_chance` | `0.20` | 近战武器 | 命中附加流血概率。 |
| `bleeding_target_damage_percent` | `0.30` | 近战武器 | 对流血目标增伤。 |
| `explosion_chance` | `0.10` | 武器/工具 | 命中触发小爆炸概率。 |
| `big_explosion_chance` | `0.04` | 武器/工具 | 命中触发大爆炸概率。 |
| `victim_explosion_chance` | `0.15` | 近战武器 | 击杀目标时引爆尸体的概率。 |
| `smash` | `1.0` | 武器/工具 | 开关型效果，猛击。 |
| `bomb` | `1.0` | 武器/工具 | 开关型效果，潜行投掷炸弹。 |
| `hyper` | `1.0` | 武器/工具 | 暴击后获得亢奋效果。 |
| `gift` | `1.0` | 武器/工具 | 击杀后获得回复/抗性。 |
| `durability_restore` | `1.0` | 武器/弓/工具 | 概率返还耐久。 |
| `ore_highlight` | `1.0` | 工具 | 挖掘时概率高亮附近矿物。 |
| `scatter_shot` | `2.0`-`5.0` | 弓 | 散射数量；配置值就是总箭数，2/3/4/5 分别表示总计发射 2/3/4/5 支箭，额外箭矢保持与主箭矢相同的速度大小。 |
| `rapid_shot` | `1.0` | 弓 | 连发箭。 |
| `charge_power` | `1.0` | 弓 | 原版满弓后继续蓄能，侧边栏显示额外蓄能进度；命中时在原版箭矢伤害结果上追加蓄能倍率。 |
| `neutral_damage_200` | `1.0` | 武器/弓/工具 | 契约：对敌伤害 200%，受到伤害 200%。 |
| `neutral_speed_200` | `1.0` | 武器/弓/工具 | 契约：移动速度 200%，受到伤害 200%。 |
| `neutral_attack_speed_200` | `1.0` | 武器/弓/工具 | 契约：攻击速度 200%，受到伤害 200%。 |
| `neutral_range_200` | `1.0` | 武器/弓/工具 | 契约：攻击距离 200%，受到伤害 200%。 |
| `neutral_crit_chance_100` | `1.0` | 武器/弓/工具 | 契约：暴击率 +100%，受到伤害 200%。 |
| `neutral_crit_damage_300` | `1.0` | 武器/弓/工具 | 契约：暴击伤害 300%，受到伤害 200%。 |
| `neutral_lifesteal_100` | `1.0` | 武器/弓/工具 | 契约：吸血 +100%，受到伤害 200%。 |
| `neutral_thunder_100` | `1.0` | 武器/弓/工具 | 契约：攻击必定雷击，受到伤害 200%。 |
| `neutral_explosion_100` | `1.0` | 武器/弓/工具 | 契约：攻击必定爆炸，受到伤害 200%。 |
| `neutral_berserk_self_harm` | `1.0` | 武器/弓/工具 | 契约：对敌伤害 300%，每次命中自损最大生命 10%。 |

## 示例

```yaml
id: flame_sword
item: minecraft:diamond_sword
name: 烈焰之剑
description: 燃烧敌人的剑
base-damage: 10
attack-speed: 1.4
durability: 800
rarity: epic
effects:
  attack_range: 3.2
  fire_damage: 4.0
  fire_duration: 3.0
  crit_chance: 0.10
  crit_damage: 1.75
```
