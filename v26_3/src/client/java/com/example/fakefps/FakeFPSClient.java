package com.example.fakefps;

import com.example.fakefps.gui.FakeFPSScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class FakeFPSClient implements ClientModInitializer {
    private KeyMapping guiKey;
    private KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = new KeyMapping.Category(Identifier.fromNamespaceAndPath("fakefps", "category"));

        // 26.3: GLFW removed in favour of SDL — use InputConstants instead of
        // org.lwjgl.glfw.GLFW constants (see Fabric blog "Fabric for Minecraft 26.3").
        // 26.3 also merged InputConstants.Type KEYSYM/SCANCODE into KEYBOARD (verified via javap).
        guiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.fakefps.gui", InputConstants.Type.KEYBOARD, InputConstants.KEY_F6, category
        ));
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.fakefps.toggle", InputConstants.Type.KEYBOARD, InputConstants.KEY_F7, category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            FakeFPSMod mod = FakeFPSMod.getInstance();
            if (mod == null) return;

            while (guiKey.consumeClick()) {
                client.setScreenAndShow(new FakeFPSScreen(null));
            }
            while (toggleKey.consumeClick()) {
                mod.getConfig().setEnabled(!mod.getConfig().isEnabled());
                mod.getConfig().save();
                if (client.player != null) {
                    boolean en = mod.getConfig().isEnabled();
                    client.player.sendOverlayMessage(Component.literal(
                        "§6[FakeFPS] §f" + (en ? "§aEnabled" : "§cDisabled")
                    ));
                }
            }
        });
    }
}
