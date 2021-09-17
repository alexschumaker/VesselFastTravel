package net.cobblers.vft;

import com.sk89q.worldedit.math.BlockVector3;

public class Anchor {
    protected String name;
    protected String normalizedDirection;
    protected BlockVector3 relativeLocation;

    public Anchor(BlockVector3 relativeLocation, String normalizedDirection, String name) {
        this.relativeLocation = relativeLocation;
        this.name = name;
        this.normalizedDirection = normalizedDirection;
    }
}
