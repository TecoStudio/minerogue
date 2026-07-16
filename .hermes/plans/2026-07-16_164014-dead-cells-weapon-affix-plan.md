# 死亡细胞式武器与词条系统优化方案

> **For Hermes:** 这是方向确认用设计方案，不直接实施代码。确认方向后再拆成实现任务。

**Goal:** 参考《死亡细胞》Wiki 对 Gear / Affixes 的描述，把当前 Roguelike 武器系统从“固定模板 + 无限式开发/强化”优化为“武器等级/品质/槽位/可重铸词条/传奇固定词条”的可成长装备系统。

**Architecture:** 保留现有 `CustomWeapon` 模板、`WeaponInstanceData` 实例数据、`WeaponAffixManager` 词条注册、`TicketManager` 强化券交互，新增一层“装备生成规则 / 品质规则 / 词条槽规则”。先用配置和小模型类落地，不引入复杂外部依赖。

**Tech Stack:** Paper 1.21.11、Java 25、Gradle、Bukkit PDC、YAML 配置。

---

## 参考点：死亡细胞系统可借鉴的核心

基于搜索到的 Wiki 摘要信息：

- Affixes 是随机生成在 Gear 上的特殊属性，不是所有 Affix 都能出现在所有物品上。
- Gear 有等级、品质和强化等级；Gear power 由基础等级 + 品质加成共同决定。
- 品质如 `+`、`++`、`S` 会提高 gear power，并影响伤害提升和词条槽数量。
- Legendary / Colorless 有独立定位：传奇物品可获得强力固定传奇词条，且有“无色/按最高属性缩放”的概念。
- Blacksmith / Apprentice 的核心作用是重铸词条、升级品质，而不是单纯无限堆数值。
- 词条池存在适用限制和协同关系，例如燃烧、油、毒、流血、击杀爆炸等状态链。

当前项目已经有不少基础：

- `src/main/java/com/roguelike/item/CustomWeapon.java`：武器模板，已有基础伤害、攻速、稀有度、effects。
- `src/main/java/com/roguelike/item/WeaponInstanceData.java`：单件武器实例数据，已有 `effectBonuses`、强化券使用次数、失败保底等。
- `src/main/java/com/roguelike/weapon/affix/WeaponAffixManager.java`：已有词条注册、分类、适用目标、流派标签、协同提示。
- `src/main/java/com/roguelike/combat/CombatHandler.java`：已有燃烧、中毒、雷电、爆炸、连锁、暴击、吸血等实际战斗效果。
- `src/main/java/com/roguelike/ticket/TicketManager.java`：已有 A 强化、B 开发、C 移除券和 GUI 交互。
- `src/main/java/com/roguelike/config/DefaultWeapons.java` 与导出的 `weapons.yml`：已有武器模板来源。

---

## 当前系统的问题判断

### 1. 稀有度和成长深度不够分层

当前 `rarity` 主要影响：

- 名字颜色 / 显示名。
- 强化成功率倍率。
- 强化数值倍率。
- 掉落概率配置。

但没有“装备等级”“品质加成”“词条槽数量”的明确关系。因此玩家拿到的普通/稀有/史诗/传奇，本质更像模板预设强弱，而不像死亡细胞那样每件掉落都能有独立 power、quality、affix slots。

### 2. 词条添加是从全局可用池随机抽一个，缺少槽位上限

`TicketManager.getAvailableEffects()` 会从 `AffixManager.weaponEffectIds()` 里拿所有未拥有词条。只要有券，理论上可以继续添加到接近全词条集合。

这会带来：

- 后期装备全能化。
- 词条选择失去构筑取舍。
- 新增词条越多，随机开发越不可控。

### 3. 基础词条、随机词条、传奇词条混在同一个 `effects` 概念里

当前模板 effects 和实例 effectBonuses 叠加：

