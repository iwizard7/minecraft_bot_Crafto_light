package com.crafto.craftolight;

import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod(CraftoLightMod.MODID)
public class CraftoLightMod
{
    public static final String MODID = "craftolight";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, BotEntity> playerBots = new HashMap<>();

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

    public static final RegistryObject<EntityType<BotEntity>> BOT_ENTITY = ENTITY_TYPES.register("bot_entity", () ->
        EntityType.Builder.of(BotEntity::new, MobCategory.MISC)
            .sized(0.6F, 1.8F)
            .build("bot_entity")
    );

    public CraftoLightMod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerAttributes);

        ENTITY_TYPES.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("Crafto Light Bot mod loaded");
    }

    private void registerAttributes(final EntityAttributeCreationEvent event)
    {
        event.put(BOT_ENTITY.get(), BotEntity.createAttributes().build());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("Crafto Light Bot server starting");
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event)
    {
        event.getDispatcher().register(Commands.literal("bot")
            .then(Commands.literal("spawn").executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                BotEntity bot = new BotEntity(CraftoLightMod.BOT_ENTITY.get(), player.level());
                bot.setPos(player.getX(), player.getY() + 1, player.getZ());
                bot.setOwner(player);
                playerBots.put(player.getUUID(), bot);
                player.level().addFreshEntity(bot);
                context.getSource().sendSuccess(() -> Component.literal("Bot spawned next to you!"), false);
                return 1;
            }))
            .then(Commands.literal("mine")
                .then(Commands.literal("diamond").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    BotEntity bot = playerBots.get(player.getUUID());
                    if (bot != null && !bot.isRemoved()) {
                        bot.setTask(BotEntity.Task.MINE_DIAMOND, 10);
                        return 1;
                    }
                    return 0;
                }))
            )
            .then(Commands.literal("build")
                .then(Commands.literal("house").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    BotEntity bot = playerBots.get(player.getUUID());
                    if (bot != null && !bot.isRemoved()) {
                        bot.setTask(BotEntity.Task.BUILD_HOUSE, 1);
                        return 1;
                    }
                    return 0;
                }))
            )
            .then(Commands.literal("followme").executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                BotEntity bot = playerBots.get(player.getUUID());
                if (bot != null && !bot.isRemoved()) {
                    bot.setTask(BotEntity.Task.FOLLOW_PLAYER, 1);
                    return 1;
                }
                return 0;
            }))
        );
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent event)
    {
        String message = event.getMessage().getString().toLowerCase();
        BotEntity bot = playerBots.get(event.getPlayer().getUUID());
        if (bot != null && !bot.isRemoved()) {
            if (message.contains("найди 10 алмазов") || message.contains("добыть алмазы")) {
                bot.setTask(BotEntity.Task.MINE_DIAMOND, 10);
                event.getPlayer().sendSystemMessage(Component.literal("Bot will mine stone!"));
            } else if (message.contains("построй дом") || message.contains("строить дом")) {
                bot.setTask(BotEntity.Task.BUILD_HOUSE, 1);
                event.getPlayer().sendSystemMessage(Component.literal("Bot will build a house!"));
            }
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            EntityRenderers.register(BOT_ENTITY.get(), BotRenderer::new);
        }
    }
}
