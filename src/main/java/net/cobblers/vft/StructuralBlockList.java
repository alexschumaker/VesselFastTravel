package net.cobblers.vft;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class StructuralBlockList {
    private static final Logger LOGGER = LogManager.getLogger();

    private final String blockListPath = "config/vft/structuralBlocks.txt";
    protected ArrayList<String> blockTags = new ArrayList<String>();
    protected ArrayList<String> blockTypes = new ArrayList<String>();

    public StructuralBlockList() {
        try {
            if (new File(blockListPath).createNewFile()) {
                initList();
            }

            FileReader r = new FileReader(blockListPath);
            if (r.read() == -1) {
                initList();
            }
            r.close();

            Scanner reader = new Scanner(new File(blockListPath));
            boolean tags = true;
            while (reader.hasNextLine()) {
                String ln = reader.nextLine();
                if (ln.equals("===="))  {
                    tags = false;
                    if (reader.hasNextLine()) {
                        ln = reader.nextLine();
                    } else {
                        break;
                    }
                }

                if (tags) {
                    blockTags.add(ln);
                } else {
                    blockTypes.add(ln);
                }
            }
            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void initList() {
        try {
            FileWriter defaultWriter = new FileWriter(blockListPath);
            String defaults = "minecraft:planks\n" +
                    "minecraft:wooden_slabs\n" +
                    "minecraft:wooden_stairs\n" +
                    "minecraft:logs\n" +
                    "minecraft:impermeable";

            defaultWriter.write(defaults);
            defaultWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> getBlockTagList() {
        return blockTags;
    }

    public ArrayList<String> getBlockTypeList() {
        return blockTypes;
    }
}
