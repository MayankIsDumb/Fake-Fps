package com.example.fakefps.gui;

import com.example.fakefps.FakeFPSMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class FakeFPSScreen extends Screen {
    private final Screen parent;
    private boolean enabled;
    private String mode;

    private Button toggleBtn;
    private Button staticBtn;
    private Button randomBtn;
    private EditBox fpsField;
    private EditBox minField;
    private EditBox maxField;
    private Button saveBtn;

    private final List<GuiEventListener> eventChildren = new ArrayList<>();

    public FakeFPSScreen(Screen parent) {
        super(Component.literal("Fake FPS Config"));
        this.parent = parent;
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return eventChildren;
    }

    @Override
    protected void init() {
        super.init();
        eventChildren.clear();

        var mod = FakeFPSMod.getInstance();
        var config = mod.getConfig();

        this.enabled = config.isEnabled();
        this.mode = config.getMode();

        int cx = width / 2;
        int sy = height / 2 - 70;

        // ── Toggle ──
        toggleBtn = Button.builder(
                Component.literal(enabled ? "Enabled" : "Disabled"),
                btn -> {
                    enabled = !enabled;
                    btn.setMessage(Component.literal(enabled ? "Enabled" : "Disabled"));
                })
            .bounds(cx - 100, sy, 200, 20)
            .build();
        eventChildren.add(toggleBtn);

        // ── Mode buttons ──
        staticBtn = Button.builder(
                Component.literal("Static"),
                btn -> { mode = "static"; refreshModeButtons(); })
            .bounds(cx - 100, sy + 30, 95, 20)
            .build();
        eventChildren.add(staticBtn);

        randomBtn = Button.builder(
                Component.literal("Random"),
                btn -> { mode = "random"; refreshModeButtons(); })
            .bounds(cx + 5, sy + 30, 95, 20)
            .build();
        eventChildren.add(randomBtn);

        // ── FPS value field ──
        fpsField = new EditBox(font, cx - 100, sy + 60, 200, 20,
            Component.literal("FPS Value"));
        fpsField.setValue(String.valueOf(config.getFakeFps()));
        fpsField.setMaxLength(4);
        fpsField.setResponder(s -> {
            if (!s.matches("\\d*")) fpsField.setValue(s.replaceAll("[^\\d]", ""));
        });
        eventChildren.add(fpsField);

        // ── Min / Max ──
        minField = new EditBox(font, cx - 100, sy + 90, 95, 20,
            Component.literal("Min FPS"));
        minField.setValue(String.valueOf(config.getMinFps()));
        minField.setMaxLength(4);
        minField.setResponder(s -> {
            if (!s.matches("\\d*")) minField.setValue(s.replaceAll("[^\\d]", ""));
        });
        eventChildren.add(minField);

        maxField = new EditBox(font, cx + 5, sy + 90, 95, 20,
            Component.literal("Max FPS"));
        maxField.setValue(String.valueOf(config.getMaxFps()));
        maxField.setMaxLength(4);
        maxField.setResponder(s -> {
            if (!s.matches("\\d*")) maxField.setValue(s.replaceAll("[^\\d]", ""));
        });
        eventChildren.add(maxField);

        // ── Save ──
        saveBtn = Button.builder(
                Component.literal("Save && Close"),
                btn -> saveAndClose())
            .bounds(cx - 100, sy + 130, 200, 20)
            .build();
        eventChildren.add(saveBtn);

        refreshModeButtons();
    }

    private void refreshModeButtons() {
        boolean isStatic = mode.equals("static");
        if (staticBtn != null) staticBtn.active = !isStatic;
        if (randomBtn != null) randomBtn.active = isStatic;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mx, int my, float delta) {
        int cx = width / 2;
        int sy = height / 2 - 70;

        extractor.centeredText(font, "§lFake FPS Config", cx, sy - 30, 0xFFFFFF);

        String status = enabled ? "§a● Enabled" : "§c● Disabled";
        extractor.centeredText(font, status, cx, sy + 12, 0xAAAAAA);

        extractor.text(font, "§7FPS Value:", cx - 100, sy + 48, 0xFFFFFF);
        extractor.text(font, "§7Min FPS:", cx - 100, sy + 78, 0xFFFFFF);
        extractor.text(font, "§7Max FPS:", cx + 5, sy + 78, 0xFFFFFF);

        fpsField.visible = mode.equals("static");
        minField.visible = mode.equals("random");
        maxField.visible = mode.equals("random");

        // Draw widgets
        for (var child : eventChildren) {
            if (child instanceof Renderable r) {
                r.extractRenderState(extractor, mx, my, delta);
            }
        }
    }

    private void saveAndClose() {
        var mod = FakeFPSMod.getInstance();
        var config = mod.getConfig();
        config.setEnabled(enabled);
        config.setMode(mode);

        try {
            String fps = fpsField.getValue();
            if (!fps.isEmpty()) config.setFakeFps(Integer.parseInt(fps));
            String min = minField.getValue();
            if (!min.isEmpty()) config.setMinFps(Integer.parseInt(min));
            String max = maxField.getValue();
            if (!max.isEmpty()) config.setMaxFps(Integer.parseInt(max));
        } catch (NumberFormatException ignored) {}

        config.save();
        onClose();
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
