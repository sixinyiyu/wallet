package com.gemwallet.android.features.recipient.viewmodel.models

import com.gemwallet.android.model.AssetInfo

sealed interface RecipientType {
    val assetInfo: AssetInfo

    data class Asset(override val assetInfo: AssetInfo) : RecipientType
}

sealed interface RecipientState {
    data object Loading : RecipientState
    data class Ready(val type: RecipientType) : RecipientState
}

sealed interface RecipientError {
    data object Empty : RecipientError
    data class Invalid(val message: String) : RecipientError
}