- 模板自带词条：例如 `flame_sword` 自带火焰。
- 开发券添加词条：也存到 `effectBonuses`。
- 契约类 / toggle 类词条：也用同一套 effect key。

这样短期简单，但后续很难实现：

- “基础武器固定机制不可重铸”。
- “随机词条可重铸”。
- “传奇固定词条不可普通移除”。
- “诅咒/中性高风险词条单独计数”。

### 4. 词条协同已有苗头，但战斗链条不完整

`WeaponAffixManager` 已经注册了：

- `oil_chance`
- `bleed_chance`
- `oiled_target_fire_damage_percent`
- `bleeding_target_damage_percent`
- `victim_explosion_chance`

但 `CombatHandler` 当前实际处理了中毒、火焰、雷电、爆炸、连锁，没有完整处理油浸、流血、击杀爆炸等。Lore 上存在、战斗中不完整，会削弱可信度。

### 5. A/B/C 券含义接近“加数值 / 加词条 / 删除词条”，还没形成铁匠铺闭环

死亡细胞式闭环更像：

- 掉落时随机装备等级和词条。
- 铁匠升级品质。
- 学徒重铸词条。
- 传奇物品有特殊规则。

当前券系统可以复用，但建议重新映射成更清晰的三类服务。

---

## 推荐更新方向：分三期推进

## Phase 1：先补“装备等级 + 品质 + 槽位”基础，不大改战斗

目标：让每件武器实例有独立成长维度，先解决装备全能化和掉落层次问题。

### 设计

在 `WeaponInstanceData` 增加：

- `int gearLevel`：装备等级，类似死亡细胞罗马数字 I、II、III……。
- `String quality`：品质档位，例如 `base`、`plus`、`plusplus`、`s`、`legendary`。
- `List<String> rolledAffixes` 或 `Map<String, Double> rolledAffixes`：随机词条集合，区别于模板固定 effects。
- `String legendaryAffix`：传奇固定词条，可为空。
- `int rerollCount` / `int upgradeCount`：用于限制重铸或品质升级成本。

新增品质规则：

| 品质 | 显示 | power 加成 | 普通随机词条槽 | 说明 |
|---|---:|---:|---:|---|
| base | 无后缀 | +0 | 1 | 普通掉落基础形态 |
| plus | `+` | +2 | 2 | 前期可见成长 |
| plusplus | `++` | +4 | 3 | 中后期核心装备 |
| s | `S` | +6 | 4 | 高级装备，词条完整 |
| legendary | `L` / 传奇 | +3 或 +6 | 3 + 传奇词条 | 固定强力玩法，不一定只是数值更高 |

> 数值可以调整，但建议先保持死亡细胞式“品质影响 power 和词条槽”的结构。

### 项目落点

- 修改 `src/main/java/com/roguelike/item/WeaponInstanceData.java`
  - 增加字段和兼容默认值。
  - 老武器读取时自动补默认 `gearLevel=1`、`quality=base`。
- 修改 `src/main/java/com/roguelike/weapon/WeaponManager.java`
  - `createWeaponStack()` 生成新武器时设置 gearLevel / quality。
  - `updateLore()` 展示：等级、品质后缀、词条槽位。
  - 基础伤害和攻速可由 gearPower 轻微放大。
- 新增或修改配置：
  - `src/main/resources/config.yml` 增加 `gear:` 配置块。
  - 或新建运行时导出 `gear-rules.yml`，但第一期建议先放 `config.yml` 简化。

### 建议公式

```
gearPower = gearLevel + qualityPowerBonus
finalBaseDamage = templateBaseDamage * (1 + gearPower * damagePowerStep)
```

初始推荐：

- `damagePowerStep: 0.08`
- 攻速不按 power 自动成长，避免 Minecraft 手感失控。
- 攻速仍主要由词条或武器模板决定。

### 为什么第一期先做这个

