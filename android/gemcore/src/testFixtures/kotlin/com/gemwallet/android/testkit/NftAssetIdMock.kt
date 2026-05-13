package com.gemwallet.android.testkit

import com.wallet.core.primitives.Chain
import com.wallet.core.primitives.NFTAssetId

fun mockNftAssetId(
    chain: Chain = Chain.Ethereum,
    contractAddress: String = "0xasset",
    tokenId: String = "1",
) = NFTAssetId(chain = chain, contractAddress = contractAddress, tokenId = tokenId)
