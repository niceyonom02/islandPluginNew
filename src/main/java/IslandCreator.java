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
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;

public class IslandCreator {

    public Island create(Player player){
        Location islandLocation = IslandPlugin.islandPlugin.getAlgorithm().getNewIsland();
        Island island = new Island();
        island.initWithPlayer(player, islandLocation);

        File defaultSchematic = new File(IslandPlugin.islandPlugin.getDataFolder(), "defaultIsland.schem");

        ClipboardFormat format = ClipboardFormats.findByFile(defaultSchematic);
        try (ClipboardReader reader = format.getReader(new FileInputStream(defaultSchematic))) {
            Clipboard clipboard = reader.read();

            try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(FaweAPI.getWorld("skyblock"), -1)) {
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(islandLocation.getX(), islandLocation.getY(), islandLocation.getZ()))
                        .ignoreAirBlocks(false)
                        .build();
                Operations.complete(operation);
            }
        } catch (Exception e){

        }
        return island;
    }
}
