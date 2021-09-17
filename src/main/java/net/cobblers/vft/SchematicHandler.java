package net.cobblers.vft;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class SchematicHandler {
    private static String saveDir = "config/vft/schematics/";

    public SchematicHandler() {
        boolean init = false;

        File config = new File(saveDir);
        if (config.getParentFile().mkdirs()) {
            init = true;
        }
        else {
            System.out.println("FAILED TO CREATE DIRECTORY.");
        }
    }

    public boolean saveSchematic(String vesselName, Clipboard clipboard) {
        File file = new File(saveDir + vesselName + ".schem");
        file.getParentFile().mkdir();

//        try {
//            file.createNewFile();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


        try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(file))) {
            writer.write(clipboard);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
