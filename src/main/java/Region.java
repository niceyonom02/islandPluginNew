import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionType;
import org.bukkit.Location;

import java.awt.geom.Area;
import java.util.List;

public class Region {
    public Location minPos;
    public Location maxPos;

    public Region(Location minPos, Location maxPos){
        this.minPos = minPos;
        this.maxPos = maxPos;
    }

    public boolean isLocationInRegion(Location loc){
        if(loc.getX() >= minPos.getX() && loc.getX() <= maxPos.getX()){
            if(loc.getY() >= minPos.getY() && loc.getY() <= maxPos.getY()){
                if(loc.getZ() >= minPos.getZ() && loc.getZ() <= maxPos.getZ()){
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Region{" +
                "minPos=" + minPos +
                ", maxPos=" + maxPos +
                '}';
    }
}
