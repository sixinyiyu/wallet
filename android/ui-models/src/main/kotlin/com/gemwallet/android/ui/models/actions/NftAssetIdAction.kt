package com.gemwallet.android.ui.models.actions

import com.wallet.core.primitives.NFTAssetId

fun interface NftAssetIdAction {
    operator fun invoke(id: NFTAssetId)
}
