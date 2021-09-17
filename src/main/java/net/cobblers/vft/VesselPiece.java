package net.cobblers.vft;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;

public class VesselPiece {
    protected String name;
    protected BlockVector3 pos1 = BlockVector3.ZERO;
    protected BlockVector3 pos2 = BlockVector3.ZERO;

    public VesselPiece() {
    }

    public VesselPiece(String name) {
        this.name = name;
    }

    public CuboidRegion getRegion() {
        return new CuboidRegion(pos1, pos2);
    }

    public CuboidRegion getRegion(World world) {
        return new CuboidRegion(world, pos1, pos2);
    }

    public void setPos(BlockVector3 pos1, BlockVector3 pos2) {
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public void setPos1(BlockVector3 pos1) {
        this.pos1 = pos1;
    }

    public void setPos2(BlockVector3 pos2) {
        this.pos2 = pos2;
    }
}
