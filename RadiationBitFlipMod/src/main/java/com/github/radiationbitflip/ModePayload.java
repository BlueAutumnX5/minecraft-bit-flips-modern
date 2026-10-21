package com.github.radiationbitflip;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

record ModePayload(int mode, int rate) implements CustomPacketPayload {
    static final Type<ModePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(RadiationBitFlipMod.ID, "mode"));
    static final StreamCodec<FriendlyByteBuf, ModePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE.map(Byte::intValue, Integer::byteValue), ModePayload::mode,
            ByteBufCodecs.BYTE.map(Byte::intValue, Integer::byteValue), ModePayload::rate,
            ModePayload::new);

    @Override
    public Type<ModePayload> type() {
        return TYPE;
    }
}
