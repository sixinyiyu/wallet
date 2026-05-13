package com.gemwallet.android.serializer

import com.wallet.core.primitives.AssetId
import com.wallet.core.primitives.Chain
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Test

class AssetIdSerializerTest {

    @Test
    fun `serializes and deserializes AssetId as identifier string`() {
        val assetId = AssetId(chain = Chain.Ethereum, tokenId = "0xabc")
        val json = jsonEncoder.encodeToString(assetId)

        assertEquals("\"ethereum_0xabc\"", json)
        assertEquals(assetId, jsonEncoder.decodeFromString<AssetId>(json))
    }

    @Test
    fun `deserializes AssetId from object payload`() {
        val json = """{"chain":"ethereum","tokenId":"0xabc"}"""

        val decoded = jsonEncoder.decodeFromString<AssetId>(json)

        assertEquals(AssetId(chain = Chain.Ethereum, tokenId = "0xabc"), decoded)
    }
}
