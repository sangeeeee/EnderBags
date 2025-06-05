package com.sange.ender_bags;

import com.sange.ender_bags.container.EnderBagMenu;
import com.sange.ender_bags.item.ModItems;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EnderBags.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEvents {
    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        Player player = event.getPlayer();
        if (player.containerMenu instanceof EnderBagMenu menu) {
            ItemStack stack = event.getEntity().getItem();
            if (stack.is(ModItems.ENDER_BAG.get()) && player.getMainHandItem().equals(stack)) {
                menu.removed(player); // Save NBT before toss
                player.closeContainer();
                System.out.println("Saved EnderBag NBT on toss for player " + player.getName().getString());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player.containerMenu instanceof EnderBagMenu menu) {
            menu.removed(player);
            player.closeContainer();
            System.out.println("Saved EnderBag NBT on logout for player " + player.getName().getString());
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (player.containerMenu instanceof EnderBagMenu menu) {
            menu.removed(player);
            player.closeContainer();
            System.out.println("Saved EnderBag NBT on respawn for player " + player.getName().getString());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MinecraftServer server = event.getServer();
            for (Player player : server.getPlayerList().getPlayers()) {
                if (player.containerMenu instanceof EnderBagMenu menu && !menu.stillValid(player)) {
                    menu.removed(player);
                    player.closeContainer();
                    System.out.println("Saved EnderBag NBT on invalid menu state for player " + player.getName().getString());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        MinecraftServer server = event.getServer();
        for (Player player : server.getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof EnderBagMenu menu) {
                menu.removed(player);
                player.closeContainer();
                System.out.println("Saved EnderBag NBT on server stop for player " + player.getName().getString());
            }
        }
    }
}