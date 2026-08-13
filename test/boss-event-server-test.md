# Boss 事件服务器测试

本流程用于在本地 Paper 服务器上手工验证周期 Boss 事件的粒子光柱、重载恢复和区域保护。请由服务器操作者启动、停止和热重载服务器。

## 1. 构建与部署

1. 在项目根目录执行：

   ```powershell
   .\gradlew.bat build
   ```

2. 从 `build/libs/` 找到最新的 `minerogue-*.jar`。
3. 在服务器 `plugins/` 目录中删除旧的 `minerogue-*.jar`，避免 Paper 或 PlugManX 加载重复版本。
4. 将最新 jar 复制到服务器 `plugins/` 目录。
5. 启动服务器，或在服务器已运行时由控制台执行：

   ```text
   plugman reload minerogue
   plugins
   rw reload
   ```

6. 检查 `server/logs/latest.log`：应看到 `minerogue` 已启用，且没有新的 `Roguelike` 异常。

## 2. 默认距离与生成公告

1. 使用 OP 玩家进入配置的 Boss 世界 `world`。
2. 确认 `plugins/minerogue/boss-events.yml` 未显式设置 `boss-events.spawn.min-distance-chunks`，或其值为 `5`。
3. 执行：

   ```text
   /rw boss event force
   /rw boss event status
   ```

4. 预期：
   - 聊天栏出现 `<Boss ID> 已经苏醒，在 x y z 位置。`。
   - `/rw boss event status` 显示活动事件、Boss 与坐标。
   - Boss 生成位置与执行命令的玩家水平距离至少约 80 格。

## 3. 红色光柱可见性

1. 按公告坐标向 Boss 位置前进，在距位置超过 32 格时观察天空。
2. 预期：从远处接近时已能看见持续刷新的红色粒子光柱，而非必须走到 Boss 身边才出现。
3. 到达光柱中心后确认：
   - Boss 位于光柱中心附近。
   - 地面没有自动粘贴建筑、底座、边界标记或其他方块。

## 4. 配置重载恢复

1. 保持 Boss 存活并保留光柱。
2. 执行：

   ```text
   /rw reload
   ```

3. 等待至少 2 秒并再次观察光柱。
4. 预期：活动 Boss 未被清除，红色光柱继续显示。
5. 可选：使用 PlugManX 再执行一次：

   ```text
   plugman reload minerogue
   ```

6. 重载完成后等待至少 2 秒。预期活动 Boss 与光柱都恢复。执行 PlugManX 重载前请保持至少一名玩家在 Boss 所在区块附近，确保实体已加载；如果该区块未加载，先让玩家靠近后再判断 Boss 是否仍存活。

## 5. 区域保护

在 `/rw boss event status` 显示的活动区域内，分别尝试：

```text
破坏方块
放置方块
使用水桶或岩浆桶
使用爆炸物破坏方块
```

预期：启用默认保护时，上述操作都不能破坏或改动活动区域内的方块。

## 6. 结束与清理

选择一种方式结束事件：

```text
击杀 Boss
```

或：

```text
/rw boss event clear
```

预期：

- `/rw boss event status` 不再显示活动事件。
- 红色粒子光柱在下一次更新内停止。
- 击杀 Boss 时，聊天栏显示事件结束消息，且按 YAML 配置掉落奖励。
- 再检查 `server/logs/latest.log`，确认没有新的 `Roguelike` 错误或堆栈。

## 结果记录

| 项目 | 结果 | 备注/日志片段 |
| --- | --- | --- |
| 插件启用与 `/rw reload` | 通过 / 失败 | |
| 默认 5 区块（80 格）距离 | 通过 / 失败 | |
| 公告坐标格式 | 通过 / 失败 | |
| 远距红色光柱可见 | 通过 / 失败 | |
| `/rw reload` 后光柱恢复 | 通过 / 失败 | |
| PlugManX 重载后光柱恢复 | 通过 / 失败 / 未测 | |
| 区域保护 | 通过 / 失败 | |
| Boss 结束后光柱停止 | 通过 / 失败 | |
| `latest.log` 无新错误 | 通过 / 失败 | |
