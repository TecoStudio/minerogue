# 怪物内容：`content/mobs/*.yml`

一个 YAML 对应一个怪物相关定义。通过 `type:` 区分内容类型。

## `type` 类型

| type | 用途 |
| --- | --- |
| `experience` | 设置某个原版怪物击杀经验。 |
| `modifier` | 设置某个原版怪物的生命/攻击/速度倍率和可选武器模板。 |
| `internal` | 定义插件内置精英怪或 Boss。 |
| `settings` | 设置怪物系统全局默认值。 |

## internal 字段

| 字段 | 类型/示例 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `id` | `skeleton-elite` | 文件名 | 内置怪物 ID。 |
| `template` | `skeleton` / `zombie` / `spider` | `zombie` | 原版实体模板。 |
| `aliases` | `- skeleton_elite` | 空 | 命令可接受别名。 |
| `enabled` | `true` | `false` | 是否启用该怪物；禁用后不会自然生成，也不能命令生成。 |
| `spawnable` | `true` | `true` | 是否允许 `/rw monster spawn <id>` 手动生成。 |
| `spawn-chance` | `0.12` | `0.0` | 自然生成转化概率。Boss 不写该字段即可不自然生成。 |
| `bossbar` | `true` / `false` | `false` | 是否显示 Boss 血条。精英怪默认不需要 BossBar。 |
| `name` | `&c骷髅精英` | 空 | 自定义名称。 |
| `health` | `30.0` | `1.0` | 最大生命值。 |
| `damage` | `5.0` | `0.0` | 普通攻击伤害。 |
| `speed-multiplier` | `1.2` | `1.0` | 移动速度倍率。 |
| `weapon-template` | `rusty_iron_sword` | 无 | 装备的 Roguelike 武器模板。 |
| `equipment` | 见下方 | 空 | 显式配置头盔、胸甲、护腿、靴子、主手、副手和各槽位原版掉落率。 |
| `potion-effects` | 见下方 | 空 | 显式配置常驻或限时药水效果，例如蜘蛛隐身。 |
| `drops` | 见下方 | 空 | 死亡时按概率掉落手持物品或 YAML 指定物品。 |
| `detect-range` | `18.0` | `0.0` | 自动寻找目标和血条可见范围。 |
| `skill-range` | `3.2` | `0.0` | 技能距离。 |
| `skill-cooldown-ticks` | `100` | `20` | 默认技能冷却。 |
| `skill-damage` | `5.0` | `0.0` | 默认技能伤害。 |
| `actions` | 见下方 | 空 | 额外动作编排。 |

## actions

| 字段 | 说明 |
| --- | --- |
| `when` | `target_close`、`target_far`、`target_detected`、`after <动作>`、`always`。 |
| `do` / `action` | `melee-burst`、`retreat`、`leap`、`shockwave`、`blink`、`blade-storm`、`slow-on-hit`。 |
| `hits` | `melee-burst` 连击次数。 |
| `chance` | 触发概率，主要用于 hit trigger，如 `slow-on-hit`。 |
| `duration-seconds` | 药水效果持续秒数。 |
| `level` | 药水效果等级，按游戏内显示填写：1 = I。 |
| `cooldown-ticks` | 覆盖该动作冷却；不写时使用怪物 `skill-cooldown-ticks`。 |
| `speed` | 覆盖位移动作速度。 |
| `damage` | 覆盖该动作伤害；不写时使用怪物 `skill-damage`。 |

## equipment / potion-effects / drops

内置怪物不会再在 Java 中按模板硬编码装备。需要的手持物品、防具、药水效果和掉落都应写在 YAML 中。

```yaml
equipment:
  helmet: minecraft:chainmail_helmet
  chestplate: minecraft:diamond_chestplate
  leggings: minecraft:chainmail_leggings
  boots: minecraft:diamond_boots
  main-hand: minecraft:bow
  off-hand-weapon-template: rusty_iron_sword
  drop-chances:
    helmet: 0.01
    chestplate: 0.01
    leggings: 0.01
    boots: 0.01
    main-hand: 0.0
    off-hand: 0.0
potion-effects:
  - type: invisibility
    level: 1
    infinite: true
    ambient: false
    particles: false
drops:
  held-item-chance: 0.0
  items: []
```

- `equipment.<slot>` 填原版材质 ID；`main-hand-weapon-template` / `off-hand-weapon-template` 填 Roguelike 武器模板 ID。
- `equipment.drop-chances.*` 是原版装备槽掉落率；默认内容中手持槽位为 `0.0`，避免与 `drops.held-item-chance` 重复掉落。
- `potion-effects.type` 使用原版药水效果名，例如 `invisibility`；`infinite: true` 表示常驻。
- `drops.held-item-chance` 会按概率掉落怪物死亡时主手/副手实际持有的物品；默认内容包为 `0.0`。
- `drops.items[]` 可填写 `weapon-template`、`item-template` 或 `material`，并用 `chance` / `amount` 控制概率和数量；默认内容包为空，怪物默认不会掉落插件物品。

## 示例

```yaml
type: internal
id: spider-elite
template: spider
enabled: true
spawnable: true
spawn-chance: 0.12
bossbar: false
name: '&5精英蜘蛛'
health: 35.0
damage: 0.0
speed-multiplier: 1.2
equipment:
  drop-chances:
    helmet: 0.0
    chestplate: 0.0
    leggings: 0.0
    boots: 0.0
    main-hand: 0.0
    off-hand: 0.0
potion-effects:
  - type: invisibility
    level: 1
    infinite: true
    ambient: false
    particles: false
drops:
  held-item-chance: 0.0
  items: []
detect-range: 16.0
skill-range: 3.0
skill-cooldown-ticks: 60
skill-damage: 1.2
actions:
  - when: target_close
    do: slow-on-hit
    chance: 0.35
    duration-seconds: 8.0
    level: 1
```
