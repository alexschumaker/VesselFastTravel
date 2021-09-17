package net.cobblers.vft;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.RegionIntersection;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class Queue {
    private static final Logger LOGGER = LogManager.getLogger();
    private List<ServerPlayerEntity> playerList;
    private final ServerPlayerEntity captain;
    private final Vessel vessel;

    private int awaitingResponse;
    private Map<String, ServerPlayerEntity> readyPlayers = new HashMap<>();
    private Map<String, ServerPlayerEntity> passPlayers = new HashMap<>();

    private boolean cancelled = false;
    private boolean completed = false;

    public Queue(ServerPlayerEntity captain, Vessel vessel) {
        playerList = captain.getServer().getPlayerManager().getPlayerList();
        this.captain = captain;
        this.vessel = vessel;

        readyPlayers.put(captain.getDisplayName().getString(), captain);
        this.awaitingResponse = playerList.size() - 1;

        if (!checkCaptainAtHelm()) {
            systemMsgToPlayer(captain, "Must be at the helm of the vessel you wish to sail.");
            cancelled = true;
        }
    }

    public void commandParser(ServerPlayerEntity sender, String command) {
        LOGGER.info("Queue Command Received.");

        if (completed) {
            systemMsgToPlayer(sender,"Voyage has already been completed. Inspect your vessel to make sure everything is good and send </vft done>. If you find an issue, return using </vft undo>.");
            return;
        }

        switch (command) {
            case "/ready" -> playerReady(sender);
            case "/cancel" -> playerCancel(sender);
            case "/pass" -> playerPass(sender);
        }
    }

    private void playerReady(ServerPlayerEntity sender) {
        String name = sender.getDisplayName().getString();
        if (!readyPlayers.containsKey(name)) {
            if (sender.getDisplayName().getString().equals(captain.getDisplayName().getString())) {
                if (!checkCaptainAtHelm()) {
                    systemMsgToPlayer(sender, "The captain must be at the helm!");
                    return;
                }
            } else if (!checkPlayerOnBoard(sender)) {
                systemMsgToPlayer(sender, "You must board the vessel!");
                return;
            }
            if (passPlayers.containsKey(name)) {
                passPlayers.remove(name);
                awaitingResponse += 1;
            }
            readyPlayers.put(name, sender);
            awaitingResponse -= 1;

            systemMsgToPlayer(sender, "Ready!");
            systemMsgToServer(name + " is ready! " + (playerList.size() - awaitingResponse) + "/" + (playerList.size() - passPlayers.size()));
        }
    }

    private void playerCancel(ServerPlayerEntity sender) {
        systemMsgToServer(sender.getDisplayName().getString() + " has cancelled the crew's voyage plans.");
        this.cancelled = true;
    }

    private void playerPass(ServerPlayerEntity sender) {
        String name = sender.getDisplayName().getString();

        if (sender.getDisplayName().getString().equals(captain.getDisplayName().getString())) {
            systemMsgToPlayer(sender, "You cannot pass on your own voyage!");
            return;
        }

        if (!passPlayers.containsKey(name)) {
            if (readyPlayers.containsKey(name)) {
                readyPlayers.remove(name);
                awaitingResponse += 1;
            }

            passPlayers.put(name, sender);
            awaitingResponse -= 1;

            systemMsgToPlayer(sender, "Passed...");
            systemMsgToServer(name + " has passed. " + (playerList.size() - awaitingResponse) + "/" + (playerList.size() - passPlayers.size()));
        }
    }

    public void complete() {
        completed = true;
    }

    public String getStatus() {
        if (cancelled) {
            return "CANCELLED";
        } else if (completed) {
            return "COMPLETED";
        } else if (awaitingResponse == 0) {
            return "READY";
        } else {
            return "WAITING";
        }
    }

    public ServerPlayerEntity getCaptain() {
        return captain;
    }

    public void resetQueue() {
        playerList = captain.getServer().getPlayerManager().getPlayerList();
        awaitingResponse = playerList.size();

        readyPlayers = new HashMap<>();
        passPlayers = new HashMap<>();

        systemMsgToServer("Not all sailors on board! The queue has been reset. (0/"+ playerList.size() + " ready)");
    }

    public Collection<ServerPlayerEntity> getConfirmedPlayers() {
        return readyPlayers.values();
    }

    public boolean checkPlayerOnBoard(ServerPlayerEntity player) {
        RegionIntersection region = new RegionIntersection(this.vessel.currentHarbor.world, this.vessel.getPiecesList(this.vessel.currentHarbor.world, this.vessel.currentHarbor));
        Vec3d pos = player.getPos();
        return region.contains(BlockVector3.at(pos.getX(), pos.getY(), pos.getZ()));
    }

    public boolean checkCaptainAtHelm() {
        Vec3d pos = captain.getPos();
        return (vessel.currentHarbor.helmLocation.distance(BlockVector3.at(pos.getX(), pos.getY(), pos.getZ())) < 3);
    }

    private void systemMsgToPlayer(ServerPlayerEntity player, String msg) {
        player.sendMessage(new LiteralText(msg), false);
    }

    public void systemMsgToServer(String msg) {
        Objects.requireNonNull(captain.getServer()).sendSystemMessage(new LiteralText(msg), Util.NIL_UUID);
    }
}
