package com.example.fakefps;

import com.example.fakefps.gui.FakeFPSScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class FakeFPSClient implements ClientModInitializer {
    private static KeyBinding guiKey;
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        KeyBinding.Category category = new KeyBinding.Category(Identifier.of("fakefps", "category"));

        // F6 = open GUI
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.fakefps.gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F6,
            category
        ));

        // F7 = quick toggle on/off
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.fakefps.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F7,
            category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (guiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new FakeFPSScreen(null));
                }
            }
            if (toggleKey.wasPressed()) {
                FakeFPSMod mod = FakeFPSMod.getInstance();
                if (mod != null) {
                    boolean newState = !mod.isEnabled();
                    mod.getConfig().setEnabled(newState);
                    mod.getConfig().save();
                    if (client.player != null) {
                        client.player.sendMessage(net.minecraft.text.Text.literal(
                            "§6[FakeFPS] §f" + (newState ? "§aEnabled" : "§cDisabled")
                        ), true);
                    }
                }
            }
        });

        FakeFPSMod.LOGGER.info("FakeFPS client initialized!");
    }
}
