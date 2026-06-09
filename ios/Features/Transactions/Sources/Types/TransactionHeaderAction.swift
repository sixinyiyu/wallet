// Copyright (c). Gem Wallet. All rights reserved.

import Primitives

public enum TransactionHeaderAction: Equatable, Sendable {
    case asset(assetId: AssetId)
    case perpetual(assetId: AssetId)
    case swap(fromAssetId: AssetId, toAssetId: AssetId)
}