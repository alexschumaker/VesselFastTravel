package net.cobblers.vft;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.util.EntityRemover;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.EntityVisitor;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionIntersection;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sk89q.worldedit.fabric.FabricAdapter.adaptPlayer;
import static com.sk89q.worldedit.fabric.FabricAdapter.adapt;


public class VesselTransport {
    String status = "START";

    private Vessel vessel;
    protected String activeVesselName;
    private String startingHarborName;
    private String destinationHarborName;

    private ServerPlayerEntity initiatingPlayer;

    private final SchematicHandler sh = new SchematicHandler();

    private boolean isSummon = false;

    private final Logger LOGGER = LogManager.getLogger();

    public VesselTransport(Vessel vessel, String destination, ServerPlayerEntity initPlayer, boolean isSummon) {
        if (vessel == null) {
            LOGGER.error("Vessel entered is null.");
            return;
        }
        this.vessel = vessel;
        this.activeVesselName = vessel.name;
        this.startingHarborName = vessel.currentHarbor.name;

        this.destinationHarborName = destination.toLowerCase();

        if (this.startingHarborName.equals(this.destinationHarborName)) {
            LOGGER.error("Already at this harbor!");
            this.status = "ABORT";
            return;
        }

        this.isSummon = isSummon;

        status = isSummon ? "SUMMONING" : "START";

        this.initiatingPlayer = initPlayer;
    }

