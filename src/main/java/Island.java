import com.boydti.fawe.FaweAPI;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldguard.bukkit.event.block.PlaceBlockEvent;
import javafx.util.Pair;
import jdk.nashorn.internal.ir.Block;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

public class Island implements Listener, Comparable<Island>{
    @Override
    public int compareTo(Island o) {
        return o.islandPoint - this.islandPoint;
    }

    enum WarpDirection {
        FORWARD, BACKWARD, LEFT, RIGHT
    }

    private int tempPoint = 0;
    private int tempPointAddedCount = 0;

    public int islandPoint = 0;
    public UUID leaderUUID;
    private int maxMemberCount = 2;
    public HashMap<UUID, String> members = new HashMap<>();
    private ArrayList<UUID> waitList = new ArrayList<>();

    private HashMap<WarpDirection, Location> warpLocations;
    private Location islandLocation;
    public Region region;
    private BukkitTask task;

    public void initWithPlayer(Player player, Location location) {
        Bukkit.getPluginManager().registerEvents(this, IslandPlugin.islandPlugin);
        this.leaderUUID = player.getUniqueId();
        this.islandLocation = location;
        members.put(player.getUniqueId(), player.getName());

        Location minPos = new Location(Bukkit.getWorld("skyblock"), islandLocation.getX() - IslandPlugin.islandPlugin.getAlgorithm().privateOffset, 0, islandLocation.getZ() - IslandPlugin.islandPlugin.getAlgorithm().privateOffset);
        Location maxPos = new Location(Bukkit.getWorld("skyblock"), islandLocation.getX() + IslandPlugin.islandPlugin.getAlgorithm().privateOffset, 260, islandLocation.getZ() + IslandPlugin.islandPlugin.getAlgorithm().privateOffset);
        region = new Region(minPos, maxPos);

        collectChunks();
    }

    public void delete() {
        members.clear();
        waitList.clear();
        BlockBreakEvent.getHandlerList().unregister(this);
        BlockPlaceEvent.getHandlerList().unregister(this);
        PlayerInteractEvent.getHandlerList().unregister(this);
        EntityDamageByEntityEvent.getHandlerList().unregister(this);
        task.cancel();
        leaderUUID = null;
    }

    public void invite(Player player) {
            waitList.add(player.getUniqueId());
            if(Bukkit.getPlayer(leaderUUID) != null){
                Bukkit.getPlayer(leaderUUID).sendMessage(IslandPlugin.prefix + player.getName() + "님에게 섬 초대를 보냈습니다!");
            }
            player.sendMessage(IslandPlugin.prefix + members.get(leaderUUID) + "님의 섬 초대를 받았습니다!");
    }

    public boolean isFull() {
        return members.size() >= maxMemberCount;
    }


    public void join(Player player) {
        waitList.remove(player.getUniqueId());
        members.put(player.getUniqueId(), player.getName());
        player.sendMessage(IslandPlugin.prefix + members.get(leaderUUID) + "님의 섬에 등록되었습니다!");
        for(UUID uuid : members.keySet()){
            if(Bukkit.getPlayer(uuid) != null){
                Bukkit.getPlayer(uuid).sendMessage(IslandPlugin.prefix + player.getName() + "님이 섬에 등록되었습니다!");
            }
        }
    }

    public void reject(Player player) {
        waitList.remove(player.getUniqueId());
        player.sendMessage(IslandPlugin.prefix + members.get(leaderUUID) + "님의 섬 초대를 거절하였습니다!");
        if(Bukkit.getPlayer(leaderUUID) != null){
            Bukkit.getPlayer(leaderUUID).sendMessage(IslandPlugin.prefix + player.getName() + "님이 섬 초대를 거절하였습니다!");
        }
    }


    public boolean isMember(String name) {
        return members.containsValue(name);
    }

    public boolean isInvitee(UUID uuid) {
        if (waitList.contains(uuid)) {
            return true;
        } else {
            return false;
        }
    }

    public void leave(Player player) {
        members.remove(player.getUniqueId());
        player.sendMessage(IslandPlugin.prefix + members.get(leaderUUID) + "님의 섬에서 탈퇴하였습니다!");
        if(Bukkit.getPlayer(leaderUUID) != null){
            Bukkit.getPlayer(leaderUUID).sendMessage(IslandPlugin.prefix + player.getName() + "님이 섬에서 탈퇴하였습니다!");
        }
        moveToRealSpawn(player);
    }

    public void moveToRealSpawn(Player player) {
        CorePlugin.corePlugin.getCommandEx().teleportSpawn(player);
    }

    public boolean isPlayerBelonged(Player player) {
        if (members.containsKey(player.getUniqueId())) {
            return true;
        }
        return false;
    }

    public void initWithUUIDString(String key) {
        Bukkit.getPluginManager().registerEvents(this, IslandPlugin.islandPlugin);
        FileConfiguration config = IslandPlugin.islandPlugin.getConfig();
        leaderUUID = UUID.fromString(key);
        maxMemberCount = config.getInt("islands." + key + ".maxMemberCount");
        islandLocation = (Location) config.get("islands." + key + ".islandLocation");

        if (config.isConfigurationSection("islands." + key + ".members")) {
            for (String uuidInMembers : config.getConfigurationSection("islands." + key + ".members").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidInMembers);
                String name = config.getString("islands." + key + ".members." + uuidInMembers);

                members.put(uuid, name);
            }
        }

