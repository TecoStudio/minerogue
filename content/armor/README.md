# 防具显示定义：`content/armor/*.yml`

一个 YAML 对应一个防具部件。防具的显示名、材质、套装 ID、自带词条、套装 lore 和原版自带附魔都从 YAML 读取；代码只负责解释这些字段并执行已知词条效果。

## 字段表

| 字段 | 类型/示例 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `id` | `explosive_chestplate` | 文件名 | 防具定义 ID。 |
| `name` | `炸药胸甲` | `id` | 游戏内显示名称。 |
| `description` | `铜质炸药套部件` | 空 | lore 第一行描述。 |
| `rarity` | `epic` | `common` | 品质文本。 |
| `set` | `explosive` | ID 的第一个 `_` 前缀 | 套装/核心词条 ID。 |
| `piece` | `chestplate` | ID 后缀 | 部位：`helmet`/`chestplate`/`leggings`/`boots`。 |
| `material` | `minecraft:copper_chestplate` | 按套装和部位推断 | 实际物品材质；版本无铜防具时炸药套仍会回退皮革。 |
| `affix` | `explosive` | `set` | 做出来时自带的插件防具词条。 |
| `affix-level` | `1` | 有 affix 时为 1 | 自带词条等级。 |
| `lore` | 字符串列表 | 按 set 内置回退 | 套装效果说明，会显示在“自带词条”之后。 |
| `enchantments` | 映射 | 空 | 原版自带附魔，例如 `blast_protection: 4`、`unbreaking: 4`。 |

## 示例

```yaml
id: explosive_chestplate
name: 炸药胸甲
description: 铜质炸药套部件
rarity: epic
set: explosive
piece: chestplate
material: minecraft:copper_chestplate
affix: explosive
affix-level: 1
lore:
  - "§5炸药: §f受击概率触发TNT爆炸"
  - "§71/2/3/4件: §f25% / 45% / 75% / 100%"
  - "§74件: §f爆炸后获得力量 III"
  - "§7原版自带: §f爆炸保护 IV"
enchantments:
  blast_protection: 4
```