- 改动范围可控。
- 不破坏现有所有战斗词条。
- 立刻改善掉落体验：同一把武器也能有不同等级和品质。
- 为后续“随机词条槽位”和“重铸”打基础。

---

## Phase 2：重构词条池：固定词条 / 随机词条 / 传奇词条 / 诅咒契约分层

目标：让词条系统从“全部 key 随机抽”变成“有规则的构筑池”。

### 词条分类建议

在 `WeaponAffixManager` 当前 `category` 基础上，增加更强的元数据：

| 类型 | 说明 | 示例 | 是否可普通重铸 |
|---|---|---|---|
| intrinsic | 武器模板固定机制 | 烈焰之剑自带 `fire_damage` | 否 |
| normal | 普通随机词条 | 暴击率、吸血、减速 | 是 |
| synergy | 协同词条 | 燃烧增伤、中毒增伤、油浸火焰增伤 | 是，但需权重控制 |
| on_kill | 击杀触发 | 击杀爆炸、击杀回复 | 是 |
| legendary | 传奇专属词条 | 某武器固定传奇能力 | 否或只能传奇重铸 |
| cursed / contract | 高收益高风险契约 | 伤害 200% 但受伤 200% | 单独槽或稀有事件 |
| utility | 工具/功能词条 | Dash、矿物高亮、用不坏 | 是，按物品类型限制 |

### 需要新增的数据结构

建议先新增一个轻量类：

- `src/main/java/com/roguelike/weapon/affix/WeaponAffixDefinition.java`

字段建议：

- `id`
- `displayName`
- `category`
- `poolType`
- `target`
- `weight`
- `minValue`
- `maxValue`
- `integer`
- `strengthenable`
- `exclusiveGroup`
- `requiresAny`
- `forbiddenWith`
- `legendaryOnly`

第一期也可以不把所有旧注册迁移过去，但新增词条抽取逻辑应使用这些元数据。

### 词条槽位规则

普通随机槽只计算 `rolledAffixes`，不计算模板 intrinsic 和 legendaryAffix。

例如：

- `flame_sword` 固定有火焰伤害和燃烧时长，不占随机槽。
- 一把 `flame_sword ++` 可以额外拥有 3 个随机词条。
- 传奇烈焰之剑拥有固定传奇词条“燃烧会蔓延”，再额外拥有普通槽。

### 词条权重建议

把词条分为三个稀有池：

| 池 | 权重方向 | 示例 |
|---|---|---|
| common_pool | 经常出现，构筑基础 | 暴击率、吸血固定、减速、火焰时长 |
| synergy_pool | 需要已有状态或成套出现 | 中毒增伤、燃烧增伤、流血增伤 |
| rare_pool | 强效果或玩法改变 | Dash、猛击、击杀爆炸、契约 |

抽取逻辑：

1. 根据物品类型过滤。
2. 根据已有固定词条和随机词条过滤 `requiresAny` / `forbiddenWith`。
3. 先决定池，再按权重抽。
4. 如果抽不到协同池，则回退普通池。

### 项目落点

- 修改 `src/main/java/com/roguelike/weapon/affix/WeaponAffixManager.java`
  - 增加 `poolType()`、`weight()`、`isRollable()`、`rollAffix()` 等方法。
  - 或拆出 `WeaponAffixRegistry`，但第一轮可以避免大拆。
- 修改 `src/main/java/com/roguelike/ticket/TicketManager.java`
  - `applyTicketB()` 不再从所有 `availableEffects` 均匀随机。
  - 改为检查随机槽是否已满，再按权重开发。
  - `applyTicketC()` 只移除 `rolledAffixes`，不移除模板固定词条或传奇词条。
- 修改 `src/main/java/com/roguelike/weapon/WeaponManager.java`
  - Lore 分区展示：固定机制、随机词条、传奇词条、契约。

---

## Phase 3：补齐状态链和传奇特色，让构筑真的成立

目标：让 Lore 里已有的死亡细胞式协同在战斗中全部可感知。

