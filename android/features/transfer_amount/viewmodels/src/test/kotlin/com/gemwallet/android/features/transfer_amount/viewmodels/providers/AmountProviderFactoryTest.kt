package com.gemwallet.android.features.transfer_amount.viewmodels.providers

import com.gemwallet.android.application.perpetual.coordinators.GetPerpetual
import com.gemwallet.android.application.perpetual.coordinators.GetPerpetualBalance
import com.gemwallet.android.data.repositories.assets.AssetsRepository
import com.gemwallet.android.data.repositories.session.SessionRepository
import com.gemwallet.android.data.repositories.stake.StakeRepository
import com.gemwallet.android.data.repositories.tokens.TokensRepository
import com.gemwallet.android.data.repositories.transactions.TransactionBalanceService
import com.gemwallet.android.model.AmountParams
import com.gemwallet.android.model.DestinationAddress
import com.gemwallet.android.testkit.mockAssetCosmos
import com.wallet.core.primitives.PerpetualDirection
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountProviderFactoryTest {

    private val asset = mockAssetCosmos()
    private val factory = AmountProviderFactory(
        assetsRepository = mockk<AssetsRepository>(relaxed = true) {
            every { getAssetInfo(any()) } returns flowOf(null)
        },
        stakeRepository = mockk(relaxed = true),
        transactionBalanceService = mockk(relaxed = true),
        getPerpetual = mockk<GetPerpetual>(relaxed = true) {
            every { getPerpetual(any()) } returns flowOf(null)
        },
        getPerpetualBalance = mockk<GetPerpetualBalance>(relaxed = true) {
            every { getBalance(any(), any()) } returns flowOf(null)
        },
        sessionRepository = mockk<SessionRepository>(relaxed = true) {
            every { session() } returns MutableStateFlow(null)
        },
        tokenRepository = mockk(relaxed = true),
    )
    private val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

    @Test
    fun `Transfer params produce TransferProvider`() {
        val provider = factory.create(
            AmountParams.Transfer(asset.id, DestinationAddress("to", null), null),
            scope,
        )
        assertTrue(provider is AmountTransferProvider)
    }

    @Test
    fun `Stake variants produce StakeProvider`() {
        assertTrue(factory.create(AmountParams.Stake.Delegate(asset.id), scope) is AmountStakeProvider)
        assertTrue(factory.create(AmountParams.Stake.Rewards(asset.id), scope) is AmountStakeProvider)
        assertTrue(factory.create(AmountParams.Stake.Withdraw(asset.id, "v1", "d1"), scope) is AmountStakeProvider)
    }

    @Test
    fun `Freeze params produce FreezeProvider`() {
        val provider = factory.create(
            AmountParams.Freeze(asset.id, AmountParams.Freeze.Direction.Freeze),
            scope,
        )
        assertTrue(provider is AmountFreezeProvider)
    }

    @Test
    fun `Perpetual params produce PerpetualProvider`() {
        val provider = factory.create(
            AmountParams.Perpetual(asset.id, "BTC-PERP", PerpetualDirection.Long),
            scope,
        )
        assertTrue(provider is AmountPerpetualProvider)
    }
}
