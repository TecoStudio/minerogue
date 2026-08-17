# 铸造台

铸造台是装备加工站，用法类似铁砧：把装备和券（或材料）放进格子，成品槽显示结果，点击成品完成加工。

默认结构：铁砧下方放白色羊毛。玩家右键结构打开铸造 GUI。

## GUI 布局

```text
[ 装备 ]  [ 券/材料 ] [ 券/材料 ] [ 券/材料 ] [ 券/材料 ]
          [        成品槽        ]
```

- 左侧 1 格：装备（武器、防具或工具）
- 右侧 4 格：券或材料
- 成品槽：显示可执行的操作或配方产物，点击完成

## 券加工

右侧放入券后，成品槽根据券与装备的组合显示操作预览：

| 券 | 目标 | 效果 |
| --- | --- | --- |
| 强化券 `ticket_a` | 武器 | 打开词条选择界面，按成功率强化 |
| 超级强化券 `super_ticket_a` | 武器 | 打开词条选择界面，必定成功 |
| 强化券 / 超级强化券 | 防具 | 随机强化一个防具词条 |
| 开发券 `ticket_b` | 普通物品 | 开发为特殊武器 |
| 开发券 `ticket_b` | 武器 | 随机添加一个武器词条 |
| 开发券 `ticket_b` | 防具 | 随机添加一个防具词条 |
| 工具开发券 `tool_ticket_b` | 镐/斧 | 随机添加工具限定词条 |
| 移除券 `ticket_c` | 武器/防具 | 打开词条选择界面，移除一个词条 |

券不再通过手持右键使用，统一放入铸造台。

## 配方合成

右侧只放材料（没有券）时，按 `forge-recipes.yml` 配方合成。

配方格式为 2 行 2 列（共 4 个材料格）：

```yaml
recipes:
  example:
    shape:
      - "TC"
      - "CT"
    ingredients:
      T: minecraft:tnt
      C: minecraft:copper_ingot
    result:
      type: armor
      id: explosive_helmet
      amount: 1
```

- `shape`：2 行，每行 2 个字符，空格表示空槽
- `ingredients`：字符对应材料 ID
- `result.type`：`armor`、`weapon` 或 `material`；`result.id` 为防具 ID、weapons.yml 武器 ID 或原版材料 ID

运行时文件：`plugins/minerogue/forge-recipes.yml`。修改后执行 `/rw reload`。旧版 3x3 配方会在重载时跳过并提示，可删除文件后重载重新导出默认配方。

建议低级装备和药水放在轻量配方中，高价值套装和传奇武器绑定 Boss、事件或稀有材料。