### 需要补齐的战斗效果

当前 `CombatHandler` 已处理：

- 燃烧。
- 中毒。
- 雷电。
- 爆炸。
- 大爆炸。
- 连锁。
- 暴击。
- 吸血。
- 减速。
- 伤害储存。

建议补齐：

1. **油浸**
   - `oil_chance` 命中时给目标标记油浸状态。
   - 若目标油浸后被点燃，延长燃烧时间或提高火焰伤害。
   - `oiled_target_fire_damage_percent` 只在目标油浸且本次有火焰来源时生效。

2. **流血**
   - `bleed_chance` 命中时给目标流血 DOT。
   - `bleeding_target_damage_percent` 对流血目标增伤。
   - 可以用 Bukkit metadata / PDC-like runtime map 维护短时状态，不必新建实体数据持久化。

3. **击杀触发**
   - `victim_explosion_chance` 应在死亡事件里结算，而不是普通命中时结算。
   - 需要 `EventListener` 或 `CombatHandler` 保留最近一次攻击武器上下文。

4. **传奇固定词条**
   - 每类代表性武器给一个专属传奇词条，而不是单纯更高数值。

### 传奇词条示例

| 武器 | 传奇词条 | 效果方向 |
|---|---|---|
| 烈焰之剑 | 蔓延之焰 | 击杀燃烧目标时点燃附近敌人 |
| 吸血匕首 | 血潮 | 暴击吸血溢出为短暂护盾/抗性 |
| 霜裂斧 | 冰封裂隙 | 对减速目标重击有概率冻结短时 |
| 风暴长矛 | 引雷针 | 雷击后短时间提高下一次雷击概率 |
| 疫毒军刀 | 剧毒扩散 | 中毒目标死亡时给附近敌人上毒 |
| 回响之刃 | 二次回响 | 连锁最后一跳有概率回到主目标 |
| 玻璃重锤 | 破釜 | 低生命时提高暴击伤害，但受伤更高 |

---

## 券系统建议映射

保留现有 A/B/C 入口，避免玩家学习成本过高，但改语义。

### A 券：强化数值 / 升级已存在词条

当前行为基本保留，但建议限制：

- 只能强化基础属性和已存在词条。
- 对 toggle / legendary / cursed 词条通常不可强化。
- 成功率继续受品质影响。
- 高品质装备成功率低，但上限更好。

### B 券：开发随机词条 / 填空槽

改动重点：

- 如果随机槽未满：按词条池权重开发一个随机词条。
- 如果槽已满：提示“随机词条槽已满，请先用 C 券重铸/移除”。
- 不能再无限添加到全词条。

### C 券：重铸 / 移除随机词条

建议拆成两个选择：

- 移除一个随机词条：保留当前逻辑，给下次 A 券成功率加成。
- 重铸一个随机词条：移除选中的 rolled affix，并立即按池规则补一个。

如果要简单落地，第一版 C 券仍只移除；之后再加“重铸模式”。

### 新增可选：品质升级服务

如果要更像死亡细胞铁匠，可新增：

- `ticket_quality` 或铸造台按钮：提升 `base -> + -> ++ -> S`。
- 每件装备限制升级次数或按材料消耗。
- 传奇不能普通升级，需独立来源。

---

## 推荐优先级

### 我建议先选方向 A：稳妥渐进版

1. 增加 `gearLevel / quality / randomAffixSlots`。
2. B 券受槽位限制。
3. Lore 分区展示固定词条和随机词条。
4. 补齐油 / 流血 / 击杀爆炸实际效果。
5. 最后加传奇固定词条。

优点：

- 不需要立刻大改所有词条注册。
- 可兼容旧武器。
- 每一步都能单独测试。

缺点：

- 早期仍保留一些旧字段混用。
- 词条元数据可能会在 Phase 2 再整理一次。

### 备选方向 B：一次性重构版

1. 新建完整 `WeaponAffixDefinition`。
2. 所有词条改成配置化 / 元数据化。
3. `WeaponInstanceData` 明确区分 fixed / rolled / legendary / cursed。
4. TicketManager 全部按新模型重写。

优点：

- 架构干净。
- 后续扩展舒服。

缺点：

- 改动大，容易影响现有战斗和 GUI。
- 测试成本高。

### 备选方向 C：玩法优先版

1. 不动 gearLevel / quality。
2. 先补油、流血、击杀爆炸、传奇词条。
3. 之后再做槽位和品质。

优点：

- 玩家最快感知到新内容。

缺点：

- 无限堆词条的问题仍在。
- 后续再限制槽位可能影响既有存档体验。

---

## 建议的最终结构

### 武器模板：定义“这是什么武器”

来源：`weapons.yml` / `DefaultWeapons.java`

建议字段：

```yaml
weapons:
  flame_sword:
    item: minecraft:diamond_sword
    name: 烈焰之剑
    description: 燃烧敌人的剑
    base-damage: 10
    attack-speed: 1.4
    rarity: epic
    scaling-tags: [暴虐]
    intrinsic-effects:
      fire_damage: 4.0
      fire_duration: 3.0
    legendary-affix: spreading_flame
```

兼容策略：

- 旧 `effects:` 继续可读。
- 新配置优先用 `intrinsic-effects:`。
- 导出配置时逐步迁移。

### 武器实例：定义“这一件武器当前长什么样”

存储在 PDC JSON：

```json
{
  "instanceId": "...",
  "baseWeaponId": "flame_sword",
  "gearLevel": 4,
  "quality": "plusplus",
  "rolledAffixes": {
    "burning_target_damage_percent": 0.28,
    "crit_chance": 0.12,
    "victim_explosion_chance": 0.10
  },
  "legendaryAffix": null,
  "damageBonus": 0.0,
  "attackSpeedBonus": 0.0
}
```

### Lore 展示建议

```text
========== 武器属性 ==========
烈焰之剑 IV++
品质: 史诗 / ++    装备强度: 8
流派: 暴虐 / 战术
─────────────────
⚔ 基础伤害: 16.4
⚡ 攻击速度: 1.40
⬛ 攻击距离: 3.2格
─────────────────
固定机制:
- 火焰: 4.0伤害 (3.0s)
随机词条 3/3:
- 燃烧增伤: 28%
- 暴击: 12% (1.8x)
- 击杀爆炸: 10%
========== 品质: 史诗++ ==========
```

---

## 需要确认的问题

1. 是否要让已有玩家武器自动迁移到新系统？
   - 推荐：是。老武器默认为 `gearLevel=1`、`quality=base`，旧 `effectBonuses` 视为 rolled affixes。

2. 是否接受“B 券不再能无限添加词条”？
   - 推荐：接受。这是构筑取舍的关键。

3. 传奇词条是随机生成，还是每个武器模板固定？
   - 推荐：每个武器模板固定一个传奇词条，更像死亡细胞，也更利于平衡。

4. 品质升级是否通过现有券，还是新增材料/铸造台？
   - 推荐：先用铸造台或新券，不混入 A/B/C，避免语义混乱。

5. 是否要把词条全部配置化？
   - 推荐：短期不全配置化，先 Java 注册 + 少量规则配置；等系统稳定再考虑。

---

## 实施任务草案

### Task 1：为武器实例增加等级、品质、随机槽字段

**Files:**

- Modify: `src/main/java/com/roguelike/item/WeaponInstanceData.java`
- Modify: `src/main/java/com/roguelike/weapon/WeaponManager.java`

**验证:**

- 旧武器读取不报错。
- 新生成武器 Lore 显示等级和品质。
- `./gradlew.bat build` 通过。

### Task 2：增加品质规则配置

**Files:**

