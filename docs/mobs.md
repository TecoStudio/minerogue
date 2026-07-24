# 怪物系统

`content/mobs/*.yml` 支持：`experience`（怪物经验）、`modifier`（普通怪强化）、`internal`（内置精英怪/Boss）、`settings`（全局设置）。

管理员可生成：

```text
/rw monster spawn <id>
```

常见 ID：`skeleton-elite`、`zombie-elite`、`spider-elite`、`blood-zombie`、`vagrant`。

怪物默认不掉落插件随机武器。需要启用时调整 `gameplay.weapon-drop-multiplier`。当前正式服路线使用 Roguelike 内置怪物，通常保持 `integrations.mythicmobs.enabled: false`。
