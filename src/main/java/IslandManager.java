import com.boydti.fawe.FaweAPI;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;

public class IslandManager implements Listener, CommandExecutor {
    private HashMap<UUID, Island> islands = new HashMap<UUID, Island>();
    private HashMap<UUID, Integer> restartWaitTime = new HashMap<>();
    private ArrayList<Material> crops = new ArrayList<Material>();

    public IslandManager() {
        getRestartWaitTime();
        getStoredIslands();
        waitScheduler();
        getCrops();
        //rankScheduler();

    }


    public void waitScheduler(){
        Bukkit.getScheduler().scheduleSyncRepeatingTask(IslandPlugin.islandPlugin, new Runnable() {
            @Override
            public void run() {
                ArrayList<UUID> toRemoveList = new ArrayList<>();

                for(UUID uuid : restartWaitTime.keySet()){
                    if(restartWaitTime.get(uuid) <= 1){
                        toRemoveList.add(uuid);
                    }
                    restartWaitTime.put(uuid, restartWaitTime.get(uuid) - 1);
                }

                for(UUID uuid : toRemoveList){
                    restartWaitTime.remove(uuid);
                }
            }
        }, 0L, 20L);
    }

    public void getRestartWaitTime(){
        FileConfiguration config = IslandPlugin.islandPlugin.getConfig();
        if(config.isConfigurationSection("wait")){
            for(String uuidString : config.getConfigurationSection("wait").getKeys(false)){
                UUID uuid = UUID.fromString(uuidString);
                int waitTime = config.getInt("wait." + uuidString);
                restartWaitTime.put(uuid, waitTime);
            }
        }
    }

    public void getCrops(){
        FileConfiguration config = IslandPlugin.islandPlugin.getConfig();
        if(config.isConfigurationSection("crop")){
            ArrayList<String> materials = (ArrayList<String>)config.getList("crop");
            for(String key : materials){
                Material crop = Material.getMaterial(key);
                crops.add(crop);
            }
        }
    }

    public void saveIslands(){
        FileConfiguration config = IslandPlugin.islandPlugin.getConfig();
        if(config.isConfigurationSection("islands")){
            config.set("islands", "");
        }

        for(UUID uuid : islands.keySet()){
            Island island = islands.get(uuid);
            island.save();
        }
        IslandPlugin.islandPlugin.saveConfig();
    }

    public void saveWait(){
        FileConfiguration config = IslandPlugin.islandPlugin.getConfig();

        if(config.isConfigurationSection("wait")){
            config.set("wait", "");
        }

        for(UUID uuid : restartWaitTime.keySet()){
            config.set("wait." + uuid, restartWaitTime.get(uuid));
        }

        IslandPlugin.islandPlugin.saveConfig();
    }

    public void getStoredIslands(){
        FileConfiguration config = IslandPlugin.islandPlugin.getConfig();

        if(config.isConfigurationSection("islands")){
            for(String key : config.getConfigurationSection("islands").getKeys(false)){
                UUID uuid = UUID.fromString(key);
                Island island = new Island();
                island.initWithUUIDString(key);

                islands.put(uuid, island);
            }
        }
    }

    public void movePlayerIsland(Player player){
        if(getBelongedIsland(player) != null){
            getBelongedIsland(player).moveToSpawn(player);
            return;
        }

        if(islands.containsKey(player.getUniqueId())){
            islands.get(player.getUniqueId()).moveToSpawn(player);
        } else{
            if(restartWaitTime.containsKey(player.getUniqueId())){
                player.sendMessage(IslandPlugin.prefix + "섬을 생성하려면 " + restartWaitTime.get(player.getUniqueId()) + "초를 기다려야 합니다");
                return;
            }

            Island island = new IslandCreator().create(player);
            islands.put(player.getUniqueId(), island);
            movePlayerIsland(player);
            if(!crops.isEmpty()){
                giveRandomCrops(player);
                player.sendMessage(IslandPlugin.prefix + "특산물이 지급되었습니다");
            }
            player.sendMessage(IslandPlugin.prefix+ "섬을 생성하였습니다");
        }
    }

