package com.quickbuild.logic;

import com.quickbuild.config.QuickBuildConfig;
import com.quickbuild.network.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class MiningLogic {

    public static void executeMining(ServerPlayer player, BlockPos startPos, Direction side, boolean isVeinSameBlock, int requestedSize) {
        ServerLevel level = player.serverLevel();
        BlockState startState = level.getBlockState(startPos);
        if (startState.isAir()) return;

        // Reach distance check
        if (player.distanceToSqr(startPos.getX() + 0.5, startPos.getY() + 0.5, startPos.getZ() + 0.5) > 64.0) return;

        int maxLimit = QuickBuildConfig.SERVER.maxVeinBlocks.get();
        List<BlockPos> targetsToMine = new ArrayList<>();

        if (isVeinSameBlock) {
            // BFS Flood-fill for matching block type up to maxLimit
            Block targetBlock = startState.getBlock();
            Queue<BlockPos> queue = new LinkedList<>();
            Set<BlockPos> visited = new HashSet<>();

            queue.add(startPos);
            visited.add(startPos);

            while (!queue.isEmpty() && targetsToMine.size() < maxLimit) {
                BlockPos current = queue.poll();
                targetsToMine.add(current);

                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = current.relative(dir);
                    if (!visited.contains(neighbor) && level.getBlockState(neighbor).is(targetBlock)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        } else {
            // Area Mining shape
            int safeSize = Math.min(requestedSize, 10);
            List<BlockPos> shapePositions = ShapeGenerator.generateMiningArea(startPos, side, safeSize);
            for (BlockPos pos : shapePositions) {
                if (targetsToMine.size() >= maxLimit) break;
                BlockState state = level.getBlockState(pos);
                if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0) {
                    targetsToMine.add(pos);
                }
            }
        }

        // Mine blocks sequentially and enforce durability check
        ItemStack tool = player.getMainHandItem();

        for (BlockPos pos : targetsToMine) {
            // Check tool durability
            if (!tool.isEmpty() && tool.isDamageableItem()) {
                if (tool.getDamageValue() >= tool.getMaxDamage() - 1) {
                    player.sendSystemMessage(Component.translatable("message.quickbuild.tool_broken"));
                    break;
                }
            }

            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;

            // Harvest block using standard drop resources logic (drops fall to ground, respects fortune/silk touch)
            boolean destroyed = level.destroyBlock(pos, true, player);
            if (destroyed && !player.isCreative() && !tool.isEmpty() && tool.isDamageableItem()) {
                tool.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));
            }
        }
    }
}
