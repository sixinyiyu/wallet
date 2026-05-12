package com.gemwallet.android.ext

import com.wallet.core.primitives.Chain
import com.wallet.core.primitives.NFTAssetId
import com.wallet.core.primitives.NFTCollectionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NftIdParsingTest {

    @Test
    fun parseAndFormat() {
        val ethAsset = "ethereum_0xabc::1".toNftAssetId()
        assertEquals(NFTAssetId(Chain.Ethereum, "0xabc", "1"), ethAsset)
        assertEquals("ethereum_0xabc::1", ethAsset?.toIdentifier())

        val ethCollection = "ethereum_0xabc".toNftCollectionId()
        assertEquals(NFTCollectionId(Chain.Ethereum, "0xabc"), ethCollection)
        assertEquals("ethereum_0xabc", ethCollection?.toIdentifier())

        val tonAsset = "ton_EQ_addr_with_underscores::EQ_token_with_underscores".toNftAssetId()
        assertEquals(
            NFTAssetId(Chain.Ton, "EQ_addr_with_underscores", "EQ_token_with_underscores"),
            tonAsset,
        )

        val tonMulti = "ton_EQabc::EQtoken::1".toNftAssetId()
        assertEquals(NFTAssetId(Chain.Ton, "EQabc", "EQtoken::1"), tonMulti)

        val tonCollection = "ton_EQ_addr_with_underscores".toNftCollectionId()
        assertEquals(NFTCollectionId(Chain.Ton, "EQ_addr_with_underscores"), tonCollection)

        assertNull("ethereum_0xabc".toNftAssetId())
        assertNull("nounderscore".toNftAssetId())
        assertNull("nounderscore".toNftCollectionId())
        assertNull("unknownchain_0xabc::1".toNftAssetId())
    }
}
