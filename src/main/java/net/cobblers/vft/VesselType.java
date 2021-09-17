package net.cobblers.vft;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.function.mask.*;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockCategory;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.HashSet;
import java.util.Locale;

public class VesselType {
    private String type = null;

    public VesselType(String type) {
        if (type.equals("air") || type.equals("sea") || type.equals("hybrid")) {
            this.type = type;
        }
    }

    public Mask getMask(EditSession editSession) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case "air":
                return AirVesselMask(editSession);
            case "sea":
                return SeaVesselMask(editSession);
            case "hybrid":
                return HybridVesselMask(editSession);
            default:
                return null;
        }
    }

    private Mask AirVesselMask(EditSession editSession) {
        return Masks.negate(new BlockTypeMask(editSession, BlockTypes.AIR));
    }

    private Mask SeaVesselMask(EditSession editSession) {
        Mask structuralBlockMask = getStructuralBlockMask(editSession);
        return new MaskUnion(
                    new MaskIntersection(
                            new MaskUnion(
                                    Masks.negate(new BlockTypeMask(editSession, BlockTypes.KELP, BlockTypes.KELP_PLANT, BlockTypes.WATER)),
                                    new MaskIntersection(
                                            new BlockTypeMask(editSession, BlockTypes.WATER),
                                            new OffsetMask(structuralBlockMask, BlockVector3.at(0,-1,0))
                                    )
                            ),
                            new ExistingBlockMask(editSession)),
                new MaskIntersection(
                        new BoundedHeightMask(0, 62),
                        Masks.negate(new ExistingBlockMask(editSession))
                )
        );
    }

    private Mask HybridVesselMask(EditSession editSession) {
        return null;
    }

    public String getType() {
        return type;
    }

    private Mask getStructuralBlockMask(EditSession editSession) {
        StructuralBlockList sbl = new StructuralBlockList();
        HashSet<BlockType> structuralBlockTypes = new HashSet<>();

        for (String tag : sbl.getBlockTagList()) {
            HashSet<BlockType> finalStructuralBlockTypes = structuralBlockTypes;
            structuralBlockTypes = new HashSet<BlockType>(){{
                addAll(finalStructuralBlockTypes);
                addAll(BlockCategory.REGISTRY.get(tag.toLowerCase(Locale.ROOT)).getAll());
            }};
        }

        for (String type : sbl.getBlockTypeList()) {
            structuralBlockTypes.add(BlockTypes.get(type.toLowerCase(Locale.ROOT)));
        }

        return new BlockTypeMask(editSession, structuralBlockTypes);
    }
}
