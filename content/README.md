# Roguelike content package

这个目录存放插件内容 YAML 源文件，独立于 `src/`。插件本体默认是空壳，启动时从这里的分类 YAML 加载武器、物品、防具和怪物内容，便于后续只通过新增 YAML 扩展。

## 目录约定

| 目录 | 内容 | 单文件格式 | 说明 |
| --- | --- | --- | --- |
| [`weapons/`](weapons/README.md) | 武器模板 | `content/weapons/<id>.yml` | 一个 YAML 对应一个武器模板。文件名默认就是 ID，也可在文件内显式写 `id:`。 |
| [`items/`](items/README.md) | 物品模板 | `content/items/<id>.yml` | 一个 YAML 对应一个物品模板。效果写在该物品文件内的 `effects:`。 |
| [`armor/`](armor/README.md) | 防具显示定义 | `content/armor/<id>.yml` | 一个 YAML 对应一个防具部件定义。当前只定义显示名称、描述和品质。 |
| [`mobs/`](mobs/README.md) | 怪物内容 | `content/mobs/<id>.yml` | 一个 YAML 对应一个怪物相关定义。通过 `type:` 区分 `internal`、`modifier`、`experience`、`settings`。 |

加载顺序：旧版 `weapons.yml` / `items.yml` / `mobs.yml` 先加载，`content/` 目录后加载；相同 ID 后加载的 YAML 会覆盖先加载内容。修改后执行 `/rw reload` 或重启服务器。

## 通用规则

| 项目 | 写法 | 说明 |
| --- | --- | --- |
| ID | `id: flame_sword` | 内容唯一 ID。单文件 YAML 可省略，省略时使用文件名去掉 `.yml` / `.yaml`。建议只用小写英文、数字、`-`、`_`。 |
| 材质 | `item: minecraft:diamond_sword` | 原版物品 ID。启用 Nova 集成时，武器可写 Nova 物品 ID。 |
| 显示名 | `name: 烈焰之剑` | 游戏内显示名称，可使用颜色代码。 |
| 描述 | `description: 燃烧敌人的剑` | lore 描述文本。 |
| 品质 | `rarity: common` | 常见值：`common`、`rare`、`epic`、`legendary`、`special`。 |
| 概率 | `0.30` | 概率字段统一使用 `0.0` - `1.0`，`0.30` 表示 30%。 |
| 效果 | `effects:` | 武器和物品的数值效果统一写在 `effects.<key>` 下。 |

## GitHub 内容同步

管理员可将整个目录发布到 GitHub raw 地址，并在 `config.yml` 的 `content.github-sync` 中启用自动拉取。`files:` 里填写相对于 `content/` 的路径，例如：

```yaml
content:
  github-sync:
    enabled: true
    base-url: "https://raw.githubusercontent.com/<owner>/<repo>/<branch>/content"
    files:
      - weapons/flame_sword.yml
      - items/healing_potion.yml
      - armor/thorns_helmet.yml
      - mobs/skeleton-elite.yml
    overwrite-existing: true
```
