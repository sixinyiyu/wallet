// Copyright (c). Gem Wallet. All rights reserved.

import Components
import Formatters
import Primitives
import SwiftUI

struct FiatCurrencyInputConfig: CurrencyInputConfigurable {
    var secondaryText: String
    let currencyFormatter: CurrencyFormatter

    var currencySymbol: String {
        currencyFormatter.symbol
    }

    var currencyPosition: CurrencyTextField.CurrencyPosition {
        switch currencyFormatter.symbolPosition {
        case .leading: .leading
        case .trailing: .trailing
        }
    }

    var placeholder: String {
        .zero
    }

    var keyboardType: UIKeyboardType {
        .decimalPad
    }

    func sanitize(_ raw: String) -> String {
        var filtered = raw.filter { "0123456789".contains($0) }
        while filtered.first == "0", filtered.count == 1 {
            filtered.removeFirst()
        }
        return filtered
    }

    var actionStyle: CurrencyInputActionStyle?
    let onTapActionButton: VoidAction = nil
}
