# Roguelike content package

这个目录存放插件内容 YAML 源文件，独立于 `src/`。插件本体默认是空壳，启动时从这里的分类 YAML 加载武器、物品、防具和怪物内容，便于后续只通过新增 YAML 扩展。

## 目录约定

- `weapons/`：一个 YAML 对应一个武器模板。文件名默认就是 ID，也可在文件内显式写 `id:`。
- `items/`：一个 YAML 对应一个物品模板。效果写在该物品文件内的 `effects:`。
- `armor/`：一个 YAML 对应一个防具显示定义。
- `mobs/`：一个 YAML 对应一个怪物相关定义：`type: internal`、`type: modifier`、`type: experience` 或 `type: settings`。`type: internal` 的怪物用英文 multiline `logic:` 组合 vanilla template 与 reusable action block，怪物 ID、别名、属性和战斗编排都留在 YAML。

## 示例

```yaml
id: flame_sword
item: minecraft:diamond_sword
name: 烈焰之剑
description: 燃烧敌人的剑
base-damage: 10
attack-speed: 1.4
durability: 800
rarity: epic
effects:
  fire_damage: 4.0
  fire_duration: 3.0
```

怪物内置战斗逻辑可以通过 `combat-script` 启用/禁用简单动作，例如：

```yaml
type: internal
id: blood-zombie
logic: |
  use template zombie
  if target_far then leap
  else shockwave
aliases:
  - blood_zombie
spawnable: true
```

管理员可将整个目录发布到 GitHub raw 地址，并在 `config.yml` 的 `content.github-sync` 中启用自动拉取。
