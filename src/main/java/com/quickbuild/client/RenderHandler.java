package com.quickbuild.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quickbuild.config.QuickBuildConfig;
import com.quickbuild.logic.ShapeGenerator;
import com.quickbuild.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@Mod.EventBusSubscriber(modid = "quickbuild", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RenderHandler {

    private static boolean wasGKeyDown = false;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        HitResult hitResult = mc.hitResult;
        if (!(hitResult instanceof BlockHitResult blockHitResult) || blockHitResult.getType() == HitResult.Type.MISS) return;

        BlockPos targetPos = blockHitResult.getBlockPos();
        Direction side = blockHitResult.getDirection();

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());

        boolean isGDown = KeyBindings.VEIN_MINE_KEY.isDown();
        QuickBuildConfig.Mode mode = QuickBuildConfig.CLIENT.activeMode.get();

        if (isGDown || mode == QuickBuildConfig.Mode.MINING) {
            // Render mining target bounding boxes (Red Wireframe)
            boolean sameBlock = QuickBuildConfig.CLIENT.veinType.get() == QuickBuildConfig.VeinType.SAME_BLOCK;
            int size = QuickBuildConfig.CLIENT.miningSize.get();

            List<BlockPos> positions;
            if (sameBlock && isGDown) {
                positions = ShapeGenerator.generateMiningArea(targetPos, side, size);
            } else {
                positions = ShapeGenerator.generateMiningArea(targetPos, side, size);
            }

            for (BlockPos pos : positions) {
                LevelRenderer.renderLineBox(poseStack, consumer, pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, 1.0f, 0.2f, 0.2f, 0.8f);
            }
        } else if (mode == QuickBuildConfig.Mode.BUILDING) {
            // Render building preview boxes (Green Wireframe)
            QuickBuildConfig.BuildShape shape = QuickBuildConfig.CLIENT.buildShape.get();
            int size = QuickBuildConfig.CLIENT.buildSize.get();
            BlockPos placeCenter = targetPos.relative(side);

            List<BlockPos> positions = ShapeGenerator.generateBuildShape(placeCenter, side, shape, size);
            for (BlockPos pos : positions) {
                LevelRenderer.renderLineBox(poseStack, consumer, pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, 0.2f, 1.0f, 0.2f, 0.8f);
            }
        }

        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onMouseClick(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.hitResult == null || !(mc.hitResult instanceof BlockHitResult blockHitResult)) return;

        if (event.isUseItem()) {
            // Right-click action in Building Mode triggers placement
            QuickBuildConfig.Mode mode = QuickBuildConfig.CLIENT.activeMode.get();
            if (mode == QuickBuildConfig.Mode.BUILDING && !mc.player.getMainHandItem().isEmpty()) {
                BlockPos targetPos = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
                Direction side = blockHitResult.getDirection();
                QuickBuildConfig.BuildShape shape = QuickBuildConfig.CLIENT.buildShape.get();
                int size = QuickBuildConfig.CLIENT.buildSize.get();

                PacketHandler.sendToServer(new PacketHandler.BuildRequestPacket(targetPos, side, shape, size));
                event.setCanceled(true);
            }
        } else if (event.isAttack()) {
            // Left-click action with G held triggers Vein Mine
            if (KeyBindings.VEIN_MINE_KEY.isDown()) {
                BlockPos targetPos = blockHitResult.getBlockPos();
                Direction side = blockHitResult.getDirection();
                boolean sameBlock = QuickBuildConfig.CLIENT.veinType.get() == QuickBuildConfig.VeinType.SAME_BLOCK;
                int size = QuickBuildConfig.CLIENT.miningSize.get();

                PacketHandler.sendToServer(new PacketHandler.MiningRequestPacket(targetPos, side, sameBlock, size));
                event.setCanceled(true);
            }
        }
    }
}
