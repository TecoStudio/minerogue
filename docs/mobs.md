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
| 骷髅精英 | 12% | 骷髅精英 | 30 | 5 | 持剑与精英弓，远程射击、近身突进三连击，攻击附毒，目标中毒时增伤 |
| 僵尸精英 | 12% | 僵尸精英 | 35 | 5 | 铁头盔，亢奋石剑，近战压迫 |
| 精英蜘蛛 | 12% | 精英蜘蛛 | 35 | - | 常驻隐身，移速提升，攻击概率附加缓慢 |

精英怪名称不会强制常显，避免玩家隔墙看到名字；准星指向实体时仍可看到其自定义名称。

## 内置 Boss

Boss 不会自然生成，管理员可用 `/rw monster spawn <id>` 生成。

| Boss ID | 名称 | 生命 | 攻击 | 特殊技能 | 装备 |
| --- | --- | ---: | ---: | --- | --- |
| `blood-zombie` | 沸血僵尸 | 180 | 9 | multiline `logic:`：`use template zombie`；`if target_far then leap`；`else shockwave` | 下界合金头盔/胸甲/靴子、钻石护腿、钻石斧 |
| `vagrant` | 流浪者 | 150 | 7 | multiline `logic:`：`use template skeleton`；`if target_detected then blink`；`if target_close then blade-storm` | 钻石胸甲/靴子、锁链头盔/护腿、下界合金剑、时钟 |

Boss 名称同样不会强制常显，避免隔墙看到名字；玩家靠近检测范围内会看到 Boss 血条。血条使用 YAML 中的中文显示名，不会把逻辑脚本展示给玩家。怪物 ID、别名、技能数值和 `logic:` 均在 `content/mobs/*.yml` 中调整。

## 普通怪强化规则

`content/mobs/<怪物 ID>.yml` 中 `type: modifier` 可为原版怪物配置生命、攻击、速度倍率和武器模板。内置默认强化如下：

| 怪物 ID | 生命倍率 | 攻击倍率 | 速度倍率 | 武器模板 |
| --- | ---: | ---: | ---: | --- |
| husk | 1.25 | 1.10 | 0.95 | frost_cleaver |
| drowned | 1.10 | 1.05 | 1.05 | storm_spear |
| pillager | 1.15 | 1.15 | 1.00 | echo_blade |
| zombified_piglin | 1.20 | 1.20 | 1.05 | plague_saber |

配置了 `weapon-template` 的普通怪死亡时，除随机武器掉落外，还会按现有普通怪武器逻辑低概率掉落该模板武器。

## 骷髅精英仇恨

骷髅精英保留原版骷髅的仇恨判定，不再主动扫描最近玩家。

如果原版目标是创造模式或旁观模式玩家，插件会清空目标并跳过攻击逻辑。

## 随机武器掉落

怪物死亡时有低概率掉落内置武器模板。掉落会优先判定高品质，命中某个品质后从该品质的内置武器中随机选择一个。

掉落概率按 `legendary -> epic -> rare -> common` 顺序独立判定。每个品质的最终概率为 `gameplay.weapon-drop-chances.<品质> * gameplay.weapon-drop-multiplier`，并限制在 0-1。

| 品质 | 掉落概率 |
| --- | ---: |
| legendary | 0.2% |
| epic | 0.5% |
| rare | 1.0% |
| common | 2.0% |

`special` 品质默认不参与怪物随机掉落。

## 相关文档

- [等级与经验](leveling.md)
- [配置文件](configuration.md)
