package com.example.fakefps.gui;

import com.example.fakefps.FakeFPSMod;
import com.example.fakefps.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class FakeFPSScreen extends Screen {
    private final Screen parent;
    private ModConfig config;

    // Widgets
    private ButtonWidget toggleBtn;
    private ButtonWidget staticBtn;
    private ButtonWidget randomBtn;
    private TextFieldWidget fpsField;
    private TextFieldWidget minField;
    private TextFieldWidget maxField;

    // Working state (editing a copy so cancel just closes)
    private boolean enabled;
    private String mode;

    public FakeFPSScreen(Screen parent) {
        super(Text.literal("Fake FPS Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        FakeFPSMod mod = FakeFPSMod.getInstance();
        this.config = mod.getConfig();
        this.enabled = config.isEnabled();
        this.mode = config.getMode();

        int cx = width / 2;
        int sy = height / 2 - 70;

        // ── Toggle ON/OFF ──
        toggleBtn = addDrawableChild(ButtonWidget.builder(
                Text.literal(enabled ? "Enabled" : "Disabled"),
                btn -> {
                    enabled = !enabled;
                    btn.setMessage(Text.literal(enabled ? "Enabled" : "Disabled"));
                })
            .position(cx - 100, sy)
            .size(200, 20)
            .build());
        toggleBtn.active = true;

        // ── Mode: Static / Random ──
        staticBtn = addDrawableChild(ButtonWidget.builder(
                Text.literal("Static"),
                btn -> { mode = "static"; refreshModeButtons(); })
            .position(cx - 100, sy + 30)
            .size(95, 20)
            .build());

        randomBtn = addDrawableChild(ButtonWidget.builder(
                Text.literal("Random"),
                btn -> { mode = "random"; refreshModeButtons(); })
            .position(cx + 5, sy + 30)
            .size(95, 20)
            .build());

        // ── FPS Value field ──
        fpsField = addDrawableChild(new TextFieldWidget(
            textRenderer, cx - 100, sy + 60, 200, 20,
            Text.literal("FPS Value")));
        fpsField.setText(String.valueOf(config.getFakeFps()));
        fpsField.setTextPredicate(s -> s.matches("\\d*") && s.length() <= 4);

        // ── Min / Max fields ──
        minField = addDrawableChild(new TextFieldWidget(
            textRenderer, cx - 100, sy + 90, 95, 20,
            Text.literal("Min FPS")));
        minField.setText(String.valueOf(config.getMinFps()));
        minField.setTextPredicate(s -> s.matches("\\d*") && s.length() <= 4);

        maxField = addDrawableChild(new TextFieldWidget(
            textRenderer, cx + 5, sy + 90, 95, 20,
            Text.literal("Max FPS")));
        maxField.setText(String.valueOf(config.getMaxFps()));
        maxField.setTextPredicate(s -> s.matches("\\d*") && s.length() <= 4);

        // ── Save ──
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Save && Close"),
                btn -> saveAndClose())
            .position(cx - 100, sy + 130)
            .size(200, 20)
            .build());

        refreshModeButtons();
    }

    /** Visually dim the inactive mode button. */
    private void refreshModeButtons() {
        boolean isStatic = mode.equals("static");
        staticBtn.active = !isStatic;
        randomBtn.active = isStatic;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);

        int cx = width / 2;
        int sy = height / 2 - 70;

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer, "§lFake FPS Config", cx, sy - 30, 0xFFFFFF);

        // Enable/disable indicator
        String status = enabled ? "§a● Enabled" : "§c● Disabled";
        ctx.drawCenteredTextWithShadow(textRenderer, status, cx, sy + 12, 0xAAAAAA);

        // Field labels
        ctx.drawTextWithShadow(textRenderer, Text.literal("§7FPS Value:"), cx - 100, sy + 48, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§7Min FPS:"), cx - 100, sy + 78, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§7Max FPS:"), cx + 5, sy + 78, 0xFFFFFF);

        // Toggle field visibility per mode
        fpsField.visible = mode.equals("static");
        minField.visible = mode.equals("random");
        maxField.visible = mode.equals("random");
    }

    private void saveAndClose() {
        config.setEnabled(enabled);
        config.setMode(mode);

        try {
            String fps = fpsField.getText();
            if (!fps.isEmpty()) config.setFakeFps(Integer.parseInt(fps));
            String min = minField.getText();
            if (!min.isEmpty()) config.setMinFps(Integer.parseInt(min));
            String max = maxField.getText();
            if (!max.isEmpty()) config.setMaxFps(Integer.parseInt(max));
        } catch (NumberFormatException ignored) {}

        config.save();
        close();
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }
}
