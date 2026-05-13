// Copyright (c). Gem Wallet. All rights reserved.

import Foundation
import Primitives

public extension NFTAssetId {
    static func mock(
        chain: Chain = .mock(),
        contractAddress: String = "0xcontract",
        tokenId: String = "1",
    ) -> NFTAssetId {
        NFTAssetId(chain: chain, contractAddress: contractAddress, tokenId: tokenId)
    }
}
