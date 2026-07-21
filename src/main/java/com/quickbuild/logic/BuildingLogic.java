package com.quickbuild.logic;

import com.quickbuild.config.QuickBuildConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = "quickbuild", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BuildingLogic {

    // Single active queue per player to avoid thread blocking
    private static class ActiveBuildQueue {
        final List<BlockPos> remainingPositions;
        final BlockState placementState;
        final List<BlockStateRecord> placedHistory = new ArrayList<>();

        ActiveBuildQueue(List<BlockPos> positions, BlockState state) {
            this.remainingPositions = positions;
            this.placementState = state;
        }
    }

    public static class BlockStateRecord {
        public final BlockPos pos;
        public final BlockState originalState;

        public BlockStateRecord(BlockPos pos, BlockState originalState) {
            this.pos = pos;
            this.originalState = originalState;
        }
    }

    private static class LastBuildHistory {
        final List<BlockStateRecord> history = new ArrayList<>();
    }

    private static final Map<UUID, ActiveBuildQueue> activeQueues = new HashMap<>();
    private static final Map<UUID, LastBuildHistory> lastBuildHistories = new HashMap<>();

    public static void executeBuild(ServerPlayer player, BlockPos center, Direction side, QuickBuildConfig.BuildShape shape, int size) {
        int maxAllowedArea = QuickBuildConfig.SERVER.maxBuildArea.get();
        if (size > maxAllowedArea) {
            player.sendSystemMessage(Component.translatable("message.quickbuild.limit_exceeded", maxAllowedArea));
            return;
        }

        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof BlockItem blockItem)) return;

        BlockState placementState = blockItem.getBlock().defaultBlockState();
        List<BlockPos> shapePositions = ShapeGenerator.generateBuildShape(center, side, shape, size);

        // Cancel any active build queue for player
        UUID uuid = player.getUUID();
        if (activeQueues.containsKey(uuid)) {
            cancelActiveQueueAndUndoPartial(player);
        }

        // Start new queue
        ActiveBuildQueue newQueue = new ActiveBuildQueue(shapePositions, placementState);
        activeQueues.put(uuid, newQueue);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (activeQueues.isEmpty()) return;

        int blocksPerTick = QuickBuildConfig.SERVER.blocksPerTick.get();
        List<UUID> finishedPlayers = new ArrayList<>();

        for (Map.Entry<UUID, ActiveBuildQueue> entry : activeQueues.entrySet()) {
            UUID uuid = entry.getKey();
            ActiveBuildQueue queue = entry.getValue();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);

            if (player == null || !player.isAlive()) {
                finishedPlayers.add(uuid);
                continue;
            }

            ServerLevel level = player.serverLevel();
            int processedThisTick = 0;

            Iterator<BlockPos> iterator = queue.remainingPositions.iterator();
            while (iterator.hasNext() && processedThisTick < blocksPerTick) {
                BlockPos pos = iterator.next();
                iterator.remove();
                processedThisTick++;

                BlockState currentPosState = level.getBlockState(pos);
                if (!currentPosState.isAir() && currentPosState.getBlock() == queue.placementState.getBlock()) {
                    continue; // Skip if identical block already present
                }

                // Check and consume item in survival mode
                if (!player.isCreative()) {
                    if (!consumeBlockFromInventory(player, queue.placementState.getBlock())) {
                        finishedPlayers.add(uuid);
                        break;
                    }
                }

                // Save original block state for exact undo
                queue.placedHistory.add(new BlockStateRecord(pos, currentPosState));

                // Place block
                level.setBlock(pos, queue.placementState, 3);
            }

            if (queue.remainingPositions.isEmpty()) {
                finishedPlayers.add(uuid);
                // Move history to single last build history slot (stack size = 1)
                LastBuildHistory lastHistory = new LastBuildHistory();
                lastHistory.history.addAll(queue.placedHistory);
                lastBuildHistories.put(uuid, lastHistory);
            }
        }

        for (UUID uuid : finishedPlayers) {
            activeQueues.remove(uuid);
        }
    }

    public static void undoLastBuild(ServerPlayer player) {
        UUID uuid = player.getUUID();

        // If currently placing, cancel queue & undo partial placement
        if (activeQueues.containsKey(uuid)) {
            cancelActiveQueueAndUndoPartial(player);
            player.sendSystemMessage(Component.translatable("message.quickbuild.undo_cancelled"));
            return;
        }

        LastBuildHistory lastHistory = lastBuildHistories.remove(uuid);
        if (lastHistory == null || lastHistory.history.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.quickbuild.undo_none"));
            return;
        }

        ServerLevel level = player.serverLevel();
        int restoredCount = 0;

        // Restore original block states
        for (BlockStateRecord record : lastHistory.history) {
            level.setBlock(record.pos, record.originalState, 3);
            restoredCount++;
        }

        player.sendSystemMessage(Component.translatable("message.quickbuild.undo_success", restoredCount));
    }

    private static void cancelActiveQueueAndUndoPartial(ServerPlayer player) {
        UUID uuid = player.getUUID();
        ActiveBuildQueue queue = activeQueues.remove(uuid);
        if (queue != null) {
            ServerLevel level = player.serverLevel();
            for (BlockStateRecord record : queue.placedHistory) {
                level.setBlock(record.pos, record.originalState, 3);
            }
        }
    }

    private static boolean consumeBlockFromInventory(ServerPlayer player, Block block) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() instanceof BlockItem bi && bi.getBlock() == block) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }
}
