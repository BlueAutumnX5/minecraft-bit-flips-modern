package com.github.radiationbitflip;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class RadiationBitFlipMod implements ModInitializer {
    static final String ID = "radiation-bit-flip";
    static final int MODE_OFF = 0;
    static final int MODE_FULL = 1;
    static final int MODE_SOLIDS_ONLY = 2;
    static final int RATE_LOW = 0;
    static final int RATE_MEDIUM = 1;
    static final int RATE_HIGH = 2;
    static final int RATE_REPLAY = 3;

    // representative rates from the original ram chip experiment
    private static final double LOW_RATE = 0.00000236689D;
    private static final double MEDIUM_RATE = 0.000224665D;
    private static final double HIGH_RATE = 0.0167743D;
    private static final int BLOCKS_PER_SECTION = 16 * 16 * 16;
    private static final int BITS_PER_BLOCK_ID = 8;

    private static final Random random = new Random();
    private static volatile int mode = MODE_SOLIDS_ONLY;
    private static volatile int rateMode = RATE_MEDIUM;
    private static long replayTicks;

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(ModePayload.TYPE, ModePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ModePayload.TYPE,
                (payload, context) -> setSettings(payload.mode(), payload.rate()));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> LegacyBlockIds.load());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            mode = MODE_SOLIDS_ONLY;
            rateMode = RATE_MEDIUM;
            replayTicks = 0;
        });
        ServerTickEvents.END_SERVER_TICK.register(RadiationBitFlipMod::onServerTick);
    }

    static void setSettings(int requestedMode, int requestedRate) {
        if (requestedMode >= MODE_OFF && requestedMode <= MODE_SOLIDS_ONLY) {
            mode = requestedMode;
        }
        if (requestedRate >= RATE_LOW && requestedRate <= RATE_REPLAY) {
            if (requestedRate == RATE_REPLAY && rateMode != RATE_REPLAY) replayTicks = 0;
            rateMode = requestedRate;
        }
    }

    private static void onServerTick(MinecraftServer server) {
        if (rateMode == RATE_REPLAY) replayTicks++;
        if (mode == MODE_OFF || !LegacyBlockIds.isLoaded()) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickPlayer(player);
        }
    }

    private static void tickPlayer(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos center = player.blockPosition();
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;
        int centerSectionY = center.getY() >> 4;
        int minSectionY = level.getMinSectionY();
        int maxSectionY = level.getMaxSectionY();
        List<Section> sections = new ArrayList<>(64);

        // the four sections on each axis are -1, 0, +1, +2 from the player's section.
        // only loaded chunks are considered; this does not load or generate terrain.
        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -1; dz <= 2; dz++) {
                int chunkX = centerChunkX + dx;
                int chunkZ = centerChunkZ + dz;
                if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) continue;
                for (int dy = -1; dy <= 2; dy++) {
                    int sectionY = centerSectionY + dy;
                    if (sectionY >= minSectionY && sectionY <= maxSectionY) {
                        sections.add(new Section(chunkX, sectionY, chunkZ));
                    }
                }
            }
        }
        if (sections.isEmpty()) return;

        // at 20 ticks/s, every represented block-id bit has the recorded
        // probability of an upset. round the fractional event count randomly.
        double expected = sections.size() * BLOCKS_PER_SECTION * BITS_PER_BLOCK_ID
                * getCurrentRate() / 20.0D;
        int attempts = (int) expected;
        if (random.nextDouble() < expected - attempts) attempts++;
        int selectedMode = mode;
        for (int i = 0; i < attempts; i++) {
            Section section = sections.get(random.nextInt(sections.size()));
            int index = random.nextInt(BLOCKS_PER_SECTION);
            BlockPos pos = new BlockPos((section.x() << 4) + (index & 15),
                    (section.y() << 4) + ((index >> 8) & 15),
                    (section.z() << 4) + ((index >> 4) & 15));
            flip(level, pos, selectedMode);
        }
    }

    private static double getCurrentRate() {
        if (rateMode == RATE_LOW) return LOW_RATE;
        if (rateMode == RATE_HIGH) return HIGH_RATE;
        if (rateMode != RATE_REPLAY) return MEDIUM_RATE;

        double seconds = replayTicks / 20.0D;
        if (seconds < 10.0D) return LOW_RATE;
        if (seconds < 20.0D) return mix(LOW_RATE, MEDIUM_RATE, (seconds - 10.0D) / 10.0D);
        if (seconds < 110.0D) return MEDIUM_RATE;
        if (seconds < 120.0D) return mix(MEDIUM_RATE, HIGH_RATE, (seconds - 110.0D) / 10.0D);
        return HIGH_RATE;
    }

    private static double mix(double start, double end, double amount) {
        return start + (end - start) * amount;
    }

    private static void flip(ServerLevel level, BlockPos pos, int selectedMode) {
        BlockState original = level.getBlockState(pos);
        if (selectedMode == MODE_SOLIDS_ONLY && !original.isSolid()) return;

        // blocks are flipped by their 1.12 id, metadata stays the same
        int oldKey = LegacyBlockIds.toLegacy(original);
        if (oldKey < 0) return;
        int newKey = oldKey ^ (1 << (4 + random.nextInt(BITS_PER_BLOCK_ID)));
        BlockState next = LegacyBlockIds.fromLegacy(newKey);
        // unknown ids or metadata keep the original block
        if (next == null) return;

        try {
            // 1.12 worked out connections on render, so refresh them here.
            // blocks that would pop off stay floating like they did back then.
            BlockState shaped = Block.updateFromNeighbourShapes(next, level, pos);
            if (!shaped.isAir() || next.isAir()) next = shaped;
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
        } catch (RuntimeException failure) {
            // if a block rejects the new state, retain the original state.
            try {
                if (level.getBlockState(pos) != original) level.setBlock(pos, original, Block.UPDATE_CLIENTS);
            } catch (RuntimeException ignored) {
                // there is no safe further operation if the world itself rejects rollback.
            }
        }
    }

    private record Section(int x, int y, int z) { }
}
