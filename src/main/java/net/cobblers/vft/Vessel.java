package net.cobblers.vft;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Vessel {
    protected String name;
    protected Harbor currentHarbor;
    protected VesselType type = null;
    protected Map<String, Harbor> harbors = new HashMap<>();
    protected Map<String, VesselPiece> pieces = new HashMap<>();
    protected Map<String, Anchor> anchors = new HashMap<>();

    public Vessel(String name) {
        this.name = name;
    }

    public void setCurrentHarbor(String harborName){
        this.currentHarbor = harbors.get(harborName);
    }

    // gets the list of pieces about the origin (0,0,0)
    // no known use right now, but...
    public List<Region> getPiecesList(World world) {
        List<Region> output = new ArrayList<>();
        for (VesselPiece piece : this.pieces.values()) {
            output.add(piece.getRegion(world));
        }

        return output;
    }

    public void addPiece(String name) {
        VesselPiece newPiece = new VesselPiece(name);
        this.pieces.put(name, newPiece);
    }

    public void addHarbor(String name, String direction, BlockVector3 location) {
        Harbor newHarbor = new Harbor(name, direction, location);
        this.harbors.put(name, newHarbor);
    }

    public void addHarbor(String name, String direction, BlockVector3 location, String anchorName) {
        Anchor anchor = anchors.get(anchorName);
        String relativeDirection = VFTUtils.rotateCardinalDirection("EAST", anchor.normalizedDirection, direction);
        BlockVector3 relativeLocation = VFTUtils.rotateVector(anchor.relativeLocation, anchor.normalizedDirection, direction);
        relativeLocation = location.subtract(relativeLocation);

        Harbor newHarbor = new Harbor(name, relativeDirection, relativeLocation);
        this.harbors.put(name, newHarbor);
    }

    public List<Region> getPiecesList(World world, Harbor harbor) {
        List<Region> output = new ArrayList<>();
        for (VesselPiece piece : this.pieces.values()) {
            BlockVector3 pos1 = piece.pos1.transform2D(-VFTUtils.getRotationAmount("EAST", harbor.direction), 0,0, harbor.helmLocation.getX(), harbor.helmLocation.getZ());
            pos1 = pos1.withY(pos1.getBlockY() + harbor.helmLocation.getBlockY());
            BlockVector3 pos2 = piece.pos2.transform2D(-VFTUtils.getRotationAmount("EAST", harbor.direction), 0,0, harbor.helmLocation.getX(), harbor.helmLocation.getZ());
            pos2 = pos2.withY(pos2.getBlockY() + harbor.helmLocation.getBlockY());

            output.add(new CuboidRegion(world, pos1, pos2));
        }

        return output;
    }

    public BlockVector3 getPos1RelativeToHarbor(VesselPiece piece, Harbor harbor) {
        return piece.pos1.transform2D(VFTUtils.getRotationAmount("EAST", harbor.direction), 0,0, harbor.helmLocation.getX(), harbor.helmLocation.getZ());
    }
    public BlockVector3 getPos2RelativeToHarbor(VesselPiece piece, Harbor harbor) {
        return piece.pos2.transform2D(VFTUtils.getRotationAmount("EAST", harbor.direction), 0,0, harbor.helmLocation.getX(), harbor.helmLocation.getZ());
    }
    public BlockVector3 getPos1RelativeToHarbor(String pieceName, String harborName) {
        return getPos1RelativeToHarbor(pieces.get(pieceName), harbors.get(harborName));
    }
    public BlockVector3 getPos2RelativeToHarbor(String pieceName, String harborName) {
        return getPos2RelativeToHarbor(pieces.get(pieceName), harbors.get(harborName));
    }

    // An anchor is a point that a user can set in order to make it easier to create a harbor. It must be within the defined constraints of a vessel
    // and it's position is saved relative to the east-facing helm.
    // Input location should be a real-world location; conversion logic relative to current harbor's helm will take place here.
    public void addAnchor(BlockVector3 location, String direction, String name) {
        if (anchors == null) {
            anchors = new HashMap<>();
        }

        BlockVector3 relativeLocation = location.subtract(currentHarbor.helmLocation);
        relativeLocation = VFTUtils.rotateVector(relativeLocation, currentHarbor.direction, "EAST");

        String relativeDir = VFTUtils.rotateCardinalDirection(direction, currentHarbor.direction, "EAST");

        anchors.put(name, new Anchor(relativeLocation, relativeDir, name));
    }

    public boolean setType(String type) {
        this.type = new VesselType(type);
        return this.type.getType() != null;
    }
}