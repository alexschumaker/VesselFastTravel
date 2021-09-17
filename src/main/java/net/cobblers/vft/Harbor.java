package net.cobblers.vft;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;

public class Harbor {
    protected String name;
    protected BlockVector3 helmLocation;
    protected String direction;
    protected World world;

    public Harbor(String name, String direction, BlockVector3 location) {
        this.name = name;
        this.direction = direction;
        this.helmLocation = location;
    }
}
