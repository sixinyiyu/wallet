package com.gemwallet.android.features.wallet.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemwallet.android.application.wallet.coordinators.GetWalletDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletImageViewModel @Inject constructor(
    getWalletDetails: GetWalletDetails,
    private val avatarService: WalletAvatarService,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val walletId = savedStateHandle.requireWalletId()

    val wallet = getWalletDetails.getWallet(walletId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val emojis: List<String> = WalletAvatarEmoji.all

    fun setEmoji(emoji: String, backgroundColor: Int) = viewModelScope.launch(Dispatchers.IO) {
        avatarService.setEmoji(walletId, wallet.value?.imageUrl, emoji, backgroundColor)
    }

    fun resetToDefault() = viewModelScope.launch(Dispatchers.IO) {
        avatarService.reset(walletId, wallet.value?.imageUrl)
    }
}