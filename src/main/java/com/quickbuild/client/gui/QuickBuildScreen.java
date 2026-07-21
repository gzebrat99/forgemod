package com.quickbuild.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.quickbuild.config.QuickBuildConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class QuickBuildScreen extends Screen {

    private QuickBuildConfig.Mode currentMode;
    private QuickBuildConfig.VeinType currentVeinType;
    private int currentMiningSize;
    private QuickBuildConfig.BuildShape currentBuildShape;
    private int currentBuildSize;

    public QuickBuildScreen() {
        super(Component.translatable("gui.quickbuild.title"));
        this.currentMode = QuickBuildConfig.CLIENT.activeMode.get();
        this.currentVeinType = QuickBuildConfig.CLIENT.veinType.get();
        this.currentMiningSize = QuickBuildConfig.CLIENT.miningSize.get();
        this.currentBuildShape = QuickBuildConfig.CLIENT.buildShape.get();
        this.currentBuildSize = QuickBuildConfig.CLIENT.buildSize.get();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 40;

        // Mode Toggle Button
        this.addRenderableWidget(Button.builder(
                Component.literal("Mode: " + currentMode.name()),
                btn -> {
                    currentMode = (currentMode == QuickBuildConfig.Mode.MINING) ? QuickBuildConfig.Mode.BUILDING : QuickBuildConfig.Mode.MINING;
                    btn.setMessage(Component.literal("Mode: " + currentMode.name()));
                }
        ).bounds(centerX - 100, startY, 200, 20).build());

        // Vein Mine Type Button
        this.addRenderableWidget(Button.builder(
                Component.literal("Mining Type: " + currentVeinType.name()),
                btn -> {
                    currentVeinType = (currentVeinType == QuickBuildConfig.VeinType.SAME_BLOCK) ? QuickBuildConfig.VeinType.AREA : QuickBuildConfig.VeinType.SAME_BLOCK;
                    btn.setMessage(Component.literal("Mining Type: " + currentVeinType.name()));
                }
        ).bounds(centerX - 100, startY + 30, 200, 20).build());

        // Mining Size Button (- / +)
        this.addRenderableWidget(Button.builder(
                Component.literal("Mining Size: " + currentMiningSize + "x" + currentMiningSize),
                btn -> {
                    currentMiningSize = (currentMiningSize % 10) + 1;
                    btn.setMessage(Component.literal("Mining Size: " + currentMiningSize + "x" + currentMiningSize));
                }
        ).bounds(centerX - 100, startY + 60, 200, 20).build());

        // Build Shape Button
        this.addRenderableWidget(Button.builder(
                Component.literal("Build Shape: " + currentBuildShape.name()),
                btn -> {
                    QuickBuildConfig.BuildShape[] shapes = QuickBuildConfig.BuildShape.values();
                    currentBuildShape = shapes[(currentBuildShape.ordinal() + 1) % shapes.length];
                    btn.setMessage(Component.literal("Build Shape: " + currentBuildShape.name()));
                }
        ).bounds(centerX - 100, startY + 90, 200, 20).build());

        // Build Size Button
        int maxAllowed = QuickBuildConfig.syncedMaxBuildArea;
        this.addRenderableWidget(Button.builder(
                Component.literal("Build Size: " + currentBuildSize + " (Max: " + maxAllowed + ")"),
                btn -> {
                    currentBuildSize += 5;
                    if (currentBuildSize > maxAllowed) currentBuildSize = 1;
                    btn.setMessage(Component.literal("Build Size: " + currentBuildSize + " (Max: " + maxAllowed + ")"));
                }
        ).bounds(centerX - 100, startY + 120, 200, 20).build());

        // Save & Close Button
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.quickbuild.save"),
                btn -> {
                    saveAndClose();
                }
        ).bounds(centerX - 100, startY + 160, 200, 20).build());
    }

    private void saveAndClose() {
        QuickBuildConfig.CLIENT.activeMode.set(currentMode);
        QuickBuildConfig.CLIENT.veinType.set(currentVeinType);
        QuickBuildConfig.CLIENT.miningSize.set(currentMiningSize);
        QuickBuildConfig.CLIENT.buildShape.set(currentBuildShape);
        QuickBuildConfig.CLIENT.buildSize.set(currentBuildSize);
        QuickBuildConfig.CLIENT_SPEC.save();
        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
