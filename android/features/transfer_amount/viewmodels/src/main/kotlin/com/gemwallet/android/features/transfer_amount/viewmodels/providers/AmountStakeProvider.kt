package com.gemwallet.android.features.transfer_amount.viewmodels.providers

import com.gemwallet.android.data.repositories.assets.AssetsRepository
import com.gemwallet.android.data.repositories.stake.StakeRepository
import com.gemwallet.android.data.repositories.transactions.TransactionBalanceService
import com.gemwallet.android.domains.stake.hasRewards
import com.gemwallet.android.ext.byChain
import com.gemwallet.android.ext.freezed
import com.gemwallet.android.features.transfer_amount.models.AmountError
import com.gemwallet.android.features.transfer_amount.models.ValidatorsSource
import com.gemwallet.android.features.transfer_amount.viewmodels.AmountTitle
import com.gemwallet.android.model.AmountParams
import com.gemwallet.android.model.AssetInfo
import com.gemwallet.android.model.ConfirmParams
import com.gemwallet.android.model.Crypto
import com.wallet.core.primitives.Delegation
import com.wallet.core.primitives.DelegationValidator
import com.wallet.core.primitives.StakeChain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import uniffi.gemstone.Config
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
class AmountStakeProvider(
    private val params: AmountParams.Stake,
    assetsRepository: AssetsRepository,
    private val stakeRepository: StakeRepository,
    private val transactionBalanceService: TransactionBalanceService,
    scope: CoroutineScope,
) : AmountDataProvider {

    override val title: AmountTitle = AmountTitle.Stake(params)
    override val canSwitchInputType: Boolean = false

    override val canChangeValue: Boolean = when (params) {
        is AmountParams.Stake.Delegate,
        is AmountParams.Stake.Redelegate,
        is AmountParams.Stake.Undelegate -> true
        is AmountParams.Stake.Withdraw,
        is AmountParams.Stake.Rewards -> false
    }

    override val minimumValue: BigInteger
        get() = when (params) {
            is AmountParams.Stake.Delegate,
            is AmountParams.Stake.Redelegate ->
                BigInteger.valueOf(Config().getStakeConfig(params.assetId.chain.string).minAmount.toLong())
            is AmountParams.Stake.Undelegate,
            is AmountParams.Stake.Withdraw,
            is AmountParams.Stake.Rewards -> BigInteger.ZERO
        }

    override val reserveForFee: BigInteger
        get() = when (params) {
            is AmountParams.Stake.Delegate -> when (StakeChain.byChain(params.assetId.chain)?.freezed()) {
                true -> BigInteger.ZERO
                else -> BigInteger.valueOf(Config().getStakeConfig(params.assetId.chain.string).reservedForFees.toLong())
            }
            is AmountParams.Stake.Undelegate,
            is AmountParams.Stake.Redelegate,
            is AmountParams.Stake.Withdraw,
            is AmountParams.Stake.Rewards -> BigInteger.ZERO
        }

    override val assetInfo: StateFlow<AssetInfo?> =
        assetsRepository.getAssetInfo(params.assetId)
            .flowOn(Dispatchers.IO)
            .stateIn(scope, SharingStarted.Eagerly, null)

    private val selectedValidatorId = MutableStateFlow(initialValidatorId(params))

    private val delegation: StateFlow<Delegation?> = run {
        val source = when (params) {
            is AmountParams.Stake.Undelegate ->
                stakeRepository.getDelegation(validatorId = "", delegationId = params.delegationId)
            is AmountParams.Stake.Redelegate ->
                stakeRepository.getDelegation(validatorId = params.validatorId, delegationId = params.delegationId)
            is AmountParams.Stake.Withdraw ->
                stakeRepository.getDelegation(validatorId = "", delegationId = params.delegationId)
            is AmountParams.Stake.Rewards -> {
                val rewardsList = assetInfo.filterNotNull().flatMapLatest { current ->
                    val owner = current.owner?.address ?: return@flatMapLatest flowOf<List<Delegation>>(emptyList())
                    stakeRepository.getDelegations(current.asset.id, owner)
                }
                combine(rewardsList, selectedValidatorId) { delegations, pickedId ->
                    val withRewards = delegations.filter { it.hasRewards() }
                    withRewards.firstOrNull { it.validator.id == pickedId } ?: withRewards.firstOrNull()
                }
            }
            else -> flowOf(null)
        }
        source.flowOn(Dispatchers.IO).stateIn(scope, SharingStarted.Eagerly, null)
    }

    private val recommendedValidator: StateFlow<DelegationValidator?> = when (params) {
        is AmountParams.Stake.Delegate,
        is AmountParams.Stake.Redelegate -> stakeRepository.getRecommended(params.assetId.chain)
            .flowOn(Dispatchers.IO)
            .stateIn(scope, SharingStarted.Eagerly, null)
        else -> MutableStateFlow(null)
    }

    val validatorState: StateFlow<DelegationValidator?> =
        combine(assetInfo, delegation, selectedValidatorId, recommendedValidator) { current, currentDelegation, pickedId, recommended ->
            val byId = if (current != null && pickedId != null) {
                stakeRepository.getStakeValidator(current.asset.id, pickedId)
            } else {
                null
            }
            byId ?: currentDelegation?.validator ?: recommended
        }.flowOn(Dispatchers.IO).stateIn(scope, SharingStarted.Eagerly, null)

    val validatorSource: StateFlow<ValidatorsSource?> = assetInfo.mapLatest { current ->
        when (params) {
            is AmountParams.Stake.Rewards ->
                current?.owner?.address?.let { ValidatorsSource.Rewards(params.assetId, it) }
            else -> ValidatorsSource.ChainValidators(chain = params.assetId.chain)
        }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    val canSelectValidator: Boolean = when (params) {
        is AmountParams.Stake.Delegate,
        is AmountParams.Stake.Redelegate,
        is AmountParams.Stake.Rewards -> true
        is AmountParams.Stake.Undelegate,
        is AmountParams.Stake.Withdraw -> false
    }

    fun selectValidator(id: String?) {
        selectedValidatorId.update { id }
    }

    override val availableBalance: StateFlow<BigInteger> =
        combine(assetInfo.filterNotNull(), delegation) { current, currentDelegation -> current to currentDelegation }
            .mapLatest { (current, currentDelegation) ->
                transactionBalanceService.getBalance(current, params, delegation = currentDelegation)
            }
            .flowOn(Dispatchers.IO)
            .stateIn(scope, SharingStarted.Eagerly, BigInteger.ZERO)

    override fun shouldReserveFee(isMaxAmount: Boolean): Boolean {
        if (!isMaxAmount || reserveForFee.signum() == 0) return false
        if (params !is AmountParams.Stake.Delegate) return false
        val maxAfterFee = (availableBalance.value - reserveForFee).max(BigInteger.ZERO)
        return maxAfterFee > minimumValue
    }

    override fun buildConfirmParams(amount: Crypto, isMax: Boolean): ConfirmParams {
        val current = assetInfo.value ?: error("assetInfo not loaded")
        val owner = current.owner ?: error("owner missing")
        val builder = ConfirmParams.Builder(current.asset, owner, amount.atomicValue, isMax)
        return when (params) {
            is AmountParams.Stake.Delegate -> {
                val validator = validatorState.value ?: throw AmountError.NoValidatorSelected
                builder.delegate(validator)
            }
            is AmountParams.Stake.Redelegate -> {
                val validator = validatorState.value ?: throw AmountError.NoValidatorSelected
                val currentDelegation = delegation.value ?: throw AmountError.NoDelegationSelected
                builder.redelegate(validator, currentDelegation)
            }
            is AmountParams.Stake.Undelegate -> {
                val currentDelegation = delegation.value ?: throw AmountError.NoDelegationSelected
                builder.undelegate(currentDelegation)
            }
            is AmountParams.Stake.Withdraw -> {
                val currentDelegation = delegation.value ?: throw AmountError.NoDelegationSelected
                builder.withdraw(currentDelegation)
            }
            is AmountParams.Stake.Rewards -> {
                val validator = validatorState.value ?: throw AmountError.NoValidatorSelected
                builder.rewards(listOf(validator))
            }
        }
    }

    private fun initialValidatorId(params: AmountParams.Stake): String? = when (params) {
        is AmountParams.Stake.Delegate -> params.validatorId
        is AmountParams.Stake.Redelegate -> params.validatorId
        else -> null
    }
}
