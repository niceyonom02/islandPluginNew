import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;

public class IslandAlgorithm {
    enum Phase{
        UP, DOWN, LEFT, RIGHT, CENTER
    };

    private Phase currentPhase = Phase.CENTER;
    private int currentShellNumber = 0;
    private int nextIslandNumber = 1;
    private int countToGoInDirection = 0;
    private Location previousIslandLocation;
    public int privateOffset = 100;
    public int margin = 25;

    public int offset = 2 * (privateOffset + margin);

    public void loadResources(){
        FileConfiguration config = IslandPlugin.islandPlugin.getConfig();

        if(config.isConfigurationSection("resources")){
            currentPhase = Phase.valueOf(config.getString("resources.currentPhase"));
            currentShellNumber = config.getInt("resources.currentShellNumber");
            nextIslandNumber = config.getInt("resources.nextIslandNumber");
            countToGoInDirection = config.getInt("resources.countToGoInDirection");
            previousIslandLocation = (Location) config.get("resources.previousIslandLocation");
            privateOffset = config.getInt("resources.privateOffset");
            margin = config.getInt("resources.margin");

            offset = 2 * (margin + privateOffset);
        }
    }

    public void saveResources(){
        FileConfiguration config = IslandPlugin.islandPlugin.getConfig();
        if(config.isConfigurationSection("resources")){
            config.set("resources", null);
        }

        config.set("resources.currentPhase", currentPhase.toString());
        config.set("resources.currentShellNumber", currentShellNumber);
        config.set("resources.nextIslandNumber", nextIslandNumber);
        config.set("resources.countToGoInDirection", countToGoInDirection);
        config.set("resources.previousIslandLocation", previousIslandLocation);
        config.set("resources.privateOffset", privateOffset);
        config.set("resources.margin", margin);

        IslandPlugin.islandPlugin.saveConfig();
    }

    public Location getNewIsland(){
        if(currentPhase == Phase.RIGHT && countToGoInDirection == 0){
            currentShellNumber++;
            currentPhase = Phase.UP;
            countToGoInDirection = 2 * currentShellNumber - 1;

            Location loc = setLocation(Phase.RIGHT);
            nextIslandNumber++;

            return loc;
        }

        if(nextIslandNumber == 1){
            currentPhase = Phase.RIGHT;
            countToGoInDirection = 0;
            Location loc = setLocation(Phase.CENTER);

            nextIslandNumber++;


            return loc;
        }

        if(countToGoInDirection > 0){
            Location loc = setLocation(currentPhase);
            nextIslandNumber++;
            countToGoInDirection--;

            return loc;
        }

        if(countToGoInDirection == 0){
            countToGoInDirection = 2 * currentShellNumber;
            switch (currentPhase){
                case UP:
                    currentPhase = Phase.LEFT;
                    break;
                case LEFT:
                    currentPhase = Phase.DOWN;
                    break;
                case DOWN:
                    currentPhase = Phase.RIGHT;
            }

            Location loc = setLocation(currentPhase);
            nextIslandNumber++;
            countToGoInDirection--;

            return loc;
        }

        return null;
    }

    //RIGHT -> x Loc ++
    //LEFT -> x Loc --
    //UP -> z Loc --
    //DOWN -> z Loc ++

    public Location setLocation(Phase direction){
        Location newIslandLocation = new Location(Bukkit.getWorld("skyblock"), 0, 100, 0);
        switch (direction){
            case UP:
                newIslandLocation = previousIslandLocation.add(0, 0, -offset);
                break;
            case DOWN:
                newIslandLocation = previousIslandLocation.add(0, 0, offset);
                break;
            case LEFT:
                newIslandLocation = previousIslandLocation.add(-offset, 0, 0);
                break;
            case RIGHT:
                newIslandLocation = previousIslandLocation.add(offset, 0, 0);
                break;
            case CENTER:
                break;
        }

        previousIslandLocation = newIslandLocation.clone();
        return newIslandLocation.clone();


    }
}
