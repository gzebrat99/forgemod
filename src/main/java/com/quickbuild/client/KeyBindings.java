package com.quickbuild.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.quickbuild.client.gui.QuickBuildScreen;
import com.quickbuild.config.QuickBuildConfig;
import com.quickbuild.network.PacketHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "quickbuild", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KeyBindings {
    public static final String KEY_CATEGORY = "key.categories.quickbuild";

    public static final KeyMapping VEIN_MINE_KEY = new KeyMapping(
            "key.quickbuild.vein_mine",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            KEY_CATEGORY
    );

    public static final KeyMapping OPEN_GUI_KEY = new KeyMapping(
            "key.quickbuild.open_gui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            KEY_CATEGORY
    );

    public static final KeyMapping TOGGLE_MODE_KEY = new KeyMapping(
            "key.quickbuild.toggle_mode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KEY_CATEGORY
    );

    public static final KeyMapping UNDO_KEY = new KeyMapping(
            "key.quickbuild.undo",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            KEY_CATEGORY
    );

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(VEIN_MINE_KEY);
        event.register(OPEN_GUI_KEY);
        event.register(TOGGLE_MODE_KEY);
        event.register(UNDO_KEY);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        if (OPEN_GUI_KEY.consumeClick()) {
            mc.setScreen(new QuickBuildScreen());
        }

        if (TOGGLE_MODE_KEY.consumeClick()) {
            QuickBuildConfig.Mode current = QuickBuildConfig.CLIENT.activeMode.get();
            QuickBuildConfig.Mode next = (current == QuickBuildConfig.Mode.MINING) ? QuickBuildConfig.Mode.BUILDING : QuickBuildConfig.Mode.MINING;
            QuickBuildConfig.CLIENT.activeMode.set(next);
            QuickBuildConfig.CLIENT_SPEC.save();
        }

        if (UNDO_KEY.consumeClick()) {
            PacketHandler.sendToServer(new PacketHandler.UndoRequestPacket());
        }
    }
}
