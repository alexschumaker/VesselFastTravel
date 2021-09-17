package net.cobblers.vft;

import com.sk89q.worldedit.util.Location;
import net.fabricmc.api.ModInitializer;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.sk89q.worldedit.fabric.FabricPlayer;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Direction;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.network.MessageType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Timer;

import static com.sk89q.worldedit.fabric.FabricAdapter.adaptPlayer;
import static java.lang.Math.floor;

public class VesselFastTravel implements ModInitializer {

    private final VFTConfig config = new VFTConfig();
    private VesselTransport sailAction = null;
    private Queue queue = null;

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            System.out.println(dedicated);

            dispatcher.register(literal("vft")
                    .then(literal("create")
                            .then(argument("vesselName", word())
                                    .executes(context -> {
                                        createVessel(context);
                                        return 1;
                                    })))
                    .then(literal("set")
                            .then(argument("vesselName", word())
                                    .then(argument("constraint", word())
                                            .then(argument("piece", word())
                                                    .executes(context -> {
                                                        setConstraint(context);
                                                        return 1;
                                                    }))
                                            .executes(context -> {
                                                setConstraint(context);
                                                return 1;
                                            }))))
                    .then(literal("delete")
                            .then(literal("vessel")
                                    .then(argument("vesselName", word())
                                            .executes(context -> {
                                                deleteVessel(context.getSource().getPlayer(), context.getArgument("vesselName", String.class));
                                                return 1;
                                            })))
                            .then(literal("harbor")
                                    .then(argument("vesselName", word())
                                            .then(argument("harborName", word())
                                                    .executes(context -> {
                                                        deleteHarbor(context.getSource().getPlayer(),
                                                                context.getArgument("vesselName", String.class),
                                                                context.getArgument("harborName", String.class));
                                                        return 1;
                                                    }))))
                            .then(literal("piece")
                                    .then(argument("vesselName", word())
                                            .then(argument("pieceName", word())
                                                    .executes(context -> {
                                                        deletePiece(context.getSource().getPlayer(),
                                                                context.getArgument("vesselName", String.class),
                                                                context.getArgument("pieceName", String.class));
                                                        return 1;
                                                    }))))
                            .then(literal("anchor")
                                    .then(argument("vesselName", word())
                                            .then(argument("anchorName", word())
                                                    .executes(context -> {
                                                        deleteAnchor(context.getSource().getPlayer(),
                                                                context.getArgument("vesselName", String.class),
                                                                context.getArgument("anchorName", String.class));
                                                        return 1;
                                                    })))))
                    .then(literal("harbor") // /vft harbor <vesselName> <harborName> [anchorName]
                            .then(argument("vesselName", word())
                                    .then(argument("harborName", word())
                                            .then(argument("anchorName", word())
                                                    .executes(context -> {
                                                        createHarbor(context.getSource().getPlayer(),
                                                                context.getArgument("vesselName", String.class),
                                                                context.getArgument("harborName", String.class),
                                                                context.getArgument("anchorName", String.class));
                                                        return 1;
                                                    }))
                                            .executes(context -> {
                                                createHarbor(context.getSource().getPlayer(),
                                                        context.getArgument("vesselName", String.class),
                                                        context.getArgument("harborName", String.class),
                                                        null);
                                                return 1;
                                            }))))
                    .then(literal("piece")
                            .then(argument("vesselName", word())
                                    .then(argument("pieceName", word())
                                            .executes(context -> {
                                                createPiece(context.getSource().getPlayer(),
                                                        context.getArgument("vesselName", String.class),
                                                        context.getArgument("pieceName", String.class));
                                                return 1;
                                            }))))
                    .then(literal("anchor")
                            .then(argument("vesselName", word())
                                    .then(argument("anchorName", word())
                                            .executes(context -> {
                                                createAnchor(context.getSource().getPlayer(),
                                                        context.getArgument("vesselName", String.class),
                                                        context.getArgument("anchorName", String.class));
                                                return 1;
                                            }))))
                    .then(literal("type")
                            .then(argument("vesselName", word())
                                    .then(argument("vesselType", word())
                                            .executes(context -> {
                                                setVesselType(context.getSource().getPlayer(),
                                                        context.getArgument("vesselName", String.class),
                                                        context.getArgument("vesselType", String.class));
                                                return 1;
                                            }))))
                    .then(literal("summon")
                            .then(argument("vesselName", word())
                                    .executes(context -> {
                                        summonVessel(context.getSource().getPlayer(),
                                                context.getArgument("vesselName", String.class));
                                        return 1;
                                    })))
                    .then(literal("sail")
                            .then(argument("vesselName", word())
                                    .then(argument("destination", word())
                                            .executes(context -> {
                                                initVoyage(context.getSource().getPlayer(),
                                                        context.getArgument("vesselName", String.class),
                                                        context.getArgument("destination", String.class));
                                                return 1;
                                            }))))
                    .then(literal("current")
                            .then(argument("vesselName", word())
                                    .then(argument("harborName", word())
                                            .executes(context -> {
                                                String vesselName = context.getArgument("vesselName", String.class).toLowerCase();
                                                this.config.getVessel(vesselName).setCurrentHarbor(context.getArgument("harborName", String.class).toLowerCase());
                                                this.config.updateFile(vesselName);
                                                return 1;
                                            }))))
                    .then(literal("ls") // /vft ls | /vft ls vessels | /vft ls <vesselName> <harbors | pieces>"
                            .executes(context -> {
                                listContent(context.getSource().getPlayer(), false, null, null);
                                return 1;
                            })
                            .then(literal("vessels")
                                    .executes(context -> {
                                        listContent(context.getSource().getPlayer(), true, null, null);
                                        return 1;
                                    }))
                            .then(argument("vesselName", word())
                                    .then(argument("subList", word())
                                            .executes(context -> {
                                                listContent(context.getSource().getPlayer(), false,
                                                        context.getArgument("vesselName", String.class),
                                                        context.getArgument("subList", String.class));
                                                return 1;
                                            }))))
                    .then(literal("undo")
                            .executes(context -> {
                                voyageCommands(context.getSource().getPlayer(), "undo");
                                return 1;
                            }))
                    .then(literal("done")
                            .executes(context -> {
                                voyageCommands(context.getSource().getPlayer(), "done");
                                return 1;
                            }))
                    .executes(context -> {
                        ServerPlayerEntity sender = context.getSource().getPlayer();
                        Location pos = adaptPlayer(sender).getBlockTrace(3);
                        String msg = pos.getBlockX() + ", " + pos.getBlockY() + ", " + pos.getBlockZ();
                        LOGGER.info(msg);
                        systemMsgToPlayer(sender, msg);
                        return 1;
                    }));

