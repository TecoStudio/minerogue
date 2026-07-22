# 怪物系统

[返回目录](README.md)

## 怪物经验

默认普通怪经验：

| 怪物 | 经验 |
| --- | ---: |
| zombie | 15 |
| skeleton | 15 |
| creeper | 20 |
| spider | 12 |
| enderman | 30 |
| blaze | 22 |
| warden | 350 |
| ender_dragon | 500 |
| wither | 300 |
| 其他怪物 | 10 |

## 内置精英怪

插件包含内部精英怪系统。若启用 MythicMobs 集成，内部刷怪逻辑会跳过。

| 精英怪 | 默认生成率 | 名称 | 生命 | 攻击 | 特点 |
| --- | ---: | --- | ---: | ---: | --- |
| 骷髅精英 | 2% | 骷髅精英 | 30 | 5 | 使用原版骷髅弓 AI 作为远程兜底，近身触发三连击后撤退；副手武器模板提供中毒相关效果 |
| 僵尸精英 | 2% | 僵尸精英 | 35 | 5 | 僵尸模板装备全套铁套与亢奋石剑，近战压迫 |
| 精英蜘蛛 | 2% | 精英蜘蛛 | 35 | 3 | 常驻隐身，移速提升，攻击概率附加缓慢 |

精英怪名称不会强制常显，避免玩家隔墙看到名字；准星指向实体时仍可看到其自定义名称。精英怪默认不显示 Boss 血条。

## 内置 Boss

Boss 不会自然生成，管理员可用 `/rw monster spawn <id>` 生成。

| Boss ID | 名称 | 生命 | 攻击 | 特殊技能 | 装备 |
| --- | --- | ---: | ---: | --- | --- |
| `blood-zombie` | 沸血僵尸 | 180 | 9 | `template: zombie` + `actions: target_far/leap, target_close/shockwave` | YAML 显式配置下界合金头盔/胸甲/靴子、钻石护腿、钻石斧 |
| `vagrant` | 流浪者 | 150 | 7 | `template: skeleton` + `actions: target_detected/blink, target_close/blade-storm` | YAML 显式配置骷髅远程兜底装备与时钟副手 |

Boss 名称同样不会强制常显，避免隔墙看到名字；配置 `bossbar: true` 的 Boss 会在玩家靠近检测范围内显示 Boss 血条。血条使用 YAML 中的中文显示名，不会把动作编排展示给玩家。怪物 ID、别名、技能数值、`bossbar:`、`template:` 和 `actions:` 均在 `content/mobs/*.yml` 中调整。

## 普通怪强化规则

`content/mobs/<怪物 ID>.yml` 中 `type: modifier` 可为原版怪物配置生命、攻击、速度倍率和武器模板。内置默认强化如下：

| 怪物 ID | 生命倍率 | 攻击倍率 | 速度倍率 | 武器模板 |
| --- | ---: | ---: | ---: | --- |
| husk | 1.25 | 1.10 | 0.95 | frost_cleaver |
| drowned | 1.10 | 1.05 | 1.05 | storm_spear |
| pillager | 1.15 | 1.15 | 1.00 | echo_blade |
| zombified_piglin | 1.20 | 1.20 | 1.05 | plague_saber |

配置了 `weapon-template` 的普通怪生成时会手持对应 Roguelike 武器；死亡掉落不再写死，而是读取同一个 YAML 的 `drops:`：

- `drops.held-item-chance`：按概率掉落怪物死亡时主手/副手实际持有物品的克隆；默认内容包为 `0.0`。
- `drops.items[]`：按概率额外掉落指定 `weapon-template`、`item-template` 或原版 `material`，可配置 `chance` 与 `amount`；默认内容包为空。
- 因此默认怪物不会掉落插件物品；服主需要插件物品掉落时再显式开启这些字段。

## 骷髅精英仇恨

骷髅精英保留原版骷髅的仇恨判定，不再主动扫描最近玩家。

如果原版目标是创造模式或旁观模式玩家，插件会清空目标并跳过攻击逻辑。

## 随机武器掉落

怪物随机武器掉落默认关闭。将 `gameplay.weapon-drop-multiplier` 从 `0.0` 调高后，怪物死亡时除 YAML 配置掉落外，才会有低概率随机掉落内置武器模板。随机掉落会优先判定高品质，命中某个品质后从该品质的内置武器中随机选择一个。

掉落概率按 `legendary -> epic -> rare -> common` 顺序独立判定。每个品质的最终概率为 `gameplay.weapon-drop-chances.<品质> * gameplay.weapon-drop-multiplier`，并限制在 0-1。

| 品质 | 启用后基础概率 |
| --- | ---: |
| legendary | 0.2% |
| epic | 0.5% |
| rare | 1.0% |
| common | 2.0% |

`special` 品质默认不参与怪物随机掉落。

## 相关文档

- [等级与经验](leveling.md)
- [配置文件](configuration.md)
