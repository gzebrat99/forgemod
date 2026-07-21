package com.quickbuild;

import com.quickbuild.client.KeyBindings;
import com.quickbuild.config.QuickBuildConfig;
import com.quickbuild.network.PacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("quickbuild")
public class QuickBuildMod {
    public static final String MOD_ID = "quickbuild";

    public QuickBuildMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register Configs
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, QuickBuildConfig.SERVER_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, QuickBuildConfig.CLIENT_SPEC);

        // Lifecycle Events
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::registerKeyMappings);
        modEventBus.addListener(this::onModConfigEvent);

        // Register Forge Event Bus
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::register);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
    }

    private void registerKeyMappings(final RegisterKeyMappingsEvent event) {
        KeyBindings.register(event);
    }

    private void onModConfigEvent(final ModConfigEvent event) {
        if (event.getConfig().getType() == ModConfig.Type.SERVER) {
            // Broadcast updated config to all connected players if changed at runtime
            PacketHandler.broadcastToAll(new PacketHandler.ConfigSyncPacket(
                    QuickBuildConfig.SERVER.maxVeinBlocks.get(),
                    QuickBuildConfig.SERVER.maxBuildArea.get()
            ));
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // Sync server config to joining player
            PacketHandler.sendToPlayer(serverPlayer, new PacketHandler.ConfigSyncPacket(
                    QuickBuildConfig.SERVER.maxVeinBlocks.get(),
                    QuickBuildConfig.SERVER.maxBuildArea.get()
            ));
        }
    }
}
