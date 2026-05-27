// Copyright (c). Gem Wallet. All rights reserved.

import Blockchain
import Components
import Localization
import Primitives
import Style
import SwiftUI

struct ConfirmButtonViewModel: StateButtonViewable {
    enum Mode {
        case confirm
        case tryAgain
        case sendMax
    }

    private let state: StateViewType<TransactionInputViewModel>
    private let isDisabled: Bool
    private let onAction: @MainActor @Sendable (Mode) -> Void

    let icon: Image?

    init(
        state: StateViewType<TransactionInputViewModel>,
        icon: Image?,
        isDisabled: Bool = false,
        onAction: @MainActor @Sendable @escaping (Mode) -> Void,
    ) {
        self.state = state
        self.icon = icon
        self.isDisabled = isDisabled
        self.onAction = onAction
    }

    var mode: Mode {
        if case let .data(data) = state, data.isReady { return .confirm }
        if case let .error(error) = state, ChainCoreError.fromError(error) == .dustChange { return .sendMax }
        return .tryAgain
    }

    var title: String {
        switch mode {
        case .confirm: Localized.Transfer.confirm
        case .tryAgain: Localized.Common.tryAgain
        case .sendMax: Localized.Transfer.max
        }
    }

    var type: ButtonType {
        let isDisabled = isDisabled || (state.value?.transferAmount?.isFailure ?? false)
        return .primary(state, isDisabled: isDisabled)
    }

    func action() {
        onAction(mode)
    }
}