        Location minPos = new Location(Bukkit.getWorld("skyblock"), islandLocation.getX() - IslandPlugin.islandPlugin.getAlgorithm().privateOffset, 0, islandLocation.getZ() - IslandPlugin.islandPlugin.getAlgorithm().privateOffset);
        Location maxPos = new Location(Bukkit.getWorld("skyblock"), islandLocation.getX() + IslandPlugin.islandPlugin.getAlgorithm().privateOffset, 260, islandLocation.getZ() + IslandPlugin.islandPlugin.getAlgorithm().privateOffset);
        region = new Region(minPos, maxPos);

        collectChunks();
    }

    public void save() {
        FileConfiguration config = IslandPlugin.islandPlugin.getConfig();

        config.set("islands." + leaderUUID.toString() + ".maxMemberCount", maxMemberCount);
        config.set("islands." + leaderUUID.toString() + ".islandLocation", islandLocation);

        for (UUID uuid : members.keySet()) {
            config.set("islands." + leaderUUID.toString() + ".members." + uuid.toString(), members.get(uuid));
        }


        IslandPlugin.islandPlugin.saveConfig();
    }

    public void moveToSpawn(Player player) {
        player.teleport(islandLocation);
        player.sendMessage(IslandPlugin.prefix + "섬으로 이동하였습니다");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void block(BlockBreakEvent e) {
        if (region.isLocationInRegion(e.getBlock().getLocation())) {
            if (!members.containsKey(e.getPlayer().getUniqueId())) {
                e.setCancelled(true);
            } else {
                e.setCancelled(false);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void block(BlockPlaceEvent e) {
        if (region.isLocationInRegion(e.getBlock().getLocation())) {
            if (!members.containsKey(e.getPlayer().getUniqueId())) {
                e.setCancelled(true);
            } else {
                e.setCancelled(false);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void damage(EntityDamageByEntityEvent e) {
        if (region.isLocationInRegion(e.getEntity().getLocation())) {
            if (!members.containsKey(e.getDamager().getUniqueId())) {
                e.setCancelled(true);
            } else {
                e.setCancelled(false);
            }
        }
    }

    @EventHandler
    public void catchChestOpen(PlayerInteractEvent e) {
        if (e.getClickedBlock() != null) {
            if (region.isLocationInRegion(e.getClickedBlock().getLocation())) {
                if (!members.containsKey(e.getPlayer().getUniqueId())) {
                    e.setCancelled(true);
                }
            }
        }
    }

    public void collectChunks() {
        World world = Bukkit.getWorld("skyblock");

        task = Bukkit.getScheduler().runTaskTimer(IslandPlugin.islandPlugin, () -> {
            Bukkit.getLogger().info("chunk start");
            LinkedList<ChunkSnapshot> chunkSnapshots = new LinkedList<>();
            int iterateCountX = 13;
            int iterateCountZ = 13;

            for (int currentX = region.minPos.getBlockX(); iterateCountX > 0; currentX += 16) {
                iterateCountX--;

                for (int currentZ = region.minPos.getBlockZ(); iterateCountZ > 0; currentZ += 16) {
                    iterateCountZ--;

                    Location location = new Location(world, currentX, 100, currentZ);
                    Chunk chunk = location.getChunk();
                    if (!chunk.isLoaded()) {
                        chunk.load();
                        ChunkSnapshot chunkSnapshot = chunk.getChunkSnapshot();
                        chunkSnapshots.add(chunkSnapshot);
                        chunk.unload();
                    } else {

                        ChunkSnapshot chunkSnapshot = chunk.getChunkSnapshot();
                        chunkSnapshots.add(chunkSnapshot);
                    }
                }
                iterateCountZ = 13;
            }

            tempPointAddedCount = 0;
            tempPoint = 0;
            calcChunkAsync(world, chunkSnapshots);
        }, 0L, 72000L);
    }

    public void addTempPoint(int toAdd) {
        tempPoint += toAdd;
        tempPointAddedCount++;

        if (tempPointAddedCount >= 169) {
            Bukkit.getLogger().info("added " + tempPointAddedCount);
            islandPoint = tempPoint;
            Bukkit.getLogger().info(members.get(leaderUUID) + " new point: " + islandPoint);
        }
    }

    public void calcChunkAsync(World world, LinkedList<ChunkSnapshot> chunkSnapshots) {
        Bukkit.getScheduler().runTaskAsynchronously(IslandPlugin.islandPlugin, () -> {
            for (ChunkSnapshot chunk : chunkSnapshots) {
                scanChunk(world, chunk);
            }
        });
    }

    public void scanChunk(World world, ChunkSnapshot chunkSnapshot) {
        int tempScanChunk = 0;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 255; y++) {

                    BlockData data = chunkSnapshot.getBlockData(x, y, z);


                    Material blockType = data.getMaterial();
                    if (blockType != Material.AIR) {
                        if(blockType == Material.DIAMOND_BLOCK){
                            tempScanChunk++;
                        }
                    }
                }
            }
        }

        addTempPoint(tempScanChunk);

    }


}