    public void giveRandomCrops(Player player){
        Random random = new Random();
        int rand = random.nextInt(crops.size());
        ItemStack itemStack = new ItemStack(crops.get(rand), 10);
        player.getInventory().addItem(itemStack);
    }

    public Island getBelongedIsland(Player player){
        for(Island island: islands.values()){
            if(island.isMember(player.getName())){
                return island;
            }
        }

        return null;
    }

    public Island getBelongedIsland(String name){
        for(Island island: islands.values()){
            if(island.isMember(name)){
                return island;
            }
        }

        return null;
    }


    public void save(){
        saveWait();
        saveIslands();
    }

    private void showIslandsRanking(Player player){
        ArrayList<Island> islandList = new ArrayList<>(islands.values());
        Collections.sort(islandList);
        player.sendMessage(IslandPlugin.prefix + "섬 랭킹");
        if(islandList.size() >= 10){
            for(int i = 0; i < 10; i++){
                Island island = islandList.get(i);
                player.sendMessage(IslandPlugin.prefix + ChatColor.YELLOW + (i + 1) + ". " + island.members.get((island.leaderUUID)) + ": " + ChatColor.GRAY + island.islandPoint);
            }
        } else{
            for(int i = 0; i < islandList.size(); i++){
                Island island = islandList.get(i);
                player.sendMessage(IslandPlugin.prefix + ChatColor.YELLOW + (i + 1) + ". " + island.members.get((island.leaderUUID)) + ": " + ChatColor.GRAY + island.islandPoint);
            }
        }
    }

