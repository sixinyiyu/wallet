package com.gemwallet.android.features.transfer_amount.viewmodels.providers

import com.gemwallet.android.data.repositories.assets.AssetsRepository
import com.gemwallet.android.data.repositories.transactions.TransactionBalanceService
import com.gemwallet.android.model.AmountParams
import com.gemwallet.android.model.ConfirmParams
import com.gemwallet.android.model.Crypto
import com.gemwallet.android.testkit.mockAssetCosmos
import com.gemwallet.android.testkit.mockAssetInfo
import com.wallet.core.primitives.Resource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class AmountFreezeProviderTest {

    private val asset = mockAssetCosmos()
    private val assetInfo = mockAssetInfo(asset = asset)
    private val assetsRepository = mockk<AssetsRepository> {
        every { getAssetInfo(asset.id) } returns flowOf(assetInfo)
    }
    private val balanceService = mockk<TransactionBalanceService> {
        coEvery { getBalance(any(), any<AmountParams>(), any(), any()) } returns BigInteger("1000")
    }
    private val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

    private fun makeProvider(direction: AmountParams.Freeze.Direction) = AmountFreezeProvider(
        params = AmountParams.Freeze(asset.id, direction),
        assetsRepository = assetsRepository,
        transactionBalanceService = balanceService,
        scope = scope,
    )

    @Test
    fun `freeze builds Stake Freeze ConfirmParams with selected resource`() = runBlocking {
        val provider = makeProvider(AmountParams.Freeze.Direction.Freeze)
        provider.setResource(Resource.Bandwidth)
        val confirm = provider.buildConfirmParams(Crypto(BigInteger.ONE), isMax = false)
        assertTrue(confirm is ConfirmParams.Stake.Freeze)
        assertEquals(Resource.Bandwidth, (confirm as ConfirmParams.Stake.Freeze).resource)
    }

    @Test
    fun `unfreeze builds Stake Unfreeze ConfirmParams`() = runBlocking {
        val provider = makeProvider(AmountParams.Freeze.Direction.Unfreeze)
        provider.setResource(Resource.Energy)
        val confirm = provider.buildConfirmParams(Crypto(BigInteger.ONE), isMax = false)
        assertTrue(confirm is ConfirmParams.Stake.Unfreeze)
    }

    @Test
    fun `unfreeze direction has zero minimum and zero reserve`() {
        val provider = makeProvider(AmountParams.Freeze.Direction.Unfreeze)
        assertEquals(BigInteger.ZERO, provider.minimumValue)
        assertEquals(BigInteger.ZERO, provider.reserveForFee)
    }
}
