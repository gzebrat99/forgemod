package com.quickbuild.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class QuickBuildConfig {

    public static class Server {
        public final ForgeConfigSpec.IntValue maxVeinBlocks;
        public final ForgeConfigSpec.IntValue maxBuildArea;
        public final ForgeConfigSpec.IntValue blocksPerTick;

        public Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Quick Build & Vein Miner Server Settings").push("server");

            maxVeinBlocks = builder
                    .comment("Maximum number of blocks that can be broken at once with Vein Miner (Default: 128)")
                    .defineInRange("maxVeinBlocks", 128, 1, 4096);

            maxBuildArea = builder
                    .comment("Maximum size dimension for structure building (Default: 128 for 128x128)")
                    .defineInRange("maxBuildArea", 128, 1, 256);

            blocksPerTick = builder
                    .comment("Number of blocks placed per tick during tick-queue building (Default: 64)")
                    .defineInRange("blocksPerTick", 64, 1, 1024);

            builder.pop();
        }
    }

    public static class Client {
        public final ForgeConfigSpec.EnumValue<Mode> activeMode;
        public final ForgeConfigSpec.EnumValue<VeinType> veinType;
        public final ForgeConfigSpec.IntValue miningSize;
        public final ForgeConfigSpec.EnumValue<BuildShape> buildShape;
        public final ForgeConfigSpec.IntValue buildSize;

        public Client(ForgeConfigSpec.Builder builder) {
            builder.comment("Quick Build & Vein Miner Client Preferences").push("client");

            activeMode = builder
                    .comment("Active Mode: MINING or BUILDING")
                    .defineEnum("activeMode", Mode.MINING);

            veinType = builder
                    .comment("Vein Mining type: SAME_BLOCK or AREA")
                    .defineEnum("veinType", VeinType.SAME_BLOCK);

            miningSize = builder
                    .comment("Mining area size (1 to 10)")
                    .defineInRange("miningSize", 3, 1, 10);

            buildShape = builder
                    .comment("Selected Build Shape")
                    .defineEnum("buildShape", BuildShape.PLANE);

            buildSize = builder
                    .comment("Selected Build Size (1 to 128)")
                    .defineInRange("buildSize", 5, 1, 128);

            builder.pop();
        }
    }

    public enum Mode {
        MINING, BUILDING
    }

    public enum VeinType {
        SAME_BLOCK, AREA
    }

    public enum BuildShape {
        PLANE, CIRCLE, SPHERE, HOLLOW_SPHERE, CYLINDER
    }

    public static final ForgeConfigSpec SERVER_SPEC;
    public static final Server SERVER;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        final Pair<Server, ForgeConfigSpec> serverSpecPair = new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER_SPEC = serverSpecPair.getRight();
        SERVER = serverSpecPair.getLeft();

        final Pair<Client, ForgeConfigSpec> clientSpecPair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = clientSpecPair.getRight();
        CLIENT = clientSpecPair.getLeft();
    }

    // Synced Server Limits on Client Side
    public static int syncedMaxVeinBlocks = 128;
    public static int syncedMaxBuildArea = 128;
}
