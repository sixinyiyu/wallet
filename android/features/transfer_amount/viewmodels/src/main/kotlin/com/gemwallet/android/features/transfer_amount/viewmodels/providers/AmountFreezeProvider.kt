package com.gemwallet.android.features.transfer_amount.viewmodels.providers

import com.gemwallet.android.data.repositories.assets.AssetsRepository
import com.gemwallet.android.data.repositories.transactions.TransactionBalanceService
import com.gemwallet.android.features.transfer_amount.viewmodels.AmountTitle
import com.gemwallet.android.model.AmountParams
import com.gemwallet.android.model.AssetInfo
import com.gemwallet.android.model.ConfirmParams
import com.gemwallet.android.model.Crypto
import com.wallet.core.primitives.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import uniffi.gemstone.Config
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
class AmountFreezeProvider(
    private val params: AmountParams.Freeze,
    assetsRepository: AssetsRepository,
    private val transactionBalanceService: TransactionBalanceService,
    scope: CoroutineScope,
) : AmountDataProvider {

    override val title: AmountTitle = AmountTitle.Freeze(params.direction)
    override val canChangeValue: Boolean = true
    override val canSwitchInputType: Boolean = false

    override val minimumValue: BigInteger = when (params.direction) {
        AmountParams.Freeze.Direction.Freeze ->
            BigInteger.valueOf(Config().getStakeConfig(params.assetId.chain.string).minAmount.toLong())
        AmountParams.Freeze.Direction.Unfreeze -> BigInteger.ZERO
    }

    override val reserveForFee: BigInteger = when (params.direction) {
        AmountParams.Freeze.Direction.Freeze ->
            BigInteger.valueOf(Config().getStakeConfig(params.assetId.chain.string).reservedForFees.toLong())
        AmountParams.Freeze.Direction.Unfreeze -> BigInteger.ZERO
    }

    private val selectedResource = MutableStateFlow(Resource.Bandwidth)
    val resource: StateFlow<Resource> = selectedResource.asStateFlow()

    fun setResource(value: Resource) {
        selectedResource.update { value }
    }

    override val assetInfo: StateFlow<AssetInfo?> =
        assetsRepository.getAssetInfo(params.assetId)
            .flowOn(Dispatchers.IO)
            .stateIn(scope, SharingStarted.Eagerly, null)

    override val availableBalance: StateFlow<BigInteger> =
        combine(assetInfo.filterNotNull(), selectedResource) { current, currentResource -> current to currentResource }
            .mapLatest { (current, currentResource) ->
                transactionBalanceService.getBalance(current, params, resource = currentResource)
            }
            .flowOn(Dispatchers.IO)
            .stateIn(scope, SharingStarted.Eagerly, BigInteger.ZERO)

    override fun shouldReserveFee(isMaxAmount: Boolean): Boolean = when (params.direction) {
        AmountParams.Freeze.Direction.Freeze -> {
            if (!isMaxAmount || reserveForFee.signum() == 0) false
            else (availableBalance.value - reserveForFee).max(BigInteger.ZERO) > minimumValue
        }
        AmountParams.Freeze.Direction.Unfreeze -> false
    }

    override fun buildConfirmParams(amount: Crypto, isMax: Boolean): ConfirmParams {
        val current = assetInfo.value ?: error("assetInfo not loaded")
        val owner = current.owner ?: error("owner missing")
        val builder = ConfirmParams.Builder(current.asset, owner, amount.atomicValue, isMax)
        return when (params.direction) {
            AmountParams.Freeze.Direction.Freeze -> builder.freeze(selectedResource.value)
            AmountParams.Freeze.Direction.Unfreeze -> builder.unfreeze(selectedResource.value)
        }
    }
}