    public void helpMessage(Player player){
        player.sendMessage(IslandPlugin.prefix + ChatColor.YELLOW + "섬 플러그인 명령어");
        player.sendMessage(IslandPlugin.prefix+ "/is");
        player.sendMessage(IslandPlugin.prefix+"/is menu");
        player.sendMessage(IslandPlugin.prefix+"/is help");
        player.sendMessage(IslandPlugin.prefix+"/is accept [초대한 사람 닉네임]");
        player.sendMessage(IslandPlugin.prefix+"/is reject [초대한 사람 닉네임]");
        player.sendMessage(IslandPlugin.prefix+"/is invite [초대할 사람 닉네임]");
        player.sendMessage(IslandPlugin.prefix+"/is leave");
        player.sendMessage(IslandPlugin.prefix+"/is member");
        player.sendMessage(IslandPlugin.prefix+"/is level");
        player.sendMessage(IslandPlugin.prefix+"/is rank");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)) return false;

        Player player = (Player) sender;

        if (label.equalsIgnoreCase("is")) {
            if (args.length == 0) {
                movePlayerIsland(player);
                return false;
            }

            if (args.length == 1) {
                switch (args[0]){
                    case "member":
                        if(getBelongedIsland(player.getName()) != null){
                            player.sendMessage(IslandPlugin.prefix + "섬 인원");
                            for(UUID uuid : getBelongedIsland(player.getName()).members.keySet()){
                                player.sendMessage(IslandPlugin.prefix + getBelongedIsland(player).members.get(uuid));
                            }
                        } else{
                            player.sendMessage(IslandPlugin.prefix + "속해있는 섬이 없습니다!");
                        }
                        break;
                    case "level":
                        if(getBelongedIsland(player.getName()) != null){
                            player.sendMessage(IslandPlugin.prefix + "섬 레벨: " + getBelongedIsland(player.getName()).islandPoint);
                        } else{
                            player.sendMessage(IslandPlugin.prefix + "속해있는 섬이 없습니다!");
                        }
                        break;
                    case "rank":
                        showIslandsRanking(player);
                        break;
                    case "menu":
                        break;
                    case "help":
                    case "join":
                    case "accept":
                    case "reject":
                    case "invite":
                        helpMessage(player);
                        break;
                    case "leave":
                        if(islands.containsKey(player.getUniqueId())){
                            player.sendMessage(IslandPlugin.prefix + "섬을 삭제하였습니다");
                            CorePlugin.corePlugin.getCommandEx().teleportSpawn(player);
                            islands.get(player.getUniqueId()).delete();
                            islands.remove(player.getUniqueId());
                            restartWaitTime.put(player.getUniqueId(), 7200);
                            return true;
                        }

                        if(getBelongedIsland(player) != null){
                            getBelongedIsland(player).leave(player);
                        } else{
                            player.sendMessage(IslandPlugin.prefix +"속해있는 섬이 없습니다!");
                            return false;
                        }
                        break;

                }
            }

            if (args.length == 2) {
                switch (args[0]){
                    case "invite":
                        if(Bukkit.getPlayer(args[1]) != null){
                            Bukkit.getLogger().info("is player");
                            Player invitee = Bukkit.getPlayer(args[1]);
                            if(invitee.getUniqueId().equals(player.getUniqueId())){
                                player.sendMessage(IslandPlugin.prefix + "자기 자신에게는 사용할 수 없는 명령어입니다!");
                                return false;
                            }

                            if(islands.containsKey(player.getUniqueId())){
                                if(islands.get(player.getUniqueId()).isInvitee(invitee.getUniqueId())){
                                    player.sendMessage(IslandPlugin.prefix + "이미 초대했습니다!");
                                    return false;
                                }

                                if(islands.get(player.getUniqueId()).isMember(Bukkit.getPlayer(args[1]).getName())){
                                    player.sendMessage(IslandPlugin.prefix + "이미 섬원입니다!");
                                    return false;
                                }

                                if(islands.get(player.getUniqueId()).isFull()){
                                    player.sendMessage(IslandPlugin.prefix + "섬이 꽉 찼습니다!");
                                    return false;
                                }

                                islands.get(player.getUniqueId()).invite(invitee);
                                return true;
                            } else{
                                player.sendMessage(IslandPlugin.prefix +"보유중인 섬이 없거나 권한이 부족합니다!");
                                return false;
                            }
                        } else{
                            player.sendMessage(IslandPlugin.prefix +"해당 플레이어는 접속중이지 않습니다!");
                            return false;
                        }
                    case "accept":

                        if(args[1].equalsIgnoreCase(player.getName())){
                            player.sendMessage(IslandPlugin.prefix +"자기 자신에게는 사용할 수 없는 명령어입니다!");
                            return false;
                        }

                        Island island = getBelongedIsland(args[1]);
                        if(island != null){
                            if(island.isInvitee(player.getUniqueId())){
                                if(island.isFull()){
                                    player.sendMessage(IslandPlugin.prefix +"섬이 꽉 찼습니다!");
                                    return false;
                                }

                                if(getBelongedIsland(player) != null){
                                    player.sendMessage(IslandPlugin.prefix + ChatColor.YELLOW + "/is leave" + ChatColor.GRAY + "를 통해 섬을 먼저 탈퇴해주세요");
                                    return false;
                                }
                                island.join(player);
                                return true;
                            } else{
                                player.sendMessage(IslandPlugin.prefix + "초대 목록에 없습니다!");
                                return false;
                            }
                        } else{
                            player.sendMessage(IslandPlugin.prefix + "해당 플레이어가 소유중인 섬이 없습니다!");
                            return false;
                        }
                    case "reject":
                        if(args[1].equalsIgnoreCase(player.getName())){
                            player.sendMessage(IslandPlugin.prefix + "자기 자신에게는 사용할 수 없는 명령어입니다!");
                            return false;
                        }

                        Island islandReject = getBelongedIsland(args[1]);
                        if(islandReject != null){
                            if(islandReject.isInvitee(player.getUniqueId())){
                                islandReject.reject(player);
                                return true;
                            } else{
                                player.sendMessage(IslandPlugin.prefix + "초대 목록에 없습니다!");
                                return false;
                            }
                        } else{
                            player.sendMessage(IslandPlugin.prefix + "해당 플레이어가 소유중인 섬이 없습니다!");
                            return false;
                        }
                }
            }

            if(args.length > 2){
                helpMessage(player);
            }
        }
        return false;
    }
}
