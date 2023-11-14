package com.piggyplugins;

import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.PacketUtils.PacketUtilsPlugin;
import com.piggyplugins.AutoAerial.AutoAerialPlugin;
import com.piggyplugins.Firemaking.FiremakingPlugin;
import com.piggyplugins.PowerSkiller.PowerSkillerPlugin;
import com.polyplugins.AutoBoner.AutoBonerPlugin;
import com.polyplugins.AutoCombat.AutoCombatPlugin;
import com.polyplugins.Butterfly.ButterflyPlugin;
import com.polyplugins.Chompy.AutoChompyPlugin;
import com.polyplugins.Dialogue.DialogueContinuerPlugin;
import com.polyplugins.KittenFeeder.KittenFeederPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import com.santaplugins.AutoLoot.AutoLootPlugin;

public class PluginTester{
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(EthanApiPlugin.class, PacketUtilsPlugin.class,
                AutoCombatPlugin.class, AutoAerialPlugin.class, AutoBonerPlugin.class,
                ButterflyPlugin.class, FiremakingPlugin.class, DialogueContinuerPlugin.class,
                KittenFeederPlugin.class, AutoChompyPlugin.class, PowerSkillerPlugin.class, AutoLootPlugin.class);
        RuneLite.main(args);
    }
}