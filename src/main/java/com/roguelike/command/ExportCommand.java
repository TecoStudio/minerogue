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
            writeFile(examples.resolve("mobs.yml"), "internal:\n" +
                    "  enabled: true\n" +
                    "  skeleton-elite:\n" +
                    "    enabled: true\n" +
                    "    spawn-chance: 0.12\n" +
                    "    name: '&c骷髅精英'\n" +
                    "  zombie-elite:\n" +
                    "    enabled: true\n" +
                    "    spawn-chance: 0.12\n" +
                    "    name: '&2僵尸精英'\n" +
                    "    weapon-template: excited_stone_sword\n" +
                    "  # 精英蜘蛛：隐身，35血，1.2倍移速，攻击概率附加缓慢。\n" +
                    "  spider-elite:\n" +
                    "    enabled: true\n" +
                    "    # 自然蜘蛛转化概率，0.12 = 12%。\n" +
                    "    spawn-chance: 0.12\n" +
                    "    name: '&5精英蜘蛛'\n" +
                    "    health: 35.0\n" +
                    "    # 1.2 = 原版移速的 1.2 倍。\n" +
                    "    speed-multiplier: 1.2\n" +
                    "    # 0.35 = 35% 概率。\n" +
                    "    slow-chance: 0.35\n" +
                    "    slow-duration-seconds: 8.0\n" +
                    "    # 药水等级按游戏内显示填写：1 = 缓慢 I，2 = 缓慢 II。\n" +
                    "    slow-level: 1\n" +
                    "default-experience: 10\n" +
                    "experience:\n" +
                    "  zombie: 15\n" +
                    "  skeleton: 15\n" +
                    "modifiers: {}\n");
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
        Message.send(sender, "&7修改 weapons.yml、items.yml、mobs.yml、forge-recipes.yml、sidebar.yml 后执行 /rw reload 生效。");
        return true;
    }

    private void writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }
}
