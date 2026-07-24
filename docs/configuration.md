# 配置文件

## 文件布局

运行时目录：

```text
plugins/Roguelike/
├─ config.yml
├─ weapons.yml
├─ items.yml
├─ mobs.yml
├─ forge-recipes.yml
├─ boss-events.yml
├─ sidebar.yml
├─ content/
│  ├─ weapons/*.yml
│  ├─ items/*.yml
│  ├─ armor/*.yml
│  └─ mobs/*.yml
└─ roguelike.db / player_data/
```

`content/` 是推荐维护位置。复制新 jar 不会覆盖已存在的运行时 `content/` 文件；内容变更需要同步 YAML 或手动修改运行时文件。

## `config.yml`

关键段落：storage、debug、content.github-sync、gameplay.mana、gameplay.exp-multiplier、gameplay.progression-exp-multiplier、gameplay.weapon-drop-multiplier、integrations、scoreboard。

## 防具 YAML

```yaml
id: guardian_chestplate
name: 守护胸甲
description: 铁质守护套部件
rarity: rare
set: guardian
piece: chestplate
material: minecraft:iron_chestplate
affix: guardian
affix-level: 1
lore:
  - "§a守护: §f每件提供 6% 插件减伤"
  - "§71/2/3/4件: §f6% / 12% / 18% / 24% 减伤"
  - "§7普通“减伤”词条已删除，减伤定位由守护套承担"
enchantments:
  protection: 2
```

防具字段说明见 `content/armor/README.md`。

## 武器、物品、怪物和 Boss

武器写在 `content/weapons/*.yml`，物品写在 `content/items/*.yml`，怪物写在 `content/mobs/*.yml`。周期 Boss 事件写在 `boss-events.yml`。修改后执行 `/rw reload`。
