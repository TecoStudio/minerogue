package com.roguelike.debug;

import com.roguelike.RoguelikePlugin;
import org.bukkit.block.Block;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Records safe, scalar summaries of plugin-relevant Bukkit business events. */
public final class DebugEventListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerResourcePackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event == null ? null : event.getPlayer();
        record("player_resource_pack_status", "resource pack status changed", () ->
                resourcePackFields(player, event == null ? null : event.getStatus()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event == null ? null : event.getPlayer();
        record("player_bucket_empty", "player emptied bucket", () ->
                bucketFields(player, event == null ? null : event.getBucket(),
                        event == null || event.getBlock() == null
                                ? null : event.getBlock().getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        Player player = event == null ? null : event.getPlayer();
        record("player_bucket_fill", "player filled bucket", () ->
                bucketFields(player, event == null ? null : event.getBucket(),
                        event == null || event.getBlock() == null
                                ? null : event.getBlock().getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEntityExplode(EntityExplodeEvent event) {
        record("entity_explode", "entity explosion", () ->
                explosionFields(event == null ? null : event.getEntity(),
                        event == null || event.blockList() == null
                                ? 0 : event.blockList().size(),
                        event != null && event.isCancelled()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockExplode(BlockExplodeEvent event) {
        record("block_explode", "block explosion", () ->
                blockExplosionFields(event == null ? null : event.getBlock(),
                        event == null || event.blockList() == null
                                ? 0 : event.blockList().size(),
                        event != null && event.isCancelled()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event == null ? null : event.getPlayer();
        record("player_join", "player joined", () -> playerFields(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event == null ? null : event.getPlayer();
        record("player_quit", "player quit", () -> playerFields(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event == null ? null : event.getPlayer();
        record("player_item_held", "player changed held slot", () -> {
            Map<String, Object> fields = playerFields(player);
            if (event != null) {
                fields.put("previous_slot", event.getPreviousSlot());
                fields.put("new_slot", event.getNewSlot());
            }
            return fields;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event == null ? null : event.getPlayer();
        record("player_interact", "player interacted", () -> {
            Map<String, Object> fields = playerFields(player);
            if (event != null) {
                putEnum(fields, "action", event.getAction());
                putEnum(fields, "hand", event.getHand());
                Block block = event.getClickedBlock();
                if (block != null) {
                    putEnum(fields, "block", block.getType());
                    putLocation(fields, block.getLocation());
                }
                putItemType(fields, "item", event.getItem());
            }
            return fields;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event == null ? null : event.getPlayer();
        record("player_toggle_sneak", "player toggled sneak", () -> {
            Map<String, Object> fields = playerFields(player);
            if (event != null) fields.put("sneaking", event.isSneaking());
            return fields;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event == null ? null : event.getPlayer();
        record("player_move", "player moved", () -> {
            Map<String, Object> fields = playerFields(player);
            if (event != null) {
                putLocation(fields, "from", event.getFrom());
                putLocation(fields, "to", event.getTo());
            }
            return fields;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        record("entity_damage_by_entity", "entity damaged by entity", () -> {
            Map<String, Object> fields = damageFields(event);
            if (event != null) {
                putEntityType(fields, "damager", event.getDamager());
            }
            return fields;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) return;
        record("entity_damage", "entity damaged", () -> damageFields(event));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEntityDeath(EntityDeathEvent event) {
        record("entity_death", "entity died", () -> {
            Map<String, Object> fields = entityFields(event == null ? null : event.getEntity());
            if (event != null) {
                LivingEntity entity = event.getEntity();
                Player killer = entity == null ? null : entity.getKiller();
                putPlayerName(fields, "killer", killer);
                fields.put("dropped_exp", event.getDroppedExp());
                fields.put("drop_count", event.getDrops() == null ? 0 : event.getDrops().size());
            }
            return fields;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // PlayerDeathEvent is an EntityDeathEvent; the parent handler records it once.
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEntityShootBow(EntityShootBowEvent event) {
        record("entity_shoot_bow", "entity shot bow", () -> {
            Map<String, Object> fields = entityFields(event == null ? null : event.getEntity());
            if (event != null) {
                putItemType(fields, "bow", event.getBow());
                fields.put("force", event.getForce());
                putEntityType(fields, "projectile", event.getProjectile());
            }
            return fields;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        Player player = event == null ? null : event.getPlayer();
        record("player_item_damage", "item damaged", () -> {
            Map<String, Object> fields = playerFields(player);
            if (event != null) {
                putItemType(fields, "item", event.getItem());
                fields.put("damage", event.getDamage());
            }
            return fields;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event == null ? null : event.getPlayer();
        record("player_drop_item", "player dropped item", () -> {
            Map<String, Object> fields = playerFields(player);
            if (event != null && event.getItemDrop() != null) {
                putItemType(fields, "item", event.getItemDrop().getItemStack());
            }
            return fields;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        record("creature_spawn", "creature spawned", () -> {
            Map<String, Object> fields = entityFields(event == null ? null : event.getEntity());
            if (event != null) putEnum(fields, "reason", event.getSpawnReason());
            return fields;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event == null ? null : event.getPlayer();
        record("block_break", "block broken", () -> blockFields(event == null ? null : event.getBlock(), player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event == null ? null : event.getPlayer();
        record("block_place", "block placed", () -> blockFields(event == null ? null : event.getBlockPlaced(), player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event == null ? null : event.getPlayer();
        record("player_item_consume", "player consumed item", () -> {
            Map<String, Object> fields = playerFields(player);
            if (event != null) {
                putItemType(fields, "item", event.getItem());
                putEnum(fields, "hand", event.getHand());
            }
            return fields;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = event != null && event.getWhoClicked() instanceof Player p ? p : null;
        record("inventory_click", "inventory clicked", () -> {
            Map<String, Object> fields = playerFields(player);
            if (event != null) {
                putEnum(fields, "click", event.getClick());
                putEnum(fields, "action", event.getAction());
                fields.put("slot", event.getSlot());
                fields.put("raw_slot", event.getRawSlot());
                putEnum(fields, "inventory", event.getView().getTopInventory().getType());
            }
            return fields;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = event != null && event.getPlayer() instanceof Player p ? p : null;
        record("inventory_close", "inventory closed", () -> {
            Map<String, Object> fields = playerFields(player);
            if (event != null && event.getView() != null) {
                putEnum(fields, "inventory", event.getView().getTopInventory().getType());
            }
            return fields;
        });
    }

    private void record(String event, String message, Supplier<Map<String, ?>> fields) {
        try {
            RoguelikePlugin plugin = RoguelikePlugin.getInstance();
            DebugService service = plugin == null ? null : plugin.getDebugService();
            if (service == null) return;
            DebugEventSummary.record(service, event, message, fields == null ? null : fields.get());
        } catch (RuntimeException ignored) {
            // Event diagnostics are strictly best-effort and must not alter gameplay.
        }
    }

    static Map<String, Object> resourcePackFields(Player player,
                                                   PlayerResourcePackStatusEvent.Status status) {
        Map<String, Object> fields = playerFields(player);
        putEnum(fields, "status", status);
        return fields;
    }

    static Map<String, Object> bucketFields(Player player, Material bucket,
                                             org.bukkit.Location location) {
        Map<String, Object> fields = playerFields(player);
        putEnum(fields, "bucket", bucket);
        putLocation(fields, location);
        return fields;
    }

    static Map<String, Object> explosionFields(Entity entity, int blockCount,
                                                boolean cancelled) {
        Map<String, Object> fields = entityFields(entity);
        fields.put("block_count", Math.max(0, blockCount));
        fields.put("cancelled", cancelled);
        return fields;
    }

    static Map<String, Object> blockExplosionFields(Block block, int blockCount,
                                                     boolean cancelled) {
        Map<String, Object> fields = blockFields(block, null);
        fields.put("block_count", Math.max(0, blockCount));
        fields.put("cancelled", cancelled);
        return fields;
    }

    private static Map<String, Object> playerFields(Player player) {
        Map<String, Object> fields = entityFields(player);
        putPlayerName(fields, "player", player);
        return fields;
    }

    private static Map<String, Object> entityFields(Entity entity) {
        Map<String, Object> fields = new LinkedHashMap<>();
        if (entity == null) return fields;
        putEntityType(fields, "entity", entity);
        if (entity.getWorld() != null) fields.put("world", entity.getWorld().getName());
        putLocation(fields, entity.getLocation());
        return fields;
    }

    private static Map<String, Object> damageFields(EntityDamageEvent event) {
        Map<String, Object> fields = entityFields(event == null ? null : event.getEntity());
        if (event != null) {
            putEnum(fields, "cause", event.getCause());
            fields.put("damage", event.getDamage());
            fields.put("final_damage", event.getFinalDamage());
            fields.put("cancelled", event.isCancelled());
        }
        return fields;
    }

    private static Map<String, Object> blockFields(Block block, Player player) {
        Map<String, Object> fields = playerFields(player);
        if (block != null) {
            putEnum(fields, "block", block.getType());
            putLocation(fields, block.getLocation());
        }
        return fields;
    }

    private static void putPlayerName(Map<String, Object> fields, String key, Player player) {
        if (player == null) return;
        if (player.getName() != null) fields.put(key, player.getName());
        if (player.getUniqueId() != null) fields.put(key + "_uuid", player.getUniqueId().toString());
    }

    private static void putEntityType(Map<String, Object> fields, String key, Entity entity) {
        if (entity == null) return;
        if (entity.getType() != null) fields.put(key, entity.getType().name());
        if (entity.getUniqueId() != null) fields.put(key + "_uuid", entity.getUniqueId().toString());
    }

    private static void putItemType(Map<String, Object> fields, String key, ItemStack item) {
        if (item != null && item.getType() != null) fields.put(key, item.getType().name());
    }

    private static void putEnum(Map<String, Object> fields, String key, Enum<?> value) {
        if (value != null) fields.put(key, value.name());
    }

    private static void putLocation(Map<String, Object> fields, org.bukkit.Location location) {
        putLocation(fields, "location", location);
    }

    private static void putLocation(Map<String, Object> fields, String prefix, org.bukkit.Location location) {
        if (location == null) return;
        if (location.getWorld() != null) fields.put(prefix + "_world", location.getWorld().getName());
        fields.put(prefix + "_x", round(location.getX()));
        fields.put(prefix + "_y", round(location.getY()));
        fields.put(prefix + "_z", round(location.getZ()));
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
