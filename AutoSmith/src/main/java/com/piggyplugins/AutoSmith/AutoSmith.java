package com.piggyplugins.AutoSmith;

import com.example.EthanApiPlugin.Collections.*;
import com.example.EthanApiPlugin.Collections.query.ItemQuery;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.NPCInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.PacketUtils.PacketUtilsPlugin;
import com.example.PacketUtils.WidgetInfoExtended;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.piggyplugins.PiggyUtils.API.InventoryUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.HotkeyListener;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

@PluginDescriptor(name = "<html><font color=\"#7ecbf2\">[PJ]</font>AutoSmith</html>",
        description = "",
        enabledByDefault = false,
        tags = {"poly", "plugin"})
@Slf4j
@PluginDependency(PacketUtilsPlugin.class)
@PluginDependency(EthanApiPlugin.class)
public class AutoSmith extends Plugin {
    public int timeout = 0;
    public int idleTicks = 0;
    public boolean started = false;
    @Inject
    Client client;
    @Inject
    AutoSmithConfig config;
    @Inject
    private KeyManager keyManager;

    public boolean isSmithing;

    @Override
    @SneakyThrows
    public void startUp() {
        timeout = 0;
        isSmithing = false;
        keyManager.registerKeyListener(toggle);
        log.info(config.bar().toString() + " - " + config.item().toString());
    }

    @Override
    public void shutDown() {
        isSmithing = false;
        timeout = 0;
        started = false;
        keyManager.unregisterKeyListener(toggle);
    }

    @Provides
    public AutoSmithConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(AutoSmithConfig.class);
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!started) return;

        if (client.getLocalPlayer().getAnimation() == -1) {
            idleTicks++;
        } else {
            idleTicks = 0;
        }

        if (timeout > 0) {
            timeout--;
            if (idleTicks > 10 || !hasEnoughBars()) {
                timeout = 0;
                idleTicks = 0;
            }
            return;
        }


        if (isSmithing) {
            if (!hasEnoughBars()) {
                isSmithing = false;
            }
            return;
        }

        checkRunEnergy();
        Optional<TileObject> anvil = TileObjects.search().withName("Anvil").nearestToPlayer();
        if (hasEnoughBars() && InventoryUtil.hasItem("Hammer")) {
            if (client.getWidget(WidgetInfo.SMITHING_INVENTORY_ITEMS_CONTAINER) != null) {
                MousePackets.queueClickPacket();
                WidgetPackets.queueWidgetAction(client.getWidget(config.item().getWidgetInfo().getPackedId()), "Smith", "Smith set");
//                this.timeout = 26;
                timeout = (int) (5 * Math.ceil(27 / config.item().getBarsRequired()));
            } else if (anvil.isPresent()) {
                boolean action = TileObjectInteraction.interact(anvil.get(), "Smith");
                if (!action)
                    log.info("failed anvil interaction");
                timeout = 3;
            }
        }

        if (!hasEnoughBars() || !InventoryUtil.hasItem("Hammer")) {
            findBank();
            bankHandler();
        }

    }

    private boolean hasEnoughBars() {
        return (Inventory.getItemAmount(config.bar().toString()) >= config.item().getBarsRequired());
    }

    private boolean canSmithBars() {
        return Inventory.search().withName(config.bar().toString()).result().size() > 5 && !Inventory.search().withName("Hammer").empty();
    }

    private void findBank() {
        Optional<NPC> banker = NPCs.search().withAction("Bank").withId(2897).nearestToPlayer();
        Optional<TileObject> bank = TileObjects.search().withAction("Bank").nearestToPlayer();
        if (!Bank.isOpen()) {
            if (banker.isPresent()) {
                NPCInteraction.interact(banker.get(), "Bank");
                timeout = 3;
            } else if (bank.isPresent()) {
                TileObjectInteraction.interact(bank.get(), "Bank");
                timeout = 3;
            } else {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Couldn't find bank or banker", null);
                EthanApiPlugin.stopPlugin(this);
            }
        }
    }

    private void bankHandler() {
        if (!Bank.isOpen()) return;
        ItemQuery barQuery = Bank.search().withName(config.bar().toString());
        ItemQuery hammerQuery = Bank.search().withName("Hammer");

        Widget widget = client.getWidget(WidgetInfo.BANK_DEPOSIT_INVENTORY);

        depositAllButHammerBars();

        Optional<Widget> bar = barQuery.first();
        Optional<Widget> hammer = hammerQuery.first();
        if (!InventoryUtil.hasItem("Hammer")) {
//            log.info("no hammer,withdraw");
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(hammer.get(), "Withdraw-1");
        }
        if (!Inventory.full()) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(bar.get(), "Withdraw-All");
        }
        timeout = 2;
    }

    private void depositAllButHammerBars() {
        BankInventory.search().filter(
                item -> !item.getName().contains("Hammer") && !item.getName().contains(config.bar().toString())
        ).result().forEach(item -> {
//            log.info("depositing " + item.getName());
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(item, "Deposit-All");
        });
    }

    private boolean runIsOff() {
        return EthanApiPlugin.getClient().getVarpValue(173) == 0;
    }

    private void checkRunEnergy() {
        if (runIsOff() && client.getEnergy() >= 10 * 100) {
            log.info("turning run on");
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetActionPacket(1, 10485787, -1, -1);
        }
    }

    private final HotkeyListener toggle = new HotkeyListener(() -> config.toggle()) {
        @Override
        public void hotkeyPressed() {
            toggle();
        }
    };

    public void toggle() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        started = !started;
    }
}