- Modify: `src/main/resources/config.yml`
- Modify: `src/main/java/com/roguelike/config/ConfigManager.java`
- Create: `src/main/java/com/roguelike/config/GearQualityConfig.java` 或类似类。

**验证:**

- 配置默认值可读取。
- 修改配置后 `/rw reload` 生效。

### Task 3：限制 B 券开发槽位

**Files:**

- Modify: `src/main/java/com/roguelike/ticket/TicketManager.java`
- Modify: `src/main/java/com/roguelike/weapon/affix/WeaponAffixManager.java`

**验证:**

- 槽未满时可开发。
- 槽满时提示不能继续开发。
- C 券移除后可再次开发。

### Task 4：Lore 分区显示固定 / 随机 / 传奇词条

**Files:**

- Modify: `src/main/java/com/roguelike/weapon/WeaponManager.java`
- Possibly Modify: `src/main/java/com/roguelike/weapon/affix/WeaponAffixManager.java`

**验证:**

- 模板自带 effects 显示在固定机制。
- B 券添加的显示在随机词条。
- 槽位计数正确。

### Task 5：补齐油、流血、击杀爆炸战斗效果

**Files:**

- Modify: `src/main/java/com/roguelike/combat/CombatHandler.java`
- Modify: `src/main/java/com/roguelike/listener/EventListener.java`
- Possibly Create: `src/main/java/com/roguelike/combat/StatusTracker.java`

**验证:**

- `oil_chance` 能给目标油浸状态。
- 油浸 + 火焰触发额外火焰增伤。
- `bleed_chance` 能造成短时 DOT。
- `victim_explosion_chance` 在击杀时触发，而非命中时触发。

### Task 6：传奇词条第一批落地

**Files:**

- Modify: `src/main/java/com/roguelike/weapon/affix/WeaponAffixManager.java`
- Modify: `src/main/java/com/roguelike/combat/CombatHandler.java`
- Modify: `src/main/java/com/roguelike/config/DefaultWeapons.java`
- Possibly Modify YAML export in `ConfigManager.java`

**验证:**

- 传奇武器有固定传奇词条。
- 普通 B/C 券不影响传奇词条。
- 传奇词条实际生效。

---

## 测试 / 验证路线

每个实现阶段至少运行：

```bash
./gradlew.bat build
```

