package com.gemwallet.android.ext

import com.wallet.core.primitives.NFTCollectionId

fun NFTCollectionId.toIdentifier(): String = "${chain.string}_$contractAddress"

fun String.toNftCollectionId(): NFTCollectionId? {
    val assetId = toAssetId() ?: return null
    val contractAddress = assetId.tokenId ?: return null
    return NFTCollectionId(chain = assetId.chain, contractAddress = contractAddress)
}
