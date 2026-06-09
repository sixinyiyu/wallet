package com.gemwallet.android.data.service.store.database

import com.wallet.core.primitives.AssetLink
import org.junit.Assert.assertEquals
import org.junit.Test

class StoreConvertersTest {
    private val converters = StoreConverters()

    @Test
    fun assetLinks_roundTripAsJson() {
        val links = listOf(
            AssetLink(name = "website", url = "https://gemwallet.com"),
            AssetLink(name = "x", url = "https://x.com/gemwallet"),
        )

        assertEquals(links, converters.toAssetLinks(converters.fromAssetLinks(links)))
    }
}