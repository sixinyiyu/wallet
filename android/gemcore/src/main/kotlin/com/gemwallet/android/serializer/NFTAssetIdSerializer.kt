package com.gemwallet.android.serializer

import com.gemwallet.android.ext.toIdentifier
import com.gemwallet.android.ext.toNftAssetId
import com.wallet.core.primitives.NFTAssetId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.IOException

object NFTAssetIdSerializer : KSerializer<NFTAssetId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(NFTAssetId::class.simpleName!!, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: NFTAssetId) {
        encoder.encodeString(value.toIdentifier())
    }

    override fun deserialize(decoder: Decoder): NFTAssetId {
        val value = decoder.decodeString()
        return value.toNftAssetId() ?: throw IOException("Invalid NFTAssetId: $value")
    }
}
