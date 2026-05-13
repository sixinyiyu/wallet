// Copyright (c). Gem Wallet. All rights reserved.

import Primitives

public enum TransactionHeaderTap: Equatable, Sendable {
    case header
    case asset(AssetId)
}

public typealias TransactionHeaderActionHandler = (TransactionHeaderTap) -> Void
