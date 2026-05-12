package com.gemwallet.android.testkit

import com.wallet.core.primitives.Chain
import com.wallet.core.primitives.NFTCollectionId

fun mockNftCollectionId(
    chain: Chain = Chain.Ethereum,
    contractAddress: String = "0xcollection",
) = NFTCollectionId(chain = chain, contractAddress = contractAddress)
