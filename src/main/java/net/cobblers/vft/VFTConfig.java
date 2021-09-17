package net.cobblers.vft;

import com.google.gson.Gson;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Direction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class VFTConfig {
    private static final Logger LOGGER = LogManager.getLogger();

    protected final String[] constraints = {"STERN", "BOW", "PORT", "STARBOARD", "TOP", "BOTTOM"};

    private final String VesselsPath = "config/vft/";
    protected Map<String, Vessel> vessels = new HashMap<>();

    public VFTConfig() {
        boolean init = false;

        File config = new File(VesselsPath);
        if (config.mkdirs()) {
            init = true;
        }

        Gson gson = new Gson();
        String[] fileList = config.list();
        if (fileList != null && fileList.length > 0) {
            for (String vesselName : fileList) {
                String[] fileParts = vesselName.split("\\.");
                if (fileParts.length > 1 && fileParts[fileParts.length-1].equals("vessel")) {
                    try (FileReader vslReader = new FileReader(VesselsPath + vesselName))
                    {
                        LOGGER.info("FOUND FILE " + vesselName + " and added it to the config.");
                        this.vessels.put(fileParts[0], gson.fromJson(vslReader, Vessel.class));

                    } catch (IOException e) {
                        e.printStackTrace();
                        LOGGER.info("Failed to load Vessel Config.");
                    }
                }
            }
        }
    }

    public void createVessel(String vesselName, Direction playerDir, BlockVector3 helmLocation) {
        if (this.vessels.containsKey(vesselName)) {
            LOGGER.error("A vessel named " + vesselName + " already exists.");
            return;
        }
        Vessel newVessel = new Vessel(vesselName);
        newVessel.addPiece("base");
        newVessel.pieces.get("base").setPos(BlockVector3.ZERO, BlockVector3.ZERO);
        newVessel.addHarbor("main", playerDir.toString(), helmLocation);
        newVessel.currentHarbor = newVessel.harbors.get("main");

        this.vessels.put(vesselName, newVessel);
        updateFile(vesselName);
    }

    public boolean deleteVessel(String vesselName) {
        if (!vessels.containsKey(vesselName)) {
            return false;
        }
        vessels.remove(vesselName);
        File f = new File(VesselsPath + vesselName + ".vessel");
        updateFiles();

        return f.delete();
    }

    public boolean setConstraint(String vesselName, String pieceName, String constraint, BlockVector3 location) {
        if (!this.vessels.containsKey(vesselName)) {
            LOGGER.error("No vessel named " + vesselName + " exists.");
            return false;
        }
        Vessel vessel = this.vessels.get(vesselName);

        switch (constraint) {
            case "TOP":
                vessel.pieces.get(pieceName).pos2 = vessel.pieces.get(pieceName).pos2.withY(location.getBlockY()-vessel.currentHarbor.helmLocation.getBlockY()-1);
                updateFile(vesselName);
                return true;
            case "BOTTOM":
                vessel.pieces.get(pieceName).pos1 = vessel.pieces.get(pieceName).pos1.withY(location.getBlockY()-vessel.currentHarbor.helmLocation.getBlockY());
                if (location.getBlockY() < 62) { // Bottom of ship is below sea-level, assume it is a SeaVessel
                    vessel.type = new VesselType("sea");
                } else {
                    vessel.type = new VesselType("air");
                }
                updateFile(vesselName);
                return true;
        }

        BlockVector3 normalizedLocation = location.transform2D(-VFTUtils.getRotationAmount(vessel.currentHarbor.direction, "EAST"),
                vessel.currentHarbor.helmLocation.getBlockX(), vessel.currentHarbor.helmLocation.getBlockZ(), 0,0).subtract(vessel.currentHarbor.helmLocation);

        LOGGER.info("Harbor is facing " + vessel.currentHarbor.direction + ".");
        LOGGER.info("Rotating by " + VFTUtils.getRotationAmount(vessel.currentHarbor.direction, "EAST"));
        LOGGER.info(location.transform2D(VFTUtils.getRotationAmount(vessel.currentHarbor.direction, "EAST"),
                vessel.currentHarbor.helmLocation.getBlockX(), vessel.currentHarbor.helmLocation.getBlockZ(), 0,0));
        LOGGER.info("East facing origin: " + normalizedLocation);

        switch (constraint) {
            case "BOW":
                vessel.pieces.get(pieceName).pos2 = vessel.pieces.get(pieceName).pos2.withX(normalizedLocation.getBlockX());
                break;
            case "STARBOARD":
                vessel.pieces.get(pieceName).pos2 = vessel.pieces.get(pieceName).pos2.withZ(normalizedLocation.getBlockZ());
                break;
            case "STERN":
                vessel.pieces.get(pieceName).pos1 = vessel.pieces.get(pieceName).pos1.withX(normalizedLocation.getBlockX());
                break;
            case "PORT":
                vessel.pieces.get(pieceName).pos1 = vessel.pieces.get(pieceName).pos1.withZ(normalizedLocation.getBlockZ());
                break;
            default:
                return false;
        }
        updateFile(vesselName);
        return true;
    }

    public boolean createHarbor(String vesselName, String harborName, Direction direction, BlockVector3 helmLocation, String anchorName) {
        Vessel vessel = this.vessels.get(vesselName);
        if (vessel.harbors.containsKey(harborName)) {
            LOGGER.info("A harbor already exists with name " + harborName + ".");
            return false;
        }
        if (anchorName != null && !vessel.anchors.containsKey(anchorName)) {
            LOGGER.info("No anchor with name " + anchorName + ".");
            return false;
        }

        if (anchorName != null) {
            vessel.addHarbor(harborName, direction.toString(), helmLocation, anchorName);
        } else {
            vessel.addHarbor(harborName, direction.toString(), helmLocation);
        }

        updateFile(vesselName);
        return true;
    }

    public boolean deleteHarbor(String vesselName, String harborName) {
        if (!vessels.containsKey(vesselName) || !vessels.get(vesselName).harbors.containsKey(harborName)) {
            return false;
        }
        vessels.get(vesselName).harbors.remove(harborName);
        updateFile(vesselName);

        return true;
    }

    public boolean createAnchor(String vesselName, String anchorName, BlockVector3 location, Direction direction) {
        if (!vessels.containsKey(vesselName)) {
            LOGGER.info("No vessel named " + vesselName + " exists.");
            return false;
        }
        Vessel vessel = this.vessels.get(vesselName);

        vessel.addAnchor(location, direction.toString(), anchorName);
        updateFile(vesselName);

        return true;
    }

    public boolean deleteAnchor(String vesselName, String anchorName) {
        if (!vessels.containsKey(vesselName) || !vessels.get(vesselName).anchors.containsKey(anchorName)) {
            return false;
        }
        vessels.get(vesselName).anchors.remove(anchorName);
        updateFile(vesselName);

        return true;
    }

    public boolean createPiece(String vesselName, String pieceName) {
        Vessel vessel = this.vessels.get(vesselName);
        if (vessel.pieces.containsKey(pieceName)) {
            LOGGER.error("A piece already exists with name: " + pieceName + ".");
            return false;
        }
        vessel.addPiece(pieceName);
        updateFile(vesselName);
        return true;
    }

    public boolean deletePiece(String vesselName, String pieceName) {
        if (!vessels.containsKey(vesselName) || !vessels.get(vesselName).pieces.containsKey(pieceName)) {
            return false;
        }
        vessels.get(vesselName).pieces.remove(pieceName);
        updateFile(vesselName);

        return true;
    }

    public void updateFile(String vesselName) {
        if (!vessels.containsKey(vesselName)) {
            LOGGER.error("No vessel to save with name: " + vesselName + ".");
            return;
        }

        Gson gson = new Gson();
        try {
            FileWriter vesselFile = new FileWriter(VesselsPath + vesselName + ".vessel");
            vesselFile.write(gson.toJson(vessels.get(vesselName)));
            vesselFile.flush();
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.info("Failed to create vessel save file.");
        }
    }

    public void updateFiles() {
        for(String vessel : vessels.keySet()) {
            updateFile(vessel);
        }
    }

    public Vessel getVessel(String vesselName) {
        return vessels.get(vesselName);
    }

    public Boolean existsVessel(String vesselName) {
        return vessels.containsKey(vesselName);
    }
}
