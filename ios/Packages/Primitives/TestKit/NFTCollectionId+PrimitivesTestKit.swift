// Copyright (c). Gem Wallet. All rights reserved.

import Foundation
import Primitives

public extension NFTCollectionId {
    static func mock(
        chain: Chain = .mock(),
        contractAddress: String = "0xcontract",
    ) -> NFTCollectionId {
        NFTCollectionId(chain: chain, contractAddress: contractAddress)
    }
}
