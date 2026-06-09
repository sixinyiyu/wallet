package com.gemwallet.android.serializer

import com.wallet.core.primitives.StreamEvent
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamEventSerializerTest {

    @Test
    fun `transforms event key to type and passes through type key`() {
        val withEventKey = """{"event": "balances", "data": {"walletId": "w1", "assetId": "bitcoin", "balance": "1.0"}}"""
        val withTypeKey = """{"type": "balances", "data": {"walletId": "w1", "assetId": "bitcoin", "balance": "1.0"}}"""

        assertTrue(jsonEncoder.decodeFromString(StreamEventSerializer, withEventKey) is StreamEvent.Balances)
        assertTrue(jsonEncoder.decodeFromString(StreamEventSerializer, withTypeKey) is StreamEvent.Balances)
    }
}