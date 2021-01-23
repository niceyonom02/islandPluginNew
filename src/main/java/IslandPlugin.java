import org.bukkit.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class IslandPlugin extends JavaPlugin implements Listener {
    public static String prefix = ChatColor.GRAY + "[" + ChatColor.YELLOW + "N" + ChatColor.GRAY + "] ";
    public static IslandPlugin islandPlugin;
    public static IslandManager islandManager;
    private IslandAlgorithm algorithm;
    public Location spawnPoint;

    @Override
    public void onEnable(){
        Bukkit.getPluginManager().registerEvents(this, this);
        if(!getDataFolder().exists()){
            getDataFolder().mkdir();
        }

        if(getServer().getWorld("skyblock") == null){
            WorldCreator wc = new WorldCreator("skyblock");
            wc.generator(new EmptyChunkGenerator()); //The chunk generator from step 1
            wc.createWorld();
        }

        File file = new File(getDataFolder(), "config.yml");
        if(!file.exists()){
            saveResource("config.yml", false);
        }


        islandPlugin = this;
        algorithm = new IslandAlgorithm();
        algorithm.loadResources();

        islandManager = new IslandManager();
        getCommand("is").setExecutor(islandManager);

        spawnPoint = Bukkit.getWorld("world").getSpawnLocation();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void block(BlockBreakEvent e){
        if(!e.getPlayer().isOp()){
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void block(BlockPlaceEvent e){
        if(!e.getPlayer().isOp()){
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void damage(EntityDamageByEntityEvent e){
        if(!e.getDamager().isOp()){
            e.setCancelled(true);
        }
    }

    public IslandAlgorithm getAlgorithm(){
        return algorithm;
    }

    @Override
    public void onDisable(){
        algorithm.saveResources();
        islandManager.save();
    }

}
