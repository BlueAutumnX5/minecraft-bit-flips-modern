package com.github.radiationbitflip;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public class ModeScreen extends Screen {
    private static final String[] MODE_LABELS = {"[off]", "[on - full]", "[on - solids only]"};
    private static final String[] RATE_LABELS = {"[low]", "[medium]", "[high]", "[replay]"};
    private static int selectedMode = RadiationBitFlipMod.MODE_SOLIDS_ONLY;
    private static int selectedRate = RadiationBitFlipMod.RATE_MEDIUM;
    private static boolean pendingSettings;
    private final Screen parent;
    private final Button[] modeButtons = new Button[MODE_LABELS.length];
    private final Button[] rateButtons = new Button[RATE_LABELS.length];

    public ModeScreen(Screen parent) {
        super(Component.literal("Radiation Bit Flip Mod"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = 120;
        int gap = 6;
        int left = (width - (3 * buttonWidth + 2 * gap)) / 2;
        int y = height / 2 - 25;
        for (int i = 0; i < modeButtons.length; i++) {
            int mode = i;
            modeButtons[i] = addRenderableWidget(Button.builder(Component.literal(MODE_LABELS[i]),
                    button -> select(mode, selectedRate))
                    .bounds(left + i * (buttonWidth + gap), y, buttonWidth, 20).build());
        }

        int rateWidth = 88;
        left = (width - (4 * rateWidth + 3 * gap)) / 2;
        y += 48;
        for (int i = 0; i < rateButtons.length; i++) {
            int rate = i;
            rateButtons[i] = addRenderableWidget(Button.builder(Component.literal(RATE_LABELS[i]),
                    button -> select(rate == RadiationBitFlipMod.RATE_REPLAY
                            ? RadiationBitFlipMod.MODE_FULL : selectedMode, rate))
                    .bounds(left + i * (rateWidth + gap), y, rateWidth, 20).build());
        }
        updateButtons();
    }

    private void select(int mode, int rate) {
        selectedMode = mode;
        selectedRate = rate;
        pendingSettings = true;
        sendSettings();
        updateButtons();
    }

    private void updateButtons() {
        for (int i = 0; i < modeButtons.length; i++) modeButtons[i].active = i != selectedMode;
        for (int i = 0; i < rateButtons.length; i++) rateButtons[i].active = i != selectedRate;
    }

    static void sendSettings() {
        if (!pendingSettings) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) return;
        // servers without the mod don't have the channel
        if (!ClientPlayNetworking.canSend(ModePayload.TYPE)) return;
        ClientPlayNetworking.send(new ModePayload(selectedMode, selectedRate));
        pendingSettings = false;
    }

    static void resetSelection() {
        selectedMode = RadiationBitFlipMod.MODE_SOLIDS_ONLY;
        selectedRate = RadiationBitFlipMod.RATE_MEDIUM;
        pendingSettings = false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (RadiationBitFlipModClient.openMenu.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 72, 0xFFFFFFFF);
        graphics.drawCenteredString(font, "type", width / 2, height / 2 - 39, 0xFFA0A0A0);
        graphics.drawCenteredString(font, "flip rate", width / 2, height / 2 + 9, 0xFFA0A0A0);
    }
}
