package com.roguelike.command;

import com.roguelike.config.ConfigManager;
import com.roguelike.forge.ForgeRecipeManager;
import com.roguelike.util.Message;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class ExportCommand {
    boolean handleExport(CommandSender sender) {
        var plugin = ConfigManager.getPlugin();
        Path examples = plugin.getDataFolder().toPath().resolve("examples");

        try {
            ConfigManager.exportEditableYaml();
            ForgeRecipeManager.exportEditableYaml();
            writeFile(examples.resolve("weapons.yml"), "weapons:\n" +
                    "  flame_sword:\n" +
                    "    item: minecraft:diamond_sword\n" +
                    "    name: 烈焰之剑\n" +
                    "    description: 燃烧敌人的剑\n" +
                    "    base-damage: 10\n" +
                    "    attack-speed: 1.4\n" +
                    "    durability: 800\n" +
                    "    rarity: epic\n" +
                    "    effects:\n" +
                    "      attack_range: 3.2\n" +
                    "      fire_damage: 4.0\n" +
                    "      fire_duration: 3.0\n" +
                    "      crit_chance: 0.1\n" +
                    "      crit_damage: 1.75\n" +
                    "  excited_stone_sword:\n" +
                    "    item: minecraft:stone_sword\n" +
                    "    name: 石剑\n" +
                    "    description: 僵尸精英使用的亢奋石剑\n" +
                    "    base-damage: 5\n" +
                    "    attack-speed: 1.6\n" +
                    "    durability: 131\n" +
                    "    rarity: common\n" +
                    "    effects:\n" +
                    "      attack_range: 3.0\n" +
                    "      crit_chance: 0.10\n" +
                    "      hyper: 1.0\n");
            writeFile(examples.resolve("items.yml"), "items:\n" +
                    "  healing_potion:\n" +
                    "    item: minecraft:potion\n" +
                    "    name: 治疗药水\n" +
                    "    description: 恢复生命值\n" +
                    "    item-type: potion\n" +
                    "    rarity: common\n" +
                    "    effects:\n" +
                    "      heal_amount: 10\n");
            writeFile(examples.resolve("mobs.yml"), "default-experience: 10\n" +
                    "experience:\n" +
                    "  zombie: 15\n" +
                    "  skeleton: 15\n" +
                    "modifiers: {}\n");
            writeFile(examples.resolve("content/mobs/skeleton-elite.yml"), "type: internal\n" +
                    "id: skeleton-elite\n" +
                    "template: skeleton\n" +
                    "aliases:\n" +
                    "  - skeleton_elite\n" +
                    "enabled: true\n" +
                    "spawnable: true\n" +
                    "spawn-chance: 0.12\n" +
                    "name: '&c骷髅精英'\n" +
                    "health: 30.0\n" +
                    "damage: 5.0\n" +
                    "weapon-template: rusty_iron_sword\n" +
                    "equipment:\n" +
                    "  helmet: minecraft:chainmail_helmet\n" +
                    "  chestplate: minecraft:diamond_chestplate\n" +
                    "  leggings: minecraft:chainmail_leggings\n" +
                    "  boots: minecraft:diamond_boots\n" +
                    "  main-hand: minecraft:bow\n" +
                    "  off-hand-weapon-template: rusty_iron_sword\n" +
                    "  drop-chances:\n" +
                    "    helmet: 0.01\n" +
                    "    chestplate: 0.01\n" +
                    "    leggings: 0.01\n" +
                    "    boots: 0.01\n" +
                    "    main-hand: 0.0\n" +
                    "    off-hand: 0.0\n" +
                    "drops:\n" +
                    "  held-item-chance: 0.0\n" +
                    "  items: []\n" +
                    "detect-range: 18.0\n" +
                    "skill-range: 3.2\n" +
                    "skill-cooldown-ticks: 100\n" +
                    "skill-damage: 5.0\n" +
                    "bossbar: false\n" +
                    "actions:\n" +
                    "  - when: target_close\n" +
                    "    do: melee-burst\n" +
                    "    hits: 3\n" +
                    "  - when: after melee-burst\n" +
                    "    do: retreat\n");
            writeFile(examples.resolve("content/mobs/spider-elite.yml"), "type: internal\n" +
                    "id: spider-elite\n" +
                    "template: spider\n" +
                    "aliases:\n" +
                    "  - spider_elite\n" +
                    "enabled: true\n" +
                    "spawnable: true\n" +
                    "spawn-chance: 0.12\n" +
                    "name: '&5精英蜘蛛'\n" +
                    "health: 35.0\n" +
                    "damage: 0.0\n" +
                    "speed-multiplier: 1.2\n" +
                    "equipment:\n" +
                    "  drop-chances:\n" +
                    "    helmet: 0.0\n" +
                    "    chestplate: 0.0\n" +
                    "    leggings: 0.0\n" +
                    "    boots: 0.0\n" +
                    "    main-hand: 0.0\n" +
                    "    off-hand: 0.0\n" +
                    "potion-effects:\n" +
                    "  - type: invisibility\n" +
                    "    level: 1\n" +
                    "    infinite: true\n" +
                    "    ambient: false\n" +
                    "    particles: false\n" +
                    "drops:\n" +
                    "  held-item-chance: 0.0\n" +
                    "  items: []\n" +
                    "detect-range: 16.0\n" +
                    "skill-range: 3.0\n" +
                    "skill-cooldown-ticks: 60\n" +
                    "skill-damage: 1.2\n" +
                    "bossbar: false\n" +
                    "actions:\n" +
                    "  - when: target_close\n" +
                    "    do: slow-on-hit\n" +
                    "    chance: 0.35\n" +
                    "    duration-seconds: 8.0\n" +
                    "    level: 1\n");
            writeFile(examples.resolve("content/mobs/blood-zombie.yml"), "type: internal\n" +
                    "id: blood-zombie\n" +
                    "template: zombie\n" +
                    "enabled: true\n" +
                    "spawnable: true\n" +
                    "bossbar: true\n" +
                    "name: '&4沸血僵尸'\n" +
                    "health: 180.0\n" +
                    "damage: 9.0\n" +
                    "equipment:\n" +
                    "  helmet: minecraft:netherite_helmet\n" +
                    "  chestplate: minecraft:netherite_chestplate\n" +
                    "  leggings: minecraft:diamond_leggings\n" +
                    "  boots: minecraft:netherite_boots\n" +
                    "  main-hand: minecraft:diamond_axe\n" +
                    "  drop-chances:\n" +
                    "    helmet: 0.01\n" +
                    "    chestplate: 0.01\n" +
                    "    leggings: 0.01\n" +
                    "    boots: 0.01\n" +
                    "    main-hand: 0.0\n" +
                    "    off-hand: 0.0\n" +
                    "drops:\n" +
                    "  held-item-chance: 0.0\n" +
                    "  items: []\n" +
                    "detect-range: 24.0\n" +
                    "skill-range: 5.0\n" +
                    "skill-cooldown-ticks: 120\n" +
                    "skill-damage: 8.0\n" +
                    "actions:\n" +
                    "  - when: target_far\n" +
                    "    do: leap\n" +
                    "  - when: target_close\n" +
                    "    do: shockwave\n");
            writeFile(examples.resolve("tab-scoreboard.yml"), "scoreboards:\n" +
                    "  roguelike:\n" +
                    "    title: '&6统计信息'\n" +
                    "    lines:\n" +
                    "      - '&7玩家: &e%player_name%'\n" +
                    "      - '&7经验: &e%roguelike_exp%/%roguelike_exp_next%'\n" +
                    "      - '&7击杀: &c%roguelike_kills%'\n" +
                    "      - '&7死亡: &4%roguelike_deaths%'\n");
            writeFile(examples.resolve("sidebar.yml"), "title: '&6统计信息'\n" +
                    "lines:\n" +
                    "  - '&f玩家: &e%player%'\n" +
                    "  - '&f等级: &e%level%'\n" +
                    "  - '&f经验: &a%exp%&7/&a%exp_next%'\n" +
                    "  - '&f击杀: &c%kills%'\n" +
                    "  - '&f死亡: &4%deaths%'\n" +
                    "  - '%ability_cooldowns%'\n");
            writeFile(examples.resolve("mythicmobs.yml"), "RoguelikeSkeletonKnight:\n" +
                    "  Type: SKELETON\n" +
                    "  Display: '&6Skeletal Knight'\n" +
                    "  Health: 40\n" +
                    "  Damage: 6\n" +
                    "  Options:\n" +
                    "    PreventOtherDrops: false\n");
        } catch (IOException e) {
            Message.send(sender, "&c导出示例失败: " + e.getMessage());
            return true;
        }

        Message.send(sender, "&a已导出 YAML 配置到 plugins/Roguelike，并导出示例到 plugins/Roguelike/examples。");
        Message.send(sender, "&7修改 weapons.yml、items.yml、mobs.yml、forge-recipes.yml、sidebar.yml 或 content/ 后执行 /rw reload 生效。");
        return true;
    }

    private void writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }
}
