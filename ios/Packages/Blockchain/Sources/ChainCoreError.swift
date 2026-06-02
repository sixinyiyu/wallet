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
    case insufficientBalance

    static func fromWalletCore(_ error: CommonSigningError) throws {
        let chainError: ChainCoreError? = switch error {
        case .errorDustAmountRequested,
             .errorNotEnoughUtxos,
             .errorMissingInputUtxos: ChainCoreError.dustThreshold
        case .ok: .none
        default: ChainCoreError.cantEstimateFee
        }

        if let error = chainError {
            throw error
        }
    }

    public static func fromError(_ error: Error) -> ChainCoreError? {
        let description = error.localizedDescription
        if description.contains("dust threshold") {
            return .dustThreshold
        }
        if description.contains("insufficient balance") {
            return .insufficientBalance
        }
        for errorCase in [ChainCoreError.dustThreshold, .feeRateMissed, .cantEstimateFee, .incorrectAmount, .insufficientBalance] {
            if description.contains(errorCase.rawValue) {
                return errorCase
            }
        }

        return nil
    }
}
