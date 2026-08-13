# 周期 Boss 事件

周期 Boss 事件把“什么时候刷、刷在哪里、生成哪个 Boss、给什么奖励”集中到 `boss-events.yml`。

能力：按权重选择事件、活动区域保护、Boss 生成位置红色粒子光柱提示和 Boss 掉落奖励。

管理命令：`/rw boss ...`。

活动区域会阻止挖掘、放置、水桶/岩浆桶和爆炸破坏区域内方块。Boss YAML 只描述 Boss 个体、权重、内置怪物 ID 和掉落；周期、区域、奖励写在 `boss-events.yml`。默认生成位置距在线玩家至少 5 个区块（80 格），可通过 `boss-events.spawn.min-distance-chunks` 调整。

## 生成提示

Boss 事件开始时不会粘贴建筑结构，也不会放置底座、边界标记或其他方块，而是在 Boss 生成中心持续显示红色粒子光柱作为位置标记。Boss 会生成在光柱中心，公告固定显示 Boss ID 和坐标：

```text
xxx 已经苏醒，在 x y z 位置。
```

旧配置中的 `structure` 字段不再读取，也不会触发任何结构生成。删除该字段后执行 `/rw reload` 即可。

部署后的完整验证步骤见 [Boss 事件服务器测试](../test/boss-event-server-test.md)。
