package net.cobblers.vft;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionIntersection;
import net.minecraft.server.network.ServerPlayerEntity;

import static com.sk89q.worldedit.fabric.FabricAdapter.adapt;

public final class VFTUtils {
    public static int[] rotateVector(int[] p, String initial, String target) {
        int[][] rotationMap = {{p[0], p[1], p[2]}, {-p[2], p[1], p[0]}, {-p[0], p[1], -p[2]}, {p[2], p[1], -p[0]}};
        String[] directionMap = {"EAST", "SOUTH", "WEST", "NORTH"};

        int initIndex = 0;
        int targetIndex = 0;
        for (int i = 0; i < directionMap.length; i++) {
            if (initial.equals(directionMap[i])) initIndex = i;
            if (target.equals(directionMap[i])) targetIndex = i;
        }

        return rotationMap[(4 + (targetIndex - initIndex)) % 4];
    }

    public static class harborProximity {
        protected double distance;
        protected Harbor harbor;

        public harborProximity(double distance, Harbor harbor) {
            this.distance = distance;
            this.harbor = harbor;
        }
    }

    public static BlockVector3 rotateVector(BlockVector3 p, String initial, String target) {
        int[] output = rotateVector(new int[]{p.getBlockX(), p.getBlockY(), p.getBlockZ()}, initial, target);
        return BlockVector3.at(output[0], output[1], output[2]);
    }

    public static String rotateCardinalDirection(String relativeDirection, String initial, String target) {
        String[] directionMap = {"EAST", "SOUTH", "WEST", "NORTH"};

        int initIndex = 0;
        int targetIndex = 0;
        int relativeIndex = 0;
        for (int i = 0; i < directionMap.length; i++) {
            if (initial.equals(directionMap[i])) initIndex = i;
            if (target.equals(directionMap[i])) targetIndex = i;
            if (relativeDirection.equals(directionMap[i])) relativeIndex = i;
        }

        return directionMap[(4 + (relativeIndex + (targetIndex - initIndex))) % 4];
    }

    public static harborProximity getClosestHarbor(Vessel vessel, ServerPlayerEntity player) {
        Harbor nearestHarbor = null;
        double nearestHarborDistance = Double.MAX_VALUE;

        BlockVector3 playerLoc = BlockVector3.at(player.getPos().getX(), player.getPos().getY(), player.getPos().getZ());

        for(Harbor harbor : vessel.harbors.values()) {
            double harborDist = playerLoc.distance(harbor.helmLocation);
            if (harborDist < nearestHarborDistance) {
                nearestHarborDistance = harborDist;
                nearestHarbor = harbor;
            }
        }

        return new harborProximity(nearestHarborDistance, nearestHarbor);
    }

    public static Vessel getBoardedVessel(VFTConfig config, ServerPlayerEntity player) {
        for(Vessel vessel : config.vessels.values()) {
            Region currentVesselArea = new RegionIntersection(vessel.getPiecesList(adapt(player.world), vessel.currentHarbor));
            if(currentVesselArea.contains(BlockVector3.at(player.getPos().getX(), player.getPos().getY(), player.getPos().getZ()))) {
                return vessel;
            }
        }
        return null;
    }

    public static double getRotationAmount(String start, String end) {
        String[] directionMap = {"EAST", "SOUTH", "WEST", "NORTH"};
        int s = 0;
        int e = 0;

        for(int i = 0; i < directionMap.length; i++) {
            if (directionMap[i].equals(start)) s = i;
            if (directionMap[i].equals(end)) e = i;
        }

        return (s-e) * 90;
    }
}
