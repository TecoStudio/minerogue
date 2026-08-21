# 周期 Boss 事件

周期 Boss 事件把“什么时候刷、刷在哪里、生成哪个 Boss、给什么奖励”集中到 `boss-events.yml`。

能力：按权重选择事件、活动区域保护、Boss 生成位置红色粒子光柱提示和 Boss 掉落奖励。

管理命令：

```text
/rw boss spawn <id>          在当前位置直接生成指定 Boss（仅玩家可用）
/rw boss event status        查看周期 Boss 事件状态
/rw boss event force         立即触发周期 Boss 事件
/rw boss event clear         清除当前活动区域并解除区域保护
/rw boss event next <小时>   调整下次 Boss 事件时间（支持小数，如 0.1）
```

活动区域会阻止挖掘、放置、水桶/岩浆桶和爆炸破坏区域内方块。Boss YAML 只描述 Boss 个体、权重、内置怪物 ID 和掉落；周期、区域、奖励写在 `boss-events.yml`。默认生成位置距在线玩家至少 5 个区块（80 格），可通过 `boss-events.spawn.min-distance-chunks` 调整。

## 生成提示

Boss 事件开始时不会粘贴建筑结构，也不会放置底座、边界标记或其他方块，而是在 Boss 生成中心持续显示红色粒子光柱作为位置标记。Boss 会生成在光柱中心，公告固定显示 Boss ID 和坐标：

```text
xxx 已经苏醒，在 x y z 位置。
```

旧配置中的 `structure` 字段不再读取，也不会触发任何结构生成。删除该字段后执行 `/rw reload` 即可。

## 伤害记分板

战斗中对 Boss 造成伤害的玩家会自动切入 Boss 伤害实时侧边栏，按伤害高低显示排行榜（前 N 名，含自己的名次与总伤害），并随战斗持续刷新。Boss 被击败时另在聊天发送伤害总榜，结束后玩家侧边栏自动恢复为普通状态。该记分板同时覆盖周期事件 Boss 与 `/rw boss spawn` 手动召唤的 Boss。相关配置在 `config.yml` 的 `boss-damage-scoreboard` 段：`enabled`（开关）、`update-interval-ticks`（刷新间隔）、`top-display`（显示名次数）。
