# 配置文件

## 文件布局

运行时目录：

```text
plugins/minerogue/
├─ config.yml
├─ weapons.yml
├─ items.yml
├─ mobs.yml
├─ boss-events.yml
├─ sidebar.yml
├─ content/
│  ├─ weapons/*.yml
│  ├─ items/*.yml
│  ├─ armor/*.yml
│  ├─ mobs/*.yml
│  └─ recipes/forge-recipes.yml
└─ roguelike.db / player_data/
```

`content/` 是推荐维护位置。复制新 jar 不会覆盖已存在的运行时 `content/` 文件；内容变更由 GitHub 同步或手动更新运行时 YAML。铸造配方源为 `content/recipes/forge-recipes.yml`，不随 jar 打包，运行时下载到 `plugins/minerogue/content/recipes/forge-recipes.yml`。

## `config.yml`

关键段落：storage、debug、content.github-sync（包括 `enabled`、`download-recipes`、`base-url`、`files` 和 `overwrite-existing`）、gameplay.exp-multiplier、gameplay.progression-exp-multiplier、gameplay.weapon-drop-multiplier、integrations、scoreboard、resource-pack。

## `config.yml` — 资源包（resource-pack）

控制玩家进服时是否自动下发武器资源包：

```yaml
resource-pack:
  url: "..."                # 资源包 zip 下载地址，留空则关闭下发（玩家需手动安装）
  hash: ""                  # zip 的 SHA1（小写 40 位）；留空时自动从 url + .sha1 拉取，填值则用固定值覆盖
  forced: false            # true = 强制加载，拒绝的玩家会被踢出
  prompt: "..."            # 下发提示语，留空用客户端默认
  declined-message: "..."  # 非强制时玩家拒绝或下载失败的提示
```

默认 `url` 指向 `minerogue-resourcepack` 子模块仓库的每夜构建（`nightly` release，始终只保留最新一份；详见子模块 README）。

**hash 自动同步**：`hash` 留空（默认）时，插件启动与定时（约 30 分钟）会异步从 `url + .sha1`（nightly release 已配套发布的校验文件）拉取最新 SHA1 并缓存进内存，玩家进服时用该值下发。配合每夜构建的「有更新才重建」规则——材质内容未变则 release 与 .sha1 都不变，服务端拉到的 hash 不变，客户端命中缓存秒级应用不重下；材质更新后 .sha1 变化，服务端拉到新 hash，客户端发现变化拉取新版。**全程无需手动维护 hash。** 网络拉取失败时降级为空 hash（客户端每次重下，不卡进服）。若自托管或离线，可在 `hash` 填入固定值跳过网络拉取。

材质回退：若资源包未包含某武器 id 的自定义材质，该武器会自动回退为其 `item` 字段指定的原版基底材质，避免出现紫黑缺失贴图。新增武器 YAML 但尚未绘制贴图时无需额外处理。

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
