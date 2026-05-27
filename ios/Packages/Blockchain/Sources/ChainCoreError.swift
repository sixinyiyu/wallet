// Copyright (c). Gem Wallet. All rights reserved.

import Foundation
import Gemstone
import Primitives
import WalletCore

public enum ChainCoreError: String, Error, Equatable {
    case feeRateMissed
    case cantEstimateFee
    case incorrectAmount
    case dustThreshold
    case dustChange

    static func fromWalletCore(_ error: CommonSigningError) throws {
        let chainError: ChainCoreError? = switch error {
        case .errorDustAmountRequested: ChainCoreError.dustThreshold
        case .errorNotEnoughUtxos: ChainCoreError.dustChange
        case .errorMissingInputUtxos: ChainCoreError.cantEstimateFee
        case .ok: .none
        default: ChainCoreError.cantEstimateFee
        }

        if let error = chainError {
            throw error
        }
    }

    public static func fromError(_ error: Error) -> ChainCoreError? {
        if let chainError = error as? ChainCoreError {
            return chainError
        }
        if let gateway = error as? Gemstone.GatewayError, case let .PlatformError(msg) = gateway {
            return ChainCoreError(rawValue: msg)
        }
        return nil
    }
}
