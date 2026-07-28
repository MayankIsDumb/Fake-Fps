package com.example.fakefps.agent;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class FPSService {

    private static int mode;           // 0 = static, 1 = random
    private static int fakeFps = 60;
    private static int minFps = 30;
    private static int maxFps = 300;
    private static String configuredMethod;

    private static volatile boolean enabled = true;

    private static final Random random = new Random();
    private static int cachedRandomFps = 60;
    private static long lastRandomUpdate;

    // Toggle key state
    private static boolean f7WasDown;
    private static boolean f6WasDown;
    private static long lastToggleCheck;

    // GLFW reflection cache
    private static Class<?> glfwClass;
    private static Method glfwGetCurrentContext;
    private static Method glfwGetKey;
    private static int glfwKeyF7;
    private static int glfwKeyF6;
    private static boolean glfwResolved;

    static {
        init();
    }

    static void init() {
        // 1. system properties
        String modeProp = System.getProperty("fakefps.mode");
        if (modeProp != null) mode = modeProp.equalsIgnoreCase("random") ? 1 : 0;

        String val = System.getProperty("fakefps.value");
        if (val != null) { try { fakeFps = Integer.parseInt(val); } catch (NumberFormatException ignored) {} }

        val = System.getProperty("fakefps.min");
        if (val != null) { try { minFps = Integer.parseInt(val); } catch (NumberFormatException ignored) {} }

        val = System.getProperty("fakefps.max");
        if (val != null) { try { maxFps = Integer.parseInt(val); } catch (NumberFormatException ignored) {} }

        configuredMethod = System.getProperty("fakefps.method");

        // 2. environment variable
        String envVal = System.getenv("FAKEFPS_VALUE");
        if (envVal != null) { try { fakeFps = Integer.parseInt(envVal); } catch (NumberFormatException ignored) {} }

        // 3. config file
        tryLoadJson(Paths.get("fakefps-agent.json"));
        tryLoadJson(Paths.get("fakefps.json"));

        // 4. resolve GLFW for key toggle
        try {
            glfwClass = Class.forName("org.lwjgl.glfw.GLFW");
            glfwGetCurrentContext = glfwClass.getMethod("glfwGetCurrentContext");
            glfwGetKey = glfwClass.getMethod("glfwGetKey", long.class, int.class);
            glfwKeyF7 = glfwClass.getField("GLFW_KEY_F7").getInt(null);
            glfwKeyF6 = glfwClass.getField("GLFW_KEY_F6").getInt(null);
            glfwResolved = true;
            System.out.println("[FakeFPSAgent] GLFW key hooks active (F6=GUI, F7=toggle)");
        } catch (Exception e) {
            glfwResolved = false;
            System.out.println("[FakeFPSAgent] GLFW not found, key hooks unavailable");
        }
    }

    // -- Config file loading -----------------------------------------------------------

    private static void tryLoadJson(Path path) {
        if (!Files.exists(path)) return;
        try {
            String json = new String(Files.readAllBytes(path));
            parseSimpleJson(json);
        } catch (IOException e) {
            System.err.println("[FakeFPSAgent] Failed to read " + path + ": " + e.getMessage());
        }
    }

    private static void parseSimpleJson(String json) {
        json = json.replaceAll("\\s+", "");
        if (json.contains("\"mode\":\"random\"")) mode = 1;
        if (json.contains("\"mode\":\"static\"")) mode = 0;

        fakeFps = parseIntField(json, "fakeFps", fakeFps);
        fakeFps = parseIntField(json, "fakefps", fakeFps);
        fakeFps = parseIntField(json, "value", fakeFps);
        minFps = parseIntField(json, "minFps", minFps);
        minFps = parseIntField(json, "min", minFps);
        maxFps = parseIntField(json, "maxFps", maxFps);
        maxFps = parseIntField(json, "max", maxFps);
    }

    private static int parseIntField(String json, String key, int fallback) {
        int idx = json.indexOf('"' + key + '"');
        if (idx < 0) return fallback;
        int colon = json.indexOf(':', idx);
        if (colon < 0) return fallback;
        int end = json.indexOf(',', colon);
        if (end < 0) end = json.indexOf('}', colon);
        if (end < 0) end = json.indexOf(']', colon);
        if (end <= colon) return fallback;
        try { return Integer.parseInt(json.substring(colon + 1, end)); } catch (Exception e) { return fallback; }
    }

    // -- Called by transformed Minecraft.getFps() --------------------------------------
    // ASM replaces: IRETURN  →  INVOKESTATIC maybeReplace(I)I  ;  IRETURN
    public static int maybeReplace(int original) {
        if (!enabled) return original;
        if (mode == 1) {
            long now = System.nanoTime();
            if (now - lastRandomUpdate >= 1_000_000_000L) {
                int range = maxFps - minFps;
                cachedRandomFps = range > 0 ? minFps + random.nextInt(range + 1) : minFps;
                lastRandomUpdate = now;
            }
            return cachedRandomFps;
        }
        return fakeFps;
    }

    // -- Called every tick/frame from transformed tick() / render() / runTick() --------
    public static void checkToggle() {
        long now = System.nanoTime();
        if (now - lastToggleCheck < 50_000_000L) return; // ~20 Hz
        lastToggleCheck = now;

        if (glfwResolved) {
            tryGlfwKeys();
        }
        tryFileToggle();
    }

    // -- GLFW key handling (F6 = GUI, F7 = toggle) ------------------------------------
    private static void tryGlfwKeys() {
        try {
            long window = (long) glfwGetCurrentContext.invoke(null);
            if (window == 0) return;

            // F7 – toggle on/off
            int stateF7 = (int) glfwGetKey.invoke(null, window, glfwKeyF7);
            boolean f7Down = stateF7 == 1;
            if (f7Down && !f7WasDown) {
                enabled = !enabled;
                System.out.println("[FakeFPSAgent] " + (enabled ? "ON" : "OFF"));
            }
            f7WasDown = f7Down;

            // F6 – open config GUI (once)
            int stateF6 = (int) glfwGetKey.invoke(null, window, glfwKeyF6);
            boolean f6Down = stateF6 == 1;
            if (f6Down && !f6WasDown) {
                SwingUtilities.invokeLater(FPSService::showConfigGui);
            }
            f6WasDown = f6Down;

        } catch (Exception ignored) {
            // GLFW not accessible from this thread
        }
    }

    // -- Swing config GUI (opened by F6) -----------------------------------------------
    private static volatile boolean guiOpen;

    private static void showConfigGui() {
        if (guiOpen) return;
        guiOpen = true;
        try {
            // Copy current values for the GUI
            JFrame frame = new JFrame("FakeFPS Config");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setResizable(false);
            frame.setAlwaysOnTop(true);
            frame.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4, 6, 4, 6);
            c.anchor = GridBagConstraints.WEST;

            // -- Enabled checkbox
            JCheckBox enabledCb = new JCheckBox("Enabled", enabled);
            c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
            frame.add(enabledCb, c);

            // -- Mode radio buttons
            c.gridwidth = 2; c.gridx = 0; c.gridy = 1;
            frame.add(new JLabel("Mode:"), c);
            c.gridwidth = 1;
            JRadioButton staticRb = new JRadioButton("Static", mode == 0);
            JRadioButton randomRb = new JRadioButton("Random", mode == 1);
            ButtonGroup modeGroup = new ButtonGroup();
            modeGroup.add(staticRb);
            modeGroup.add(randomRb);
            c.gridx = 0; c.gridy = 2;
            frame.add(staticRb, c);
            c.gridx = 1;
            frame.add(randomRb, c);

            // -- Static value
            JTextField valueFld = new JTextField(8);
            valueFld.setText(String.valueOf(fakeFps));
            c.gridx = 0; c.gridy = 3; c.gridwidth = 1;
            frame.add(new JLabel("Fake FPS:"), c);
            c.gridx = 1;
            frame.add(valueFld, c);

            // -- Random min
            JTextField minFld = new JTextField(8);
            minFld.setText(String.valueOf(minFps));
            c.gridx = 0; c.gridy = 4;
            frame.add(new JLabel("Min FPS:"), c);
            c.gridx = 1;
            frame.add(minFld, c);

            // -- Random max
            JTextField maxFld = new JTextField(8);
            maxFld.setText(String.valueOf(maxFps));
            c.gridx = 0; c.gridy = 5;
            frame.add(new JLabel("Max FPS:"), c);
            c.gridx = 1;
            frame.add(maxFld, c);

            // -- Apply button
            JButton applyBtn = new JButton("Apply");
            c.gridx = 0; c.gridy = 6; c.gridwidth = 2;
            c.anchor = GridBagConstraints.CENTER;
            frame.add(applyBtn, c);

            applyBtn.addActionListener(e -> {
                enabled = enabledCb.isSelected();
                mode = staticRb.isSelected() ? 0 : 1;
                try { fakeFps = Integer.parseInt(valueFld.getText().trim()); } catch (Exception ignored) {}
                try { minFps  = Integer.parseInt(minFld.getText().trim()); } catch (Exception ignored) {}
                try { maxFps  = Integer.parseInt(maxFld.getText().trim()); } catch (Exception ignored) {}
                if (minFps > maxFps) { int t = minFps; minFps = maxFps; maxFps = t; }
                frame.dispose();
                System.out.println("[FakeFPSAgent] Config applied: " + getDescription());
            });

            frame.pack();
            // Center on screen
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        } finally {
            guiOpen = false;
        }
    }

    // -- File-based toggle (supplement) ------------------------------------------------
    private static Path toggleFile = Paths.get("fakefps-toggle.txt");
    private static long lastFileToggleCheck;

    private static void tryFileToggle() {
        long now = System.nanoTime();
        if (now - lastFileToggleCheck < 1_000_000_000L) return;
        lastFileToggleCheck = now;

        if (!Files.exists(toggleFile)) return;
        try {
            String content = new String(Files.readAllBytes(toggleFile)).trim();
            boolean shouldBe = content.equalsIgnoreCase("off")
                    || content.equalsIgnoreCase("0")
                    || content.equalsIgnoreCase("disable");
            if (shouldBe == enabled) {
                enabled = !shouldBe;
                System.out.println("[FakeFPSAgent] File toggle: "
                        + (enabled ? "ON" : "OFF"));
            }
        } catch (IOException ignored) {}
    }

    // -- Accessors --------------------------------------------------------------------
    public static String getConfiguredMethod() {
        return configuredMethod;
    }

    public static String getDescription() {
        if (mode == 1) return "mode=random(" + minFps + "-" + maxFps + ")" + " | "
                + (enabled ? "ON" : "OFF");
        return "mode=static(" + fakeFps + ")" + " | " + (enabled ? "ON" : "OFF");
    }
}
