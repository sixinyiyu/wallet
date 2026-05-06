package com.gemwallet.android.features.transfer_amount.viewmodels.providers

import com.gemwallet.android.application.perpetual.coordinators.GetPerpetual
import com.gemwallet.android.application.perpetual.coordinators.GetPerpetualBalance
import com.gemwallet.android.data.repositories.assets.AssetsRepository
import com.gemwallet.android.data.repositories.session.SessionRepository
import com.gemwallet.android.data.repositories.tokens.TokensRepository
import com.gemwallet.android.features.transfer_amount.viewmodels.AmountTitle
import com.gemwallet.android.model.AmountParams
import com.gemwallet.android.testkit.mockAssetCosmos
import com.wallet.core.primitives.PerpetualDirection
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test

class AmountPerpetualProviderTest {

    @Test
    fun `setLeverage updates the leverage flow`() {
        val provider = makeProvider()
        provider.setLeverage(10)
        assertEquals(10, provider.leverage.value)
    }

    @Test
    fun `title carries the direction`() {
        val provider = makeProvider(direction = PerpetualDirection.Short)
        val title = provider.title as AmountTitle.Perpetual
        assertEquals(PerpetualDirection.Short, title.direction)
    }

    private fun makeProvider(direction: PerpetualDirection = PerpetualDirection.Long): AmountPerpetualProvider {
        val asset = mockAssetCosmos()
        val assetsRepository = mockk<AssetsRepository>(relaxed = true) {
            every { getAssetInfo(any()) } returns flowOf(null)
        }
        val sessionRepository = mockk<SessionRepository>(relaxed = true) {
            every { session() } returns MutableStateFlow(null)
        }
        val tokenRepository = mockk<TokensRepository>(relaxed = true)
        val getPerpetual = mockk<GetPerpetual>(relaxed = true) {
            every { getPerpetual(any()) } returns flowOf(null)
        }
        val getPerpetualBalance = mockk<GetPerpetualBalance>(relaxed = true) {
            every { getBalance(any(), any()) } returns flowOf(null)
        }
        return AmountPerpetualProvider(
            params = AmountParams.Perpetual(asset.id, "BTC-PERP", direction),
            assetsRepository = assetsRepository,
            tokenRepository = tokenRepository,
            sessionRepository = sessionRepository,
            getPerpetual = getPerpetual,
            getPerpetualBalance = getPerpetualBalance,
            scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob()),
        )
    }
}
