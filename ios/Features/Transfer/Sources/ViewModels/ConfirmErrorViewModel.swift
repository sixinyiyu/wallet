// Copyright (c). Gem Wallet. All rights reserved.

import Blockchain
import Components
import Localization
import Primitives

struct ConfirmErrorViewModel {
    private let state: StateViewType<TransactionInputViewModel>
    private let onSelectListError: (Error) -> Void

    init(
        state: StateViewType<TransactionInputViewModel>,
        onSelectListError: @escaping (Error) -> Void,
    ) {
        self.state = state
        self.onSelectListError = onSelectListError
    }
}

// MARK: - ItemModelProvidable

extension ConfirmErrorViewModel: ItemModelProvidable {
    var itemModel: ConfirmTransferItemModel {
        guard let error = listError else { return .empty }
        return .error(
            title: Localized.Errors.errorOccured,
            error: ChainCoreError.fromError(error) ?? error,
            onInfoAction: infoAction(for: error),
        )
    }
}

// MARK: - Private

extension ConfirmErrorViewModel {
    private var listError: Error? {
        if case let .error(error) = state { return error }
        if case let .failure(error) = state.value?.transferAmount { return error }
        return nil
    }

    private func infoAction(for error: Error) -> VoidAction {
        switch ChainCoreError.fromError(error) {
        case .dustChange: nil
        case .dustThreshold, .feeRateMissed, .cantEstimateFee, .incorrectAmount, .none: { onSelectListError(error) }
        }
    }
}