如果改到构建脚本，再运行：

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File ./tests/build-script-check.ps1
```

运行时验证建议：

1. 用户手动启动本地 Paper server。
2. 构建并部署最新 `build/libs/minerogue-*.jar` 到 `server/plugins`。
3. 使用 PlugManX：

```text
plugman reload Roguelike
plugins
rw reload
```

4. 用命令生成测试武器和券：

```text
/rw give weapon flame_sword <player> 1
/rw give ticket ticket_b <player> 4
/rw give ticket ticket_a <player> 4
/rw give ticket ticket_c <player> 2
```

5. 验证：

- 新武器显示等级 / 品质 / 槽位。
- B 券槽满后不能继续添加。
- C 券移除随机词条后槽位恢复。
- A 券能强化已有词条。
- 油 / 流血 / 击杀爆炸在战斗中真实触发。

---

## 风险与处理

### 风险 1：旧武器数据兼容

处理：

- `WeaponInstanceData.fromItemStack()` 读取旧 JSON 时自动补默认字段。
- 不删除旧 `effectBonuses` 字段。
- 迁移期内：旧 effectBonuses 视为随机词条或“遗留强化词条”。

### 风险 2：词条槽位限制影响已有强力武器

处理：

- 已超槽旧武器不强制删除词条，只标记“遗留超槽”。
- 新增开发受槽位限制。
- C 券移除后不再允许补回超过槽位。

### 风险 3：状态 DOT 与 Bukkit 伤害事件递归

处理：

- 继续复用 `CombatHandler.internalDamage` 防止递归。
- DOT 伤害用内部伤害方法。
- runtime status map 定时清理，避免内存泄漏。

### 风险 4：爆炸破坏方块过强

处理：

- 普通击杀爆炸默认不破坏方块。
- 大爆炸 / 契约类才允许高风险破坏，且可在配置里关闭。

---

## 我的推荐结论

建议采用 **方向 A：稳妥渐进版**。

第一轮更新目标不要追求“完整复制死亡细胞”，而是把它最关键的三件事引入当前项目：

1. **品质影响强度和词条槽**：装备之间有掉落层次。
2. **随机词条有槽位和权重**：构筑需要取舍，不会全能化。
3. **状态协同真实生效**：燃烧 / 油 / 毒 / 流血 / 击杀触发形成死亡细胞式 build 链。

如果你确认这个方向，我下一步建议先实现 Phase 1 + Phase 2 的最小闭环：

- `gearLevel / quality / slots`
- B 券槽位限制
- Lore 分区
- 旧武器兼容

然后再补 Phase 3 的战斗状态链。

---

## 用户已确认的更新方向

确认采用以下方向作为后续实现基准：

1. **采用装备等级 / 品质 / 词条槽位体系。**
   - 武器实例需要拥有独立 `gearLevel`、`quality`、随机词条槽位等成长数据。
   - 旧武器需要兼容迁移，不能因为新字段导致读取失败。

2. **采用品质系统，但不完全硬限制词条数量。**
   - 常规武器遵循品质决定随机词条槽位的规则。
   - 允许出现少量“很特别的武器”，可以突破常规槽位或拥有额外特殊词条。
   - 这类武器应通过明确规则控制，例如：
     - `special` 品质；
     - `legendary` 品质；
     - 模板配置 `bonus-affix-slots`；
     - 模板配置 `allow-overflow-affixes`；
     - 特定开发事件或稀有掉落。
   - 不建议让所有武器都可无限堆词条；“突破限制”应成为稀有特性，而不是默认行为。

3. **采用每个武器模板固定专属传奇词条。**
   - 传奇词条不应只是更高数值，而应改变玩法或强化武器主题。
   - 普通 B/C 券不应直接移除或替换传奇词条。
   - 未来如需传奇重铸，应使用独立机制。

4. **采用状态协同，但不做油浸。**
   - 删除或暂缓 `oil_chance`、`oiled_target_fire_damage_percent` 相关设计，因为 Minecraft 中没有直观、自然、好维护的油浸表现形式。
   - 保留并优先补齐流血体系：
     - `bleed_chance`：命中触发流血 DOT；
     - `bleeding_target_damage_percent`：对流血目标增伤；
     - 可扩展“流血导致中毒”“击杀流血目标触发效果”等死亡细胞式协同。
   - 状态链重点调整为：燃烧 / 中毒 / 流血 / 减速 / 击杀触发 / 暴击 / 连锁。

5. **采用保留 A/B/C 券但调整定位的方案。**
   - A 券：强化已有基础属性或已有词条。
   - B 券：开发随机词条槽；常规受槽位限制，但可根据特殊武器规则突破。
   - C 券：移除随机词条；后续可扩展为重铸随机词条。
   - 品质升级后续建议走铸造台或独立新券，不混入 A/B/C 的基础语义。

### 因确认方向产生的实现调整

后续实现时，应把方案优先级调整为：

1. 先实现 `gearLevel / quality / affixSlots / bonusAffixSlots / allowOverflowAffixes` 数据结构。
2. B 券先按槽位开发，但保留“特殊武器可突破”的配置入口。
3. Lore 明确展示：
   - 固定机制；
   - 随机词条 `当前/上限`；
   - 额外特殊槽；
   - 传奇词条；
   - 遗留超槽词条。
4. 从词条池中移除或禁用油浸相关词条，避免玩家抽到暂不生效的词条。
5. 优先补齐流血和击杀触发，而不是油浸。
6. 传奇词条按武器模板固定配置，不作为普通随机词条进入 B 券池。
