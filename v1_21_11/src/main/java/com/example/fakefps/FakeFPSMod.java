package com.example.fakefps;

import com.example.fakefps.config.ModConfig;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.server.command.CommandManager.*;

public class FakeFPSMod implements ModInitializer {
    public static final String MOD_ID = "fakefps";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static FakeFPSMod instance;
    private ModConfig config;

    public static FakeFPSMod getInstance() {
        return instance;
    }

    @Override
    public void onInitialize() {
        instance = this;
        config = ModConfig.load(FabricLoader.getInstance().getConfigDir());
        registerCommands();
        LOGGER.info("FakeFPS mod initialized!");
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("fakefps")
                .executes(ctx -> {
                    boolean en = config.isEnabled();
                    ctx.getSource().sendMessage(Text.literal(
                        "§6[FakeFPS] §fStatus: " + (en ? "§aEnabled" : "§cDisabled") +
                        " | Mode: §e" + config.getMode() +
                        " | Value: §e" + config.getFakeFps() + " FPS"
                    ));
                    return 1;
                })
                .then(literal("toggle")
                    .executes(ctx -> {
                        config.setEnabled(!config.isEnabled());
                        config.save();
                        boolean en = config.isEnabled();
                        ctx.getSource().sendMessage(Text.literal(
                            "§6[FakeFPS] §f" + (en ? "§aEnabled" : "§cDisabled")
                        ));
                        return 1;
                    })
                )
                .then(literal("set")
                    .then(argument("value", IntegerArgumentType.integer(1, 9999))
                        .executes(ctx -> {
                            int value = IntegerArgumentType.getInteger(ctx, "value");
                            config.setFakeFps(value);
                            config.save();
                            ctx.getSource().sendMessage(Text.literal(
                                "§6[FakeFPS] §fFake FPS set to §e" + value
                            ));
                            return 1;
                        })
                    )
                )
                .then(literal("mode")
                    .then(argument("mode", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("static");
                            builder.suggest("random");
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String mode = StringArgumentType.getString(ctx, "mode");
                            if (!mode.equals("static") && !mode.equals("random")) {
                                ctx.getSource().sendMessage(Text.literal(
                                    "§cMode must be 'static' or 'random'"
                                ));
                                return 0;
                            }
                            config.setMode(mode);
                            config.save();
                            ctx.getSource().sendMessage(Text.literal(
                                "§6[FakeFPS] §fMode set to §e" + mode
                            ));
                            return 1;
                        })
                    )
                )
                .then(literal("random")
                    .then(argument("min", IntegerArgumentType.integer(1, 9999))
                        .then(argument("max", IntegerArgumentType.integer(1, 9999))
                            .executes(ctx -> {
                                int min = IntegerArgumentType.getInteger(ctx, "min");
                                int max = IntegerArgumentType.getInteger(ctx, "max");
                                if (min > max) {
                                    ctx.getSource().sendMessage(Text.literal(
                                        "§cMin must be <= max"
                                    ));
                                    return 0;
                                }
                                config.setMode("random");
                                config.setMinFps(min);
                                config.setMaxFps(max);
                                config.save();
                                ctx.getSource().sendMessage(Text.literal(
                                    "§6[FakeFPS] §fRandom range set to §e" + min + "-" + max + " FPS"
                                ));
                                return 1;
                            })
                        )
                    )
                )
            );
        });
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    public int getCurrentFps() {
        return config.getCurrentFps();
    }

    public ModConfig getConfig() {
        return config;
    }
}