    public void sail() {
        World world = adapt(initiatingPlayer.world);

        BlockVector3 startingHarborLoc = this.vessel.harbors.get(startingHarborName).helmLocation;
        String startingHarborDirection = this.vessel.harbors.get(startingHarborName).direction;

        BlockVector3 destinationHarborLoc = this.vessel.harbors.get(destinationHarborName).helmLocation;
        String destinationHarborDirection = this.vessel.harbors.get(destinationHarborName).direction;

        // This is the RegionInstersection of all the pieces (which is all of them put together despite the name)
        RegionIntersection region = new RegionIntersection(world, this.vessel.getPiecesList(world, this.vessel.harbors.get(startingHarborName)));
        if (isSummon) {
            for(BlockVector2 chunk : region.getChunks()) {
                initiatingPlayer.getServerWorld().getChunkManager().setChunkForced(new ChunkPos(chunk.getX(), chunk.getZ()), true);
            }
        }

        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(startingHarborLoc);
        try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, -1)) {
            ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                    editSession, region, clipboard, region.getMinimumPoint()
            );
            // configure here
            forwardExtentCopy.setCopyingEntities(true);
            forwardExtentCopy.setSourceMask(vessel.type.getMask(editSession));

            Operations.complete(forwardExtentCopy);
        } catch (WorldEditException e) {
            e.printStackTrace();
        }

        try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, -1)) {
            ClipboardHolder holder = new ClipboardHolder(clipboard);
            holder.setTransform(new AffineTransform().rotateY(VFTUtils.getRotationAmount(startingHarborDirection, destinationHarborDirection)));

            Operation operation = holder
                    .createPaste(editSession)
                    .to(destinationHarborLoc) // might need a mask on top of this. Should look into source code to see what is actually being done here.
                    .copyEntities(true)
                    // configure here
                    .build();
            Operations.complete(operation);
        } catch (WorldEditException e) {
            LOGGER.error(e);
            e.printStackTrace();
        }

        if (isSummon) {
            for(BlockVector2 chunk : region.getChunks()) {
                initiatingPlayer.getServerWorld().getChunkManager().setChunkForced(new ChunkPos(chunk.getX(), chunk.getZ()), false);
            }
        }

        sh.saveSchematic(vessel.name, clipboard);

        this.status = isSummon ? "SUMMON_COMPLETE" : "AWAITING_TIMER";
    }

    public void removeEntities(boolean reverse, Region region) {
        List<EntityVisitor> visitors = new ArrayList<>();

        LocalSession session = new LocalSession();
        EditSession editSession = session.createEditSession(adaptPlayer(initiatingPlayer));
        List<? extends Entity> entities;

//        List<Region> itemRemovalRegions = new ArrayList<>();
//        Harbor targetHarbor = this.vessel.harbors.get(reverse ? destinationHarborName : startingHarborName);
//
        World world = adapt(initiatingPlayer.world);
//        for (Region reg : this.vessel.getPiecesList(world, targetHarbor)) {
//            try {
//                reg.expand(BlockVector3.at(5, -reg.getMinimumPoint().getBlockY(), 5));
//                itemRemovalRegions.add(reg);
//            } catch (RegionOperationException e) {
//                e.printStackTrace();
//            }
//        }

        entities = editSession.getEntities(new RegionIntersection(world, region));

        visitors.add(new EntityVisitor(entities.iterator(), EntityRemover.fromString("items").createFunction()));
        try {
            for (EntityVisitor visitor : visitors) {
                Operations.complete(visitor);
            }
        } catch (WorldEditException e) {
            e.printStackTrace();
        }
//        session.remember(editSession);
        editSession.close();
    }

    public void remove(boolean reverse) {
        World world = adapt(initiatingPlayer.world);

        Harbor targetHarbor = this.vessel.harbors.get(reverse ? destinationHarborName : startingHarborName);

        // This is the RegionInstersection of all the pieces (which is all of them put together despite the name)
        RegionIntersection region = new RegionIntersection(world, this.vessel.getPiecesList(world, targetHarbor));
        for(BlockVector2 chunk : region.getChunks()) {
            initiatingPlayer.getServerWorld().getChunkManager().setChunkForced(new ChunkPos(chunk.getX(), chunk.getZ()), true);
        }

        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);

        clipboard.setOrigin(targetHarbor.helmLocation);
        try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, -1)) {
            ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                    editSession, region, clipboard, region.getMinimumPoint()
            );

            assert BlockTypes.AIR != null;
            forwardExtentCopy.setSourceFunction(new BlockReplace(editSession, BlockTypes.AIR.getDefaultState()));
            forwardExtentCopy.setRemovingEntities(true);

            // configure here
            Operations.complete(forwardExtentCopy);
        } catch (WorldEditException e) {
            e.printStackTrace();
        }

        removeEntities(reverse, region);

        if (reverse) {
            status = "START";
        }

        if (isSummon) {
            vessel.currentHarbor = vessel.harbors.get(reverse ? startingHarborName : destinationHarborName);
        }

        for(BlockVector2 chunk : region.getChunks()) {
            initiatingPlayer.getServerWorld().getChunkManager().setChunkForced(new ChunkPos(chunk.getX(), chunk.getZ()), false);
        }
    }

    public void removeStart() {
        remove(false);
    }

    public void removeDestination() {
        remove(true);
    }

    public boolean checkPlayerOnBoard(ServerPlayerEntity player, Harbor harbor) {
        RegionIntersection region = new RegionIntersection(harbor.world, this.vessel.getPiecesList(harbor.world, harbor));

        return region.contains(BlockVector3.at(player.getPos().getX(), player.getPos().getY(), player.getPos().getZ()));
    }

    public boolean transportPlayers(boolean reverse, Queue queue) {
        LOGGER.info("TRANSPORTING "+queue.getConfirmedPlayers().size()+" PLAYERS. REVERSE: " + reverse);
        Map<ServerPlayerEntity, Location> playerDestinations = new HashMap<>();
        Harbor sH = vessel.harbors.get(reverse ? destinationHarborName : startingHarborName);
        Harbor dH = vessel.harbors.get(reverse ? startingHarborName : destinationHarborName);
        ServerPlayerEntity captain = queue.getCaptain();

        for (ServerPlayerEntity player : queue.getConfirmedPlayers()) {
            if ((player.getDisplayName() == captain.getDisplayName() && queue.checkCaptainAtHelm()) || !checkPlayerOnBoard(player, sH)) {
                return false;
            }

            // Get all players' positions on the vessel and translate those locations to the new location
            Location dest = adaptPlayer(player).getLocation();
            double rotationAmount = VFTUtils.getRotationAmount(sH.direction, dH.direction);
            dest = dest.setPosition(BlockVector3.at(dest.getX(), dest.getY(), dest.getZ()).transform2D(-rotationAmount, sH.helmLocation.getBlockX(), sH.helmLocation.getBlockZ(), 0, 0).toVector3());
            dest = dest.setPosition(BlockVector3.at(dest.getX(), dest.getY()+.5, dest.getZ()).add((dH.helmLocation.subtract(sH.helmLocation))).toVector3());
            dest = dest.setYaw(dest.getYaw() - (float) rotationAmount);

            playerDestinations.put(player, dest);
        }

        // teleport everyone!
        for(ServerPlayerEntity player : playerDestinations.keySet()) {
            Location dest = playerDestinations.get(player);
//            player.setMotion(0,0,0);
//            player.setPosition(dest.getX(), dest.getY(), dest.getZ());
//            player.rotationYaw = dest.getYaw();
//            player.getServer().getCommandManager().handleCommand(player.getServer().getCommandSource(), "/tp " +player.getDisplayName().getString() +" "+ dest.getX() +" "+ dest.getY() +" "+ dest.getZ());
            player.setPos(dest.getX(), dest.getY(), dest.getZ());
            player.setYaw(dest.getYaw());
            player.setPitch(dest.getPitch());
        }

        this.vessel.currentHarbor = this.vessel.harbors.get(dH.name);
        return true;
    }

    public boolean transportPlayers(Queue queue) {
        return transportPlayers(false, queue);
    }

    public boolean returnPlayers(Queue queue) {
        return transportPlayers(true, queue);
    }

    public boolean isSummon() {
        return isSummon;
    }
}
