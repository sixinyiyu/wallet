package com.gemwallet.android.features.transfer_amount.viewmodels.providers

import com.gemwallet.android.application.perpetual.coordinators.GetPerpetual
import com.gemwallet.android.application.perpetual.coordinators.GetPerpetualBalance
import com.gemwallet.android.data.repositories.assets.AssetsRepository
import com.gemwallet.android.data.repositories.session.SessionRepository
import com.gemwallet.android.data.repositories.tokens.TokensRepository
import com.gemwallet.android.domains.asset.chain
import com.gemwallet.android.ext.getAccount
import com.gemwallet.android.features.transfer_amount.viewmodels.AmountTitle
import com.gemwallet.android.model.AmountParams
import com.gemwallet.android.model.AssetInfo
import com.gemwallet.android.model.ConfirmParams
import com.gemwallet.android.model.Crypto
import com.wallet.core.primitives.AssetId
import com.wallet.core.primitives.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.math.BigInteger
import kotlin.math.min

@OptIn(ExperimentalCoroutinesApi::class)
class AmountPerpetualProvider(
    private val params: AmountParams.Perpetual,
    assetsRepository: AssetsRepository,
    tokenRepository: TokensRepository,
    sessionRepository: SessionRepository,
    getPerpetual: GetPerpetual,
    getPerpetualBalance: GetPerpetualBalance,
    scope: CoroutineScope,
) : AmountDataProvider {

    override val title: AmountTitle = AmountTitle.Perpetual(params.direction)
    override val canChangeValue: Boolean = true
    override val canSwitchInputType: Boolean = false
    override val minimumValue: BigInteger = BigInteger.ZERO
    override val reserveForFee: BigInteger = BigInteger.ZERO

    private val perpetual = getPerpetual.getPerpetual(params.perpetualId)
        .onEach { current ->
            val session = sessionRepository.session().firstOrNull() ?: return@onEach
            val perpetualAssetId = perpetualUsdcAssetId(current?.asset?.chain ?: return@onEach)
            tokenRepository.search(perpetualAssetId, session.currency)
            session.wallet.getAccount(perpetualAssetId.chain) ?: return@onEach
            assetsRepository.switchVisibility(session.wallet.id, perpetualAssetId, false)
        }
        .onEach { current -> selectedLeverage.update { min(current?.maxLeverage ?: 0, 5) } }
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val selectedLeverage = MutableStateFlow(0)
    val leverage: StateFlow<Int> = selectedLeverage.asStateFlow()
    fun setLeverage(value: Int) { selectedLeverage.update { value } }

    val availableLeverages: StateFlow<List<Int>> = perpetual.filterNotNull().map { current ->
        val minLeverage = min(current.maxLeverage, 5)
        (minLeverage..current.maxLeverage step 5).toList()
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val assetInfo: StateFlow<AssetInfo?> = perpetual.filterNotNull()
        .flatMapLatest { current -> assetsRepository.getAssetInfo(perpetualUsdcAssetId(current.asset.id.chain)) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    override val availableBalance: StateFlow<BigInteger> = sessionRepository.session()
        .filterNotNull()
        .flatMapLatest { getPerpetualBalance.getBalance(it.wallet.id) }
        .combine(assetInfo.filterNotNull()) { balance, current ->
            val available = balance?.available ?: 0.0
            Crypto(available.toBigDecimal(), current.asset.decimals).atomicValue
        }
        .stateIn(scope, SharingStarted.Eagerly, BigInteger.ZERO)

    override fun shouldReserveFee(isMaxAmount: Boolean): Boolean = false

    override suspend fun buildConfirmParams(amount: Crypto, isMax: Boolean): ConfirmParams {
        val current = assetInfo.value ?: error("assetInfo not loaded")
        val owner = current.owner ?: error("owner missing")
        val currentPerpetual = perpetual.value ?: error("perpetual not loaded")
        val builder = ConfirmParams.Builder(current.asset, owner, amount.atomicValue, isMax)
        return builder.perpetualOrder(
            perpetualId = currentPerpetual.id,
            perpetualPrice = currentPerpetual.price,
            perpetualProvider = currentPerpetual.provider,
            perpetualIdentifier = currentPerpetual.identifier,
            action = ConfirmParams.PerpetualParams.OrderAction.Open,
            leverage = selectedLeverage.value,
            baseAsset = current.asset,
            direction = params.direction,
            marginType = currentPerpetual.marginType,
        )
    }

    private fun perpetualUsdcAssetId(chain: Chain): AssetId = when (chain) {
        Chain.HyperCore -> AssetId(chain = chain, tokenId = "perpetual::USDC")
        else -> error("Unsupported perpetual chain: $chain")
    }
}
