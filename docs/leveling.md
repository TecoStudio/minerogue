# 等级与经验

## 经验来源

| 来源 | 说明 |
| --- | --- |
| 击杀怪物 | 按怪物类型和 `gameplay.exp-multiplier` 给经验 |
| 挖矿 | 里程碑式经验，受 `gameplay.progression-exp-multiplier` 影响 |
| 进食 | 里程碑式经验，受 `gameplay.progression-exp-multiplier` 影响 |
| 管理命令 | `/rw exp <数量> [玩家]` |

插件不接管原版经验条，玩家正常获得原版经验。Roguelike 等级与经验通过侧边栏计分板（`sidebar.yml`）和 `/rl status` 查看。

## 升级奖励

每级给强化券；等级 2 或每 3 级给开发券；等级 2 或每 5 级给移除券。

## 统计命令

```text
/rw stats [玩家]
/rw stats top <level|kills|deaths> [数量]
```
