package com.github.radiationbitflip;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class RadiationBitFlipModClient implements ClientModInitializer {
    static KeyMapping openMenu;
    private boolean hadPlayer;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(RadiationBitFlipMod.ID, "main"));
        openMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.radiation-bit-flip.menu",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, category));
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(Minecraft mc) {
        while (openMenu.consumeClick()) {
            if (mc.player != null && mc.screen == null) mc.setScreen(new ModeScreen(null));
        }
        boolean hasPlayer = mc.player != null;
        if (!hasPlayer && hadPlayer) ModeScreen.resetSelection();
        hadPlayer = hasPlayer;
        ModeScreen.sendSettings();
    }
}
