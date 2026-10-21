package com.github.radiationbitflip;

import com.mojang.serialization.Dynamic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.BlockStateData;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Maps block states to and from 1.12 (id << 4 | meta) keys, using the
 * game's own flattening table so ids match the 1.12 version of the mod.
 */
final class LegacyBlockIds {
    private static final int KEY_COUNT = 256 * 16;
    // data version the flattening table was written for
    private static final int FLATTENING_VERSION = 1451;
    // still water, still lava and dead bush share states with other ids
    private static final int[] PREFERRED_IDS = {9, 11, 32};

    private static BlockState[] byKey;
    private static final Map<BlockState, Integer> byState = new HashMap<>();
    private static final Map<Block, List<Integer>> byBlock = new HashMap<>();

    private LegacyBlockIds() { }

    static synchronized void load() {
        if (byKey != null) return;
        BlockState[] states = new BlockState[KEY_COUNT];
        Dynamic<?> unknown = BlockStateData.getTag(-1);
        for (int key = 0; key < KEY_COUNT; key++) {
            Dynamic<?> tag = BlockStateData.getTag(key);
            // missing ids fall back to air, missing metadata to the block default
            if (key != 0 && tag == unknown) continue;
            if (isFiller(key, tag) || tag.get("Name").asString("").equals("%%FILTER_ME%%")) continue;
            BlockState state = upgrade(tag);
            if (state == null) continue;
            states[key] = state;
            byBlock.computeIfAbsent(state.getBlock(), block -> new ArrayList<>()).add(key);
        }
        for (int id : PREFERRED_IDS) {
            for (int meta = 0; meta < 16; meta++) {
                if (states[id << 4 | meta] != null) byState.putIfAbsent(states[id << 4 | meta], id << 4 | meta);
            }
        }
        for (int key = 0; key < KEY_COUNT; key++) {
            if (states[key] != null) byState.putIfAbsent(states[key], key);
        }
        byKey = states;
    }

    static boolean isLoaded() {
        return byKey != null;
    }

    static BlockState fromLegacy(int key) {
        return key >= 0 && key < KEY_COUNT ? byKey[key] : null;
    }

    /** Returns the closest 1.12 key for a state, or -1 if the block did not exist in 1.12. */
    static int toLegacy(BlockState state) {
        Integer cached = byState.get(state);
        if (cached != null) return cached;

        int best = state.isAir() ? 0 : -1;
        int bestScore = -1;
        for (int key : byBlock.getOrDefault(state.getBlock(), List.of())) {
            // modern only properties (waterlogged, connections) are ignored by
            // picking the variant that shares the most property values
            BlockState legacy = byKey[key];
            int score = 0;
            for (Property<?> property : state.getProperties()) {
                if (legacy.getValue(property).equals(state.getValue(property))) score++;
            }
            if (score > bestScore) {
                best = key;
                bestScore = score;
            }
        }
        byState.put(state, best);
        return best;
    }

    private static boolean isFiller(int key, Dynamic<?> tag) {
        int first = key & ~15;
        for (int other = first; other < key; other++) {
            if (BlockStateData.getTag(other) == tag) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static BlockState upgrade(Dynamic<?> tag) {
        Dynamic<Tag> updated = DataFixers.getDataFixer().update(References.BLOCK_STATE, (Dynamic<Tag>) tag,
                FLATTENING_VERSION, SharedConstants.getCurrentVersion().dataVersion().version());
        if (!(updated.getValue() instanceof CompoundTag compound)) return null;
        BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK, compound);
        // unknown names are read as air
        if (state.is(Blocks.AIR) && !compound.getStringOr("Name", "").equals("minecraft:air")) return null;
        return state;
    }
}
