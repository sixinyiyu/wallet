package com.gemwallet.android.serializer

import com.gemwallet.android.ext.toIdentifier
import com.gemwallet.android.ext.toNftCollectionId
import com.wallet.core.primitives.NFTCollectionId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.IOException

object NFTCollectionIdSerializer : KSerializer<NFTCollectionId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(NFTCollectionId::class.simpleName!!, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: NFTCollectionId) {
        encoder.encodeString(value.toIdentifier())
    }

    override fun deserialize(decoder: Decoder): NFTCollectionId {
        val value = decoder.decodeString()
        return value.toNftCollectionId() ?: throw IOException("Invalid NFTCollectionId: $value")
    }
}
