package kingtayy;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BetterShulker extends JavaPlugin implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, ItemStack> openShulkerItems = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("BetterShulker enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("BetterShulker disabled.");
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Action action = event.getAction();

        // Only proceed if holding a shulker box in MAIN hand
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.getType().name().endsWith("SHULKER_BOX")) return;

        // Prevent placing or interacting with block form
        event.setCancelled(true);
        if (action == Action.RIGHT_CLICK_BLOCK) return;

        if (action != Action.RIGHT_CLICK_AIR) return;

        // Cooldown check
        if (cooldowns.containsKey(uuid) && System.currentTimeMillis() < cooldowns.get(uuid)) {
            long secondsLeft = (long) Math.ceil((cooldowns.get(uuid) - System.currentTimeMillis()) / 1000.0);
            if (secondsLeft <= 0) return;
            player.sendMessage("§cPlease wait " + secondsLeft + "s before opening another shulker box.");
            return;
        }

        cooldowns.put(uuid, System.currentTimeMillis() + 3000);

        if (item.getItemMeta() instanceof BlockStateMeta meta &&
                meta.getBlockState() instanceof ShulkerBox shulkerBox) {
            Inventory shulkerInventory = shulkerBox.getInventory();
            openShulkerItems.put(uuid, item);
            player.openInventory(shulkerInventory);
            player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, 1.0f, 1.0f);
        }
    }
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!openShulkerItems.containsKey(uuid)) return;

        ItemStack drop = event.getItemDrop().getItemStack();
        if (drop.equals(openShulkerItems.get(uuid))) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot drop the shulker box while it's open.");
        }
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();

        if (!openShulkerItems.containsKey(uuid)) return;

        ItemStack heldShulker = openShulkerItems.get(uuid);
        int mainSlot = player.getInventory().getHeldItemSlot();

        // Prevent moving shulker from main hand
        if (event.getSlot() == mainSlot || event.getHotbarButton() == mainSlot) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.equals(heldShulker)) {
                event.setCancelled(true);
                player.sendMessage("§cYou cannot move the shulker box while it's open.");
            }
        }


        ItemStack current = event.getCurrentItem();
        if (current != null && current.getType().name().endsWith("SHULKER_BOX")) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot use other shulker boxes while one is open.");
        }
    }




    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!openShulkerItems.containsKey(uuid)) return;

        ItemStack item = openShulkerItems.remove(uuid);
        if (item.getItemMeta() instanceof BlockStateMeta meta && meta.getBlockState() instanceof ShulkerBox shulkerBox) {
            Inventory inv = event.getInventory();
            shulkerBox.getInventory().setContents(inv.getContents());
            shulkerBox.update();
            meta.setBlockState(shulkerBox);
            item.setItemMeta(meta);
        }

        player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_CLOSE, 1.0f, 1.0f);
    }
}
