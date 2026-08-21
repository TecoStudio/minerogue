# 怪物系统

`content/mobs/*.yml` 支持：`experience`（怪物经验）、`modifier`（普通怪强化）、`internal`（内置精英怪/Boss）、`settings`（全局设置）。

管理员可生成：

```text
/rw monster spawn <id>
```

常见 ID：`skeleton-elite`、`zombie-elite`、`spider-elite`、`blood-zombie`、`vagrant`。

怪物默认不掉落插件随机武器。需要启用时调整 `gameplay.weapon-drop-multiplier`。当前正式服路线使用 Roguelike 内置怪物，通常保持 `integrations.mythicmobs.enabled: false`。

## 精英怪自动清理

每个 `internal` 怪物 YAML 可选 `cleanup:` 配置块，控制精英怪在无人时自动移除，避免遗留实体堆积：

```yaml
cleanup:
  enabled: true
  max-lifetime-ticks: 12000   # 最长存活时间（tick），超时后满足条件即移除
  range: 32.0                  # 附近多少格内有玩家则不清理
  remove-at-morning: true      # 是否在早晨窗口清理
  morning-window-ticks: 1000   # 早晨窗口长度（世界时间 0 起的 tick 数）
```

清理条件：附近 `range` 范围内**无玩家**，且满足以下任一：

- 寿命超过 `max-lifetime-ticks`；
- `remove-at-morning` 为 `true` 且世界时间落在 `0 ~ morning-window-ticks` 早晨窗口内。

周期 Boss（如 `blood-zombie`）默认关闭清理（`enabled: false`）；精英怪（如 `zombie-elite`、`skeleton-elite`、`spider-elite`）默认开启，典型值为 12000 tick 寿命、32 格范围。
