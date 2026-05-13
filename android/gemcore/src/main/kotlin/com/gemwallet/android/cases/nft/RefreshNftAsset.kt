package com.gemwallet.android.cases.nft

import com.wallet.core.primitives.NFTAssetId
import com.wallet.core.primitives.Wallet

interface RefreshNftAsset {
    suspend fun refreshNftAsset(wallet: Wallet, assetId: NFTAssetId)
}
