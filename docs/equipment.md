# 装备与武器

[返回目录](README.md)

## 内置武器模板

| ID                  | 名称       | 物品                      | 伤害 | 攻速 | 耐久 | 品质      | 默认效果                                                                                           |
| ------------------- | ---------- | ------------------------- | ---: | ---: | ---: | --------- | -------------------------------------------------------------------------------------------------- |
| wooden_sword        | 木剑       | minecraft:wooden_sword    |    4 |  1.6 |   59 | common    | 攻击距离 3.0，暴击率 5%，暴击倍率 1.5                                                              |
| flame_sword         | 烈焰之剑   | minecraft:diamond_sword   |   10 |  1.4 |  800 | epic      | 攻击距离 3.2，火焰伤害 4，燃烧 3 秒，暴击率 10%，暴击倍率 1.75                                     |
| vampire_dagger      | 吸血匕首   | minecraft:iron_sword      |    5 |  2.4 |  600 | rare      | 攻击距离 2.8，吸血 15%，暴击率 8%，暴击倍率 1.6                                                    |
| ember_knife         | 余烬短刃   | minecraft:stone_sword     |    5 |  2.0 |  180 | common    | 攻击距离 2.7，火焰伤害 2，燃烧 2 秒，暴击率 8%                                                     |
| frost_cleaver       | 霜裂斧     | minecraft:iron_axe        |   11 |  0.9 |  520 | rare      | 攻击距离 3.0，减速 2.5 秒，减速等级 1，暴击倍率 1.8                                                |
| storm_spear         | 风暴长矛   | minecraft:trident         |    8 |  1.2 |  650 | rare      | 攻击距离 4.0，雷电概率 10%，暴击率 8%                                                              |
| plague_saber        | 疫毒军刀   | minecraft:golden_sword    |    9 |  1.5 |  500 | epic      | 攻击距离 3.0，中毒概率 35%，中毒目标增伤 30%，暴击率 12%，暴击倍率 1.7                             |
| echo_blade          | 回响之刃   | minecraft:diamond_sword   |   10 |  1.3 |  900 | epic      | 攻击距离 3.2，连锁 4 目标，范围 3.5，连锁伤害 45%，暴击率 10%                                      |
| glass_cannon_hammer | 玻璃重锤   | minecraft:netherite_axe   |   20 |  0.7 | 1600 | legendary | 攻击距离 3.1，暴击率 20%，暴击倍率 2.4，伤害储存 20%，储存触发次数减少 4                           |
| thunder_axe         | 雷霆战斧   | minecraft:diamond_axe     |   14 |  0.9 | 1200 | legendary | 攻击距离 3.0，雷电概率 15%，暴击率 12%，暴击倍率 2.0                                               |
| whirlwind_blade     | 旋风之刃   | minecraft:iron_sword      |    9 |  1.3 | 1000 | epic      | 攻击距离 3.1，连锁 3 目标，范围 3.0，连锁伤害 50%，暴击率 10%                                      |
| inferno_greatsword  | 炼狱巨剑   | minecraft:netherite_sword |   18 |  0.8 | 2000 | legendary | 攻击距离 3.5，火焰伤害 6，燃烧 4 秒，连锁 4 目标，范围 4.0，连锁伤害 40%，暴击率 15%，暴击倍率 2.0 |
| phase_scythe        | 相位之镰   | minecraft:iron_hoe        |    7 |  3.2 |  250 | epic      | 攻击距离 3.0，Dash，亢奋 1，暴击率 30%                                                             |
| special_weapon      | 特殊武器   | minecraft:wooden_sword    |    3 |  1.6 |  250 | special   | 攻击距离 3.0                                                                                       |
| rusty_iron_sword    | 生锈的铁剑 | minecraft:iron_sword      |    5 |  1.6 |  250 | common    | 攻击距离 3.0，中毒概率 30%，中毒目标增伤 10%                                                       |
| excited_stone_sword | 石剑       | minecraft:stone_sword     |    5 |  1.6 |  131 | common    | 攻击距离 3.0，暴击率 10%，亢奋 1                                                                   |

怪物默认不会掉落插件物品；服主可在配置中开启按品质随机掉落内置武器。掉落概率见 [怪物系统](mobs.md)。

特殊武器由开发券生成时，会以 `special_weapon` 为模板，但会保留被开发物品原本的伤害、攻击速度和已有攻击距离属性。

## 内置物品

| ID                     | 名称         | 物品                  | 类型   | 品质   | 效果                                     |
| ---------------------- | ------------ | --------------------- | ------ | ------ | ---------------------------------------- |
| healing_potion         | 治疗药水     | minecraft:potion      | potion | common | heal_amount 10                           |
| greater_healing_potion | 强效治疗药水 | minecraft:potion      | potion | rare   | heal_amount 20                           |
| swift_tonic            | 迅捷药剂     | minecraft:potion      | tonic  | rare   | speed_level 1，duration_seconds 12       |
| iron_skin_tonic        | 铁肤药剂     | minecraft:potion      | tonic  | epic   | resistance_level 1，duration_seconds 10  |
| burger                 | 汉堡         | minecraft:player_head | food   | rare   | heal_percent 0.30，full_saturation 1     |

内置物品当前作为可发放和可配置模板保存。`/rw give item` 会按 `item` 字段创建对应材质；药水和药剂模板默认显示为药水物品，并在 lore 或药水元数据中呈现配置效果，仍使用原版饮用交互。汉堡使用玩家头颅贴图，右键直接食用，回复最大生命值的 30%，并补满饱食度和饱和度。

物品名称和品质 lore 会按品质染色，`/rw give` 图形界面会使用真实物品预览，因此可以直接在 GUI 中查看武器属性、防具图标、药水颜色和物品 lore。

## 防具套装

| 套装   | 材质                           | 效果                                                                                          |
| ------ | ------------------------------ | --------------------------------------------------------------------------------------------- |
| 荆棘套 | 锁链                           | 受击反弹基于敌人攻击力的伤害；2/3/4 件为 30%/35%/45%；4 件受击获得亢奋与力量 II               |
| 神速套 | 黄金                           | 每件提供速度等级 +1；自带用不坏 IV；4 件使 Dash 变为 3 充能，冷却 4 秒                        |
| 炸药套 | 铜质，若版本无铜防具则回退皮革 | 受击概率触发受控爆炸伤害；1/2/3/4 件为 25%/45%/75%/100%；伤害随距离衰减，4 件最大约 10.2；自带爆炸保护 IV；4 件爆炸后获得力量 III |

## 原版附魔兼容

当前插件允许 Roguelike 武器同时拥有原版 MC 附魔和插件词条。

| 系统         | 规则                                     |
| ------------ | ---------------------------------------- |
| 附魔台       | Roguelike 武器不再被插件禁止附魔         |
| 铁砧         | Roguelike 武器不再被插件禁止合并附魔     |
| lore         | 武器 lore 会显示原版附魔区块             |
| 插件随机词条 | 当前插件随机词条不再写入原版附魔         |
| 防具词条     | 插件防具词条不再使用原版附魔作为效果载体 |

## 相关文档

- [词条数据](affixes.md)
- [铸造台](forge.md)
- [配置文件](configuration.md)
