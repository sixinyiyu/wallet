// Copyright (c). Gem Wallet. All rights reserved.

import Localization
@testable import Primitives
import PrimitivesTestKit
import Testing
@testable import Transfer
import TransferTestKit

struct ConfirmButtonViewModelTests {
    @Test
    func titles() {
        let confirm = ConfirmButtonViewModel(state: .data(TransactionInputViewModel.mock()), icon: nil, onAction: { _ in })
        let tryAgain = ConfirmButtonViewModel(state: .error(AnyError("test")), icon: nil, onAction: { _ in })
        #expect(confirm.title == Localized.Transfer.confirm)
        #expect(tryAgain.title == Localized.Common.tryAgain)
    }

    @Test
    func disabled() {
        let forced = ConfirmButtonViewModel(state: .data(TransactionInputViewModel.mock()), icon: nil, isDisabled: true, onAction: { _ in })
        let enabled = ConfirmButtonViewModel(state: .data(TransactionInputViewModel.mock()), icon: nil, isDisabled: false, onAction: { _ in })
        #expect(forced.type.isDisabled)
        #expect(!enabled.type.isDisabled)
    }
}
