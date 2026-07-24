# 券系统

## 强化券 `ticket_a`

打开词条选择 GUI，选择一个可强化词条后按成功率尝试强化。失败消耗券，并为下次普通强化累积 3%-13% 成功率加成。

## 超级强化券 `super_ticket_a`

同样打开强化选择 GUI，但本次强化必定成功。强化量仍按品质区间计算。

## 开发券 `ticket_b`

| 目标 | 效果 |
| --- | --- |
| 普通非 Roguelike 物品 | 开发为 `special_weapon`，保留原物品基础属性与已有攻击距离属性 |
| Roguelike 武器 | 随机添加一个尚未拥有的可用武器词条 |
| 防具 | 随机添加一个尚未拥有的可用防具词条 |

武器已移除随机词条槽位上限，只要还有未拥有的可用词条就能继续开发。

## 工具开发券 `tool_ticket_b`

只能用于 Roguelike 镐或斧，只从工具限定池中添加词条。当前工具限定词条包括 `ore_highlight`。

## 移除券 `ticket_c`

可移除 Roguelike 武器或防具上已记录的插件词条；武器移除还会为下次普通强化成功率 +10%。

## 当前防具词条池

```text
dash
thorns
swift
explosive
guardian
vampire
storm
protection
```

套装做出来时会自带对应核心词条；开发券也可能给防具添加可用防具词条。普通“减伤”词条已删除，减伤定位由守护套承担。

## 已删除防具词条

`damage_reduction`（普通减伤）已删除，减伤定位由守护套 `guardian` 承担。
