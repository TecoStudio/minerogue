package com.roguelike.ticket;

import org.bukkit.Material;

public enum TicketType {
    TICKET_A("ticket_a", "§c§l强化券", "§7提升武器的某个词条，成功率随使用次数递减", Material.PAPER),
    SUPER_TICKET_A("super_ticket_a", "§f§l超级强化券", "§7必定成功强化武器的某个词条", Material.BONE_MEAL),
    TICKET_B("ticket_b", "§a§l开发券", "§7开发普通物品为武器，或随机给装备添加词条", Material.PAPER),
    TICKET_C("ticket_c", "§9§l移除券", "§7选择移除一个词条，并提升下次强化成功率", Material.PAPER),
    WEAPON_DEVELOPMENT("weapon_development", "§d§l武器开发券", "§7将任意物品开发为特殊品质武器", Material.NETHER_STAR);

    private final String id;
    private final String displayName;
    private final String description;
    private final Material material;

    TicketType(String id, String displayName, String description, Material material) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.material = material;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public Material getMaterial() { return material; }

    public static TicketType fromId(String id) {
        for (TicketType type : values()) {
            if (type.id.equalsIgnoreCase(id)) return type;
        }
        return null;
    }
}
