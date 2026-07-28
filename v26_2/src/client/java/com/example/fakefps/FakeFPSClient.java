package com.example.fakefps;

import com.example.fakefps.gui.FakeFPSScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class FakeFPSClient implements ClientModInitializer {
    private KeyMapping guiKey;
    private KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = new KeyMapping.Category(Identifier.fromNamespaceAndPath("fakefps", "category"));

        guiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.fakefps.gui", GLFW.GLFW_KEY_F6, category
        ));
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.fakefps.toggle", GLFW.GLFW_KEY_F7, category
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
