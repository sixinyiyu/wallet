package com.gemwallet.android.ext

import com.wallet.core.primitives.NFTAssetId

private const val TOKEN_ID_SEPARATOR = "::"

fun NFTAssetId.toIdentifier(): String =
    "${chain.string}_$contractAddress$TOKEN_ID_SEPARATOR$tokenId"

fun String.toNftAssetId(): NFTAssetId? {
    val assetId = toAssetId() ?: return null
    val rest = assetId.tokenId ?: return null
    val tokenStart = rest.indexOf(TOKEN_ID_SEPARATOR).takeIf { it >= 0 } ?: return null
    return NFTAssetId(
        chain = assetId.chain,
        contractAddress = rest.substring(0, tokenStart),
        tokenId = rest.substring(tokenStart + TOKEN_ID_SEPARATOR.length),
    )
}
