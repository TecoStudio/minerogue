# 铸造台

[返回目录](README.md)

## 结构

铸造台用于合成插件防具、工具和武器。

制作方式：在铁砧下方放一个白色羊毛，即可把该铁砧作为铸造台使用。

```text
铁砧
白色羊毛
```

右键铸造台会打开箱子 GUI。GUI 中间按工作台形式摆出 3x3 输入格，右侧为结果槽。关闭 GUI 会返还输入材料。

## 配方配置

铸造台配方存储在：

```text
plugins/Roguelike/forge-recipes.yml
```

修改后使用 `/rw reload` 重载。

### 配方格式

```yaml
recipes:
  explosive_chestplate:
    shape:
      - "CTC"
      - "TCT"
      - "CTC"
    ingredients:
      C: minecraft:copper_ingot
      T: minecraft:tnt
    result:
      type: armor
      id: explosive_chestplate
      amount: 1
```

字段说明：

| 字段              | 说明                                   |
| ----------------- | -------------------------------------- |
| `shape`         | 必须 3 行，每行 3 个字符；空格表示空槽 |
| `ingredients`   | 字符到原版材料 ID 的映射               |
| `result.type`   | `armor`、`weapon`、`material`    |
| `result.id`     | 防具 ID、武器 ID 或原版材料 ID         |
| `result.amount` | 产物数量                               |

## 默认炸药套配方

炸药套每个部件都需要 4 个 TNT，全套共需要 16 个 TNT。`C` 表示铜锭，`T` 表示 TNT，空格表示空槽。

炸药头盔：

```text
T C T
C C C
T C T
```

炸药胸甲：

```text
C T C
T C T
C T C
```

炸药护腿：

```text
T C T
C   C
T C T
```

炸药靴子：

```text
C   C
T   T
T   T
```

## 默认武器配方

新增内置武器也会随默认 `forge-recipes.yml` 导出铸造配方。每个配方中间的 `W` 表示对应原版武器，另外两个材料作为强化核心。

| 产物 ID | W 材料 | 上方材料 | 下方材料 |
| --- | --- | --- | --- |
| `ember_knife` | minecraft:stone_sword | minecraft:flint | minecraft:stick |
| `frost_cleaver` | minecraft:iron_axe | minecraft:blue_ice | minecraft:iron_block |
| `storm_spear` | minecraft:trident | minecraft:copper_ingot | minecraft:redstone |
| `plague_saber` | minecraft:golden_sword | minecraft:spider_eye | minecraft:gold_ingot |
| `echo_blade` | minecraft:diamond_sword | minecraft:amethyst_shard | minecraft:diamond |
| `glass_cannon_hammer` | minecraft:netherite_axe | minecraft:netherite_ingot | minecraft:diamond_block |

武器配方形状均为：

```text
核心材料
原版武器
下方材料
```

## 相关文档

- [装备与武器](equipment.md)
- [配置文件](configuration.md)
