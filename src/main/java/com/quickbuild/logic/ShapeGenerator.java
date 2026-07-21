package com.quickbuild.logic;

import com.quickbuild.config.QuickBuildConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public class ShapeGenerator {

    public static List<BlockPos> generateBuildShape(BlockPos center, Direction side, QuickBuildConfig.BuildShape shape, int size) {
        List<BlockPos> positions = new ArrayList<>();
        if (size < 1) return positions;

        int radius = size / 2;

        switch (shape) {
            case PLANE:
                // Generate flat plane facing player's target side
                Direction.Axis axis = side.getAxis();
                for (int u = -radius; u <= radius; u++) {
                    for (int v = -radius; v <= radius; v++) {
                        BlockPos pos;
                        if (axis == Direction.Axis.Y) {
                            pos = center.offset(u, 0, v);
                        } else if (axis == Direction.Axis.X) {
                            pos = center.offset(0, u, v);
                        } else {
                            pos = center.offset(u, v, 0);
                        }
                        positions.add(pos);
                    }
                }
                break;

            case CIRCLE:
                // Circle on horizontal plane (or wall face)
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (x * x + z * z <= radius * radius) {
                            positions.add(center.offset(x, 0, z));
                        }
                    }
                }
                break;

            case SPHERE:
                // Solid Sphere
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            if (x * x + y * y + z * z <= radius * radius) {
                                positions.add(center.offset(x, y, z));
                            }
                        }
                    }
                }
                break;

            case HOLLOW_SPHERE:
                // Hollow Sphere shell (outer radius vs inner radius)
                int innerRadiusSq = (radius - 1) * (radius - 1);
                int outerRadiusSq = radius * radius;
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            int distSq = x * x + y * y + z * z;
                            if (distSq <= outerRadiusSq && (radius <= 1 || distSq >= innerRadiusSq)) {
                                positions.add(center.offset(x, y, z));
                            }
                        }
                    }
                }
                break;

            case CYLINDER:
                // Vertical Cylinder
                int height = size;
                for (int y = 0; y < height; y++) {
                    for (int x = -radius; x <= radius; x++) {
                        for (int z = -radius; z <= radius; z++) {
                            if (x * x + z * z <= radius * radius) {
                                positions.add(center.offset(x, y, z));
                            }
                        }
                    }
                }
                break;
        }

        return positions;
    }

    public static List<BlockPos> generateMiningArea(BlockPos center, Direction side, int size) {
        List<BlockPos> positions = new ArrayList<>();
        int half = size / 2;
        Direction.Axis axis = side.getAxis();

        for (int u = -half; u <= (size - 1 - half); u++) {
            for (int v = -half; v <= (size - 1 - half); v++) {
                BlockPos pos;
                if (axis == Direction.Axis.Y) {
                    pos = center.offset(u, 0, v);
                } else if (axis == Direction.Axis.X) {
                    pos = center.offset(0, u, v);
                } else {
                    pos = center.offset(u, v, 0);
                }
                positions.add(pos);
            }
        }
        return positions;
    }
}
