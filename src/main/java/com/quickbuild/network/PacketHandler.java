package com.quickbuild.network;

import com.quickbuild.config.QuickBuildConfig;
import com.quickbuild.logic.BuildingLogic;
import com.quickbuild.logic.MiningLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("quickbuild", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        registerMessage(MiningRequestPacket.class, MiningRequestPacket::encode, MiningRequestPacket::decode, MiningRequestPacket::handle);
        registerMessage(BuildRequestPacket.class, BuildRequestPacket::encode, BuildRequestPacket::decode, BuildRequestPacket::handle);
        registerMessage(UndoRequestPacket.class, UndoRequestPacket::encode, UndoRequestPacket::decode, UndoRequestPacket::handle);
        registerMessage(ConfigSyncPacket.class, ConfigSyncPacket::encode, ConfigSyncPacket::decode, ConfigSyncPacket::handle);
    }

    private static <MSG> void registerMessage(Class<MSG> messageType, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG, Supplier<NetworkEvent.Context>> messageConsumer) {
        INSTANCE.registerMessage(packetId++, messageType, encoder, decoder, messageConsumer);
    }

    // Packet 1: Mining Request
    public static class MiningRequestPacket {
        private final BlockPos targetPos;
        private final Direction side;
        private final boolean isVeinSameBlock;
        private final int size;

        public MiningRequestPacket(BlockPos targetPos, Direction side, boolean isVeinSameBlock, int size) {
            this.targetPos = targetPos;
            this.side = side;
            this.isVeinSameBlock = isVeinSameBlock;
            this.size = size;
        }

        public static void encode(MiningRequestPacket msg, FriendlyByteBuf buf) {
            buf.writeBlockPos(msg.targetPos);
            buf.writeEnum(msg.side);
            buf.writeBoolean(msg.isVeinSameBlock);
            buf.writeInt(msg.size);
        }

        public static MiningRequestPacket decode(FriendlyByteBuf buf) {
            return new MiningRequestPacket(buf.readBlockPos(), buf.readEnum(Direction.class), buf.readBoolean(), buf.readInt());
        }

        public static void handle(MiningRequestPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player != null) {
                    MiningLogic.executeMining(player, msg.targetPos, msg.side, msg.isVeinSameBlock, msg.size);
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    // Packet 2: Build Request (Parametric, lightweight)
    public static class BuildRequestPacket {
        private final BlockPos targetPos;
        private final Direction side;
        private final QuickBuildConfig.BuildShape shape;
        private final int size;

        public BuildRequestPacket(BlockPos targetPos, Direction side, QuickBuildConfig.BuildShape shape, int size) {
            this.targetPos = targetPos;
            this.side = side;
            this.shape = shape;
            this.size = size;
        }

        public static void encode(BuildRequestPacket msg, FriendlyByteBuf buf) {
            buf.writeBlockPos(msg.targetPos);
            buf.writeEnum(msg.side);
            buf.writeEnum(msg.shape);
            buf.writeInt(msg.size);
        }

        public static BuildRequestPacket decode(FriendlyByteBuf buf) {
            return new BuildRequestPacket(buf.readBlockPos(), buf.readEnum(Direction.class), buf.readEnum(QuickBuildConfig.BuildShape.class), buf.readInt());
        }

        public static void handle(BuildRequestPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player != null) {
                    BuildingLogic.executeBuild(player, msg.targetPos, msg.side, msg.shape, msg.size);
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    // Packet 3: Undo Request
    public static class UndoRequestPacket {
        public UndoRequestPacket() {}

        public static void encode(UndoRequestPacket msg, FriendlyByteBuf buf) {}

        public static void decode(FriendlyByteBuf buf) {
            return new UndoRequestPacket();
        }

        public static void handle(UndoRequestPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player != null) {
                    BuildingLogic.undoLastBuild(player);
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    // Packet 4: Config Sync (Server to Client)
    public static class ConfigSyncPacket {
        private final int maxVeinBlocks;
        private final int maxBuildArea;

        public ConfigSyncPacket(int maxVeinBlocks, int maxBuildArea) {
            this.maxVeinBlocks = maxVeinBlocks;
            this.maxBuildArea = maxBuildArea;
        }

        public static void encode(ConfigSyncPacket msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.maxVeinBlocks);
            buf.writeInt(msg.maxBuildArea);
        }

        public static ConfigSyncPacket decode(FriendlyByteBuf buf) {
            return new ConfigSyncPacket(buf.readInt(), buf.readInt());
        }

        public static void handle(ConfigSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                QuickBuildConfig.syncedMaxVeinBlocks = msg.maxVeinBlocks;
                QuickBuildConfig.syncedMaxBuildArea = msg.maxBuildArea;
            });
            ctx.setPacketHandled(true);
        }
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }

    public static void broadcastToAll(Object packet) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }
}
