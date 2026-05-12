// Copyright (c). Gem Wallet. All rights reserved.

import Foundation
import Primitives

public enum TransactionHeaderAction: Equatable, Sendable {
    case url(URL)
    case nft(assetId: NFTAssetId)
}