            // Queue commands
            dispatcher.register(literal("ready")
                    .executes(context -> {
                        queueCommands(context.getSource().getPlayer(), "ready");
                        return 1;
                    }));
            dispatcher.register(literal("cancel")
                    .executes(context -> {
                        queueCommands(context.getSource().getPlayer(), "cancel");
                        return 1;
                    }));
            dispatcher.register(literal("pass")
                    .executes(context -> {
                        queueCommands(context.getSource().getPlayer(), "pass");
                        return 1;
                    }));
        });
    }

    private void createVessel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity sender = context.getSource().getPlayer();
        FabricPlayer player = adaptPlayer(sender);
        BlockVector3 helmLocation = BlockVector3.at(player.getBlockTrace(3).getBlockX(), player.getBlockTrace(3).getBlockY(), player.getBlockTrace(3).getBlockZ());
        Direction playerDir = player.getCardinalDirection();

        if (helmLocation == null || !playerDir.isCardinal()) {
            systemMsgToPlayer(sender, "Stand targeting the helm, facing the front of the Vessel.");
            return;
        }

        String vesselName = context.getArgument("vesselName", String.class);
        config.createVessel(vesselName, playerDir, helmLocation);
    }

    private void setConstraint(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity sender = context.getSource().getPlayer();
        Vec3d playerLoc = sender.getPos();
        String vesselName = context.getArgument("vesselName", String.class);
        String constraint = context.getArgument("constraint", String.class).toUpperCase();

        if (!config.existsVessel(vesselName)) {
            context.getSource().sendFeedback(new LiteralText("No vessel named \"" + vesselName + "\" exists."), false);
            return;
        }

        if (!Arrays.asList(config.constraints).contains(constraint)) {
            context.getSource().sendFeedback(new LiteralText("Invalid constraint. Use top, bottom, port, starboard, bow, or stern. Google it."), false);
            return;
        }

        String pieceArg;
        try {
            pieceArg = context.getArgument("piece", String.class);
        }
        catch(IllegalArgumentException e) {
            pieceArg = "base";
        }

        if (config.setConstraint(vesselName, pieceArg, constraint, BlockVector3.at(playerLoc.getX(), playerLoc.getY(), playerLoc.getZ()))) {
            systemMsgToPlayer(sender, "The " + constraint + " end of " + vesselName + " has been set successfully!");
        } else {
            systemMsgToPlayer(sender, "An error has occurred :(");
        }
    }

    private void deleteVessel(ServerPlayerEntity sender, String vesselName) {
        if (config.existsVessel(vesselName)) {
            if (config.deleteVessel(vesselName)) {
                systemMsgToPlayer(sender, "Successfully deleted data for " + vesselName + ".");
            }
        }
        else {
            systemMsgToPlayer(sender, "Failed to delete data for " + vesselName + ".");
        }
    }

    private void createHarbor(ServerPlayerEntity sender, String vesselName, String harborName, String anchorName) {
        FabricPlayer player = adaptPlayer(sender);
        BlockVector3 helmLocation = BlockVector3.at(player.getBlockTrace(3).getBlockX(), player.getBlockTrace(3).getBlockY(), player.getBlockTrace(3).getBlockZ());
        Direction playerDir = player.getCardinalDirection();
        if (helmLocation == null || !playerDir.isCardinal()) {
            systemMsgToPlayer(sender, "Stand targeting the helm or anchor, facing way you'd like the vessel to face.");
            return;
        }

        if (config.createHarbor(vesselName, harborName, playerDir, helmLocation, anchorName)) {
            systemMsgToPlayer(sender, "Harbor created sucessfully!");
        }
        else {
            systemMsgToPlayer(sender, "A harbor already exists with name " + harborName + ".");
        }
    }

    private void deleteHarbor(ServerPlayerEntity sender, String vesselName, String harborName) {
        if (config.deleteHarbor(vesselName, harborName)) {
            systemMsgToPlayer(sender, "Successfully deleted the " + harborName + " harbor data for " + vesselName + ".");
        } else {
            systemMsgToPlayer(sender, "Failed to delete the " + harborName + " harbor data for " + vesselName + ".");
        }
    }

    private void createAnchor(ServerPlayerEntity sender, String vesselName, String anchorName) {
        FabricPlayer player = adaptPlayer(sender);
        BlockVector3 helmLocation = BlockVector3.at(player.getBlockTrace(3).getBlockX(), player.getBlockTrace(3).getBlockY(), player.getBlockTrace(3).getBlockZ());
        Direction playerDir = player.getCardinalDirection();
        if (helmLocation == null || !playerDir.isCardinal()) {
            systemMsgToPlayer(sender, "Stand targeting the anchor, facing way you'd like the anchor to be relative to the vessel.");
            return;
        }

        if (config.createAnchor(vesselName, anchorName, helmLocation, playerDir)) {
            systemMsgToPlayer(sender, "Anchor created sucessfully!");
        }
        else {
            systemMsgToPlayer(sender, "No vessel named " + vesselName + " exists.");
        }
    }

    private void deleteAnchor(ServerPlayerEntity sender, String vesselName, String anchorName) {
        if (config.deleteAnchor(vesselName, anchorName)) {
            systemMsgToPlayer(sender, "Successfully deleted the " + anchorName + " anchor data for " + vesselName + ".");
        } else {
            systemMsgToPlayer(sender, "Failed to delete the " + anchorName + " anchor data for " + vesselName + ".");
        }
    }

    private void createPiece(ServerPlayerEntity sender, String vesselName, String pieceName) {
        if (config.createPiece(vesselName, pieceName)) {
            systemMsgToPlayer(sender, "Piece created sucessfully!");
        }
        else {
            systemMsgToPlayer(sender, "A piece already exists with name " + pieceName + ".");
        }
    }

    private void deletePiece(ServerPlayerEntity sender, String vesselName, String pieceName) {
        if (config.deletePiece(vesselName, pieceName)) {
            systemMsgToPlayer(sender, "Successfully deleted the " + pieceName + " harbor data for " + vesselName + ".");
        } else {
            systemMsgToPlayer(sender, "Failed to delete the " + pieceName + " harbor data for " + vesselName + ".");
        }
    }

    private void setVesselType(ServerPlayerEntity sender, String vesselName, String vesselType) {
        if (!config.existsVessel(vesselName)) {
            systemMsgToPlayer(sender, "No vessel named "+ vesselName +".");
            return;
        }

        if (!config.vessels.get(vesselName).setType(vesselType)) {
            systemMsgToPlayer(sender, "Vessel type \""+ vesselType +"\" not supported.");
            return;
        }

        config.updateFile(vesselName);
        LOGGER.info(vesselName);
        LOGGER.info(config.vessels.get(vesselName).type.getType());
        LOGGER.info(config.vessels.get(vesselName));
    }

    private void initVoyage(ServerPlayerEntity sender, String vesselName, String destination) {
        Vessel vessel = config.getVessel(vesselName);
        this.sailAction = new VesselTransport(vessel, destination, sender, false);
        this.queue = new Queue(sender, vessel);

        if (sailAction.status.equals("ABORT") || queue.getStatus().equals("CANCELLED")) {
            sailAction = null;
            queue = null;
            return;
        }

        systemMsgToServer(sender, sender.getDisplayName().getString() + " wants to make a voyage to " + destination + ". To come along, climb aboard the " + vesselName + " and type /ready. Otherwise, type /pass.");

        checkQueue();
    }

    private void queueCommands(ServerPlayerEntity sender, String command) {
        if (queue != null) {
            queue.commandParser(sender, command);
        }
        else {
            systemMsgToPlayer(sender, "No active voyage.");
        }
    }

    private void voyageCommands(ServerPlayerEntity sender, String command) {
        if ((queue != null && queue.getStatus().equals("COMPLETED")) || (sailAction != null && sailAction.isSummon())) {
            switch (command) {
                case "undo" -> undo(sender);
                case "done" -> finish(sender);
                default -> systemMsgToPlayer(sender, "Finalize or undo the current voyage.");
            }
        }
        else {
            systemMsgToPlayer(sender, "No active vessel transport.");
        }
    }

    private void setSail() {
        sailAction.sail();
        Timer timer = new Timer();
        timer.schedule(new VoyageTimer(sailAction, queue), 0,1000);
    }

    private void systemMsgToPlayer(ServerPlayerEntity player, String msg) {
        player.sendMessage(new LiteralText(msg), false);
    }

    public void systemMsgToServer(ServerPlayerEntity initializer, String msg) {
        initializer.getServer().getPlayerManager().broadcastChatMessage(new LiteralText(msg), MessageType.SYSTEM, Util.NIL_UUID);
    }

    private void undo(ServerPlayerEntity player) {
        if (sailAction == null) {
            systemMsgToPlayer(player, "No current sailing route to undo.");
            return;
        }

        if (sailAction.status == "SUMMON_COMPLETE") {
            sailAction.removeDestination();
            sailAction = null;
            return;
        }

        if (sailAction.status.equals("DESTINATION")) {
            if (sailAction.returnPlayers(queue)) {
                sailAction.removeDestination();
                sailAction = null;
                queue = null;
            } else {
                systemMsgToServer(player, player.getDisplayName().getString() + " is trying to undo this Voyage, but can't until all players are on board.");
            }
        } else {
            systemMsgToPlayer(player, "Can't undo until after teleportation.");
        }
    }

    private void finish(ServerPlayerEntity player) {
        if (sailAction == null) {
            systemMsgToPlayer(player, "No voyage to finalize.");
            return;
        }
        systemMsgToPlayer(player, "Finalizing transport.");
        if (sailAction.status.equals("DESTINATION") || sailAction.status.equals("SUMMON_COMPLETE")) {
            sailAction.removeStart();
        }

        config.updateFile(sailAction.activeVesselName);
        sailAction = null;
        queue = null;
    }

    // Summons the specified vessel to the nearest harbor
    private void summonVessel(ServerPlayerEntity sender, String vesselName) {
        if (sailAction != null) {
            systemMsgToPlayer(sender, "Voyage in progress; can't summon now!");
            return;
        }

        if (!config.vessels.containsKey(vesselName)) {
            systemMsgToPlayer(sender, "No vessel named " + vesselName + " found.");
            return;
        }

        Vessel vessel = config.vessels.get(vesselName);
        VFTUtils.harborProximity prox = VFTUtils.getClosestHarbor(vessel, sender);

        if (prox.distance > 60) {
            systemMsgToPlayer(sender, "You are " + floor(prox.distance) + " blocks away from the nearest harbor. Must be within 60 to summon.");
            return;
        }

        sailAction = new VesselTransport(vessel, prox.harbor.name, sender, true);

        if (sailAction.status.equals("ABORT")) {
            sailAction = null;
            return;
        }

        sailAction.sail();
    }

    private void listContent(ServerPlayerEntity sender, boolean vessels, String vesselName, String subList) {
        if (!vessels && vesselName == null) {
            Vessel currentVessel = VFTUtils.getBoardedVessel(config, sender);
            if (currentVessel != null) {
                systemMsgToPlayer(sender, "You are currently aboard \"" + currentVessel.name + "\".");
                systemMsgToPlayer(sender, " \"" + currentVessel.name + "\" is made up of these pieces: ");
                for(String piece : currentVessel.pieces.keySet()) {
                    systemMsgToPlayer(sender, "   - " + piece);
                }
                systemMsgToPlayer(sender, " \"" + currentVessel.name + "\" has these harbors: ");
                for(String harbor : currentVessel.harbors.keySet()) {
                    systemMsgToPlayer(sender, "   - " + harbor + (harbor.equals(currentVessel.currentHarbor.name) ? " <- current" : ""));
                }
            } else {
                systemMsgToPlayer(sender, "Cannot find boarded vessel. Either it hasn't been created or is misplaced (try using /vft current <vesselName> <harborName>).");
            }
        } else if (vessels) {
            systemMsgToPlayer(sender, "Your Vessels: ");
            for(String vessel : config.vessels.keySet()) {
                systemMsgToPlayer(sender, "   - " + vessel);
            }
        } else if (subList != null) {
            if (subList.equals("harbors")) {
                systemMsgToPlayer(sender, "Harbors for " + vesselName + ": ");
                for(String harbor : config.vessels.get(vesselName).harbors.keySet()) {
                    systemMsgToPlayer(sender, "   - " + harbor);
                }
            } else if (subList.equals("pieces")) {
                systemMsgToPlayer(sender, "Pieces for " + vesselName + ": ");
                for(String piece : config.vessels.get(vesselName).pieces.keySet()) {
                    systemMsgToPlayer(sender, "   - " + piece);
                }
            } else {
                systemMsgToPlayer(sender, "No vessel named " + vesselName);
            }
        } else {
            systemMsgToPlayer(sender, "Invalid argument. /vft ls vessels | /vft ls <vesselName> <harbors | pieces>");
        }
    }

    private void checkQueue() {
        // check queue.status and act accordingly
        String status = queue.getStatus();

        if (status.equals("CANCELLED") && sailAction.status.equals("START")) {
            queue = null;
            sailAction = null;
        } else if (status.equals("CANCELLED") && sailAction.status.equals("AWAITING_TIMER")) {
            sailAction.removeDestination();
            queue = null;
            sailAction = null;
        } else if (status.equals("READY")) {
            setSail();
        }
    }
}
// TODO schematic loading, better undo, glass pane testHarbor, nudging/anchor adaptation