// Copyright (c). Gem Wallet. All rights reserved.

import Style
import SwiftUI
import UIKit

public struct CurrencyTextField: UIViewRepresentable {
    public enum CurrencyPosition {
        case leading
        case trailing
    }

    @Binding var text: String

    private let currencyPosition: CurrencyPosition
    private let currencySymbol: String
    private let placeholder: String
    private let keyboardType: UIKeyboardType
    private let sanitize: (String) -> String

    public init(
        text: Binding<String>,
        currencySymbol: String,
        currencyPosition: CurrencyPosition,
        placeholder: String,
        keyboardType: UIKeyboardType,
        sanitize: @escaping (String) -> String,
    ) {
        _text = text
        self.currencyPosition = currencyPosition
        self.currencySymbol = switch currencyPosition {
        case .leading: "\(currencySymbol) "
        case .trailing: " \(currencySymbol)"
        }
        self.placeholder = placeholder
        self.keyboardType = keyboardType
        self.sanitize = sanitize
    }

    public func makeUIView(context: Context) -> UITextField {
        let field = UITextField()
        field.delegate = context.coordinator
        field.keyboardType = keyboardType
        field.textAlignment = .center
        field.font = .systemFont(ofSize: 44, weight: .semibold)
        field.adjustsFontSizeToFitWidth = true
        field.minimumFontSize = 22
        field.attributedText = composedText
        return field
    }

    public func updateUIView(_ field: UITextField, context: Context) {
        context.coordinator.parent = self
        let composed = composedText
        guard field.attributedText?.string != composed.string else { return }
        field.attributedText = composed
        moveCaretToEnd(field)
    }

    public func sizeThatFits(_ proposal: ProposedViewSize, uiView _: UITextField, context _: Context) -> CGSize? {
        CGSize(
            width: proposal.width ?? UIView.layoutFittingExpandedSize.width,
            height: UIFont.systemFont(ofSize: 44, weight: .semibold).lineHeight,
        )
    }

    public func makeCoordinator() -> Coordinator { Coordinator(parent: self) }

    private func strip(_ composed: String) -> String {
        switch currencyPosition {
        case .leading:
            composed.hasPrefix(currencySymbol) ? String(composed.dropFirst(currencySymbol.count)) : composed
        case .trailing:
            composed.hasSuffix(currencySymbol) ? String(composed.dropLast(currencySymbol.count)) : composed
        }
    }

    private var composedText: NSAttributedString {
        let symbol = NSAttributedString(string: currencySymbol, attributes: [.foregroundColor: Colors.black.uiColor])
        let digits = NSAttributedString(string: text.isEmpty ? placeholder : text, attributes: [.foregroundColor: text.isEmpty ? .placeholderText : Colors.black.uiColor])
        let result = NSMutableAttributedString()
        switch currencyPosition {
        case .leading:
            result.append(symbol)
            result.append(digits)
        case .trailing:
            result.append(digits)
            result.append(symbol)
        }
        return result
    }

    private func moveCaretToEnd(_ field: UITextField) {
        let total = ((field.text ?? "") as NSString).length
        let offset = switch currencyPosition {
        case .leading: total
        case .trailing: total - (currencySymbol as NSString).length
        }
        guard let position = field.position(from: field.beginningOfDocument, offset: offset) else { return }
        if let current = field.selectedTextRange,
           field.compare(current.start, to: position) == .orderedSame,
           field.compare(current.end, to: position) == .orderedSame {
            return
        }
        field.selectedTextRange = field.textRange(from: position, to: position)
    }
}

public extension CurrencyTextField {
    final class Coordinator: NSObject, UITextFieldDelegate {
        var parent: CurrencyTextField

        init(parent: CurrencyTextField) { self.parent = parent }

        public func textField(
            _ field: UITextField,
            shouldChangeCharactersIn range: NSRange,
            replacementString string: String,
        ) -> Bool {
            if parent.text.isEmpty {
                let sanitized = parent.sanitize(string)
                if parent.text != sanitized { parent.text = sanitized }
                return false
            }
            let proposed = ((field.text ?? "") as NSString).replacingCharacters(in: range, with: string)
            let sanitized = parent.sanitize(parent.strip(proposed))
            if parent.text != sanitized { parent.text = sanitized }
            return false
        }

        public func textFieldDidChangeSelection(_ field: UITextField) {
            parent.moveCaretToEnd(field)
        }
    }
}

#Preview {
    @Previewable @State var textLeading = "10"
    @Previewable @State var textTrailing = "100"

    return VStack {
        CurrencyTextField(text: $textLeading, currencySymbol: "$", currencyPosition: .leading, placeholder: "0", keyboardType: .numberPad, sanitize: { $0 })
        CurrencyTextField(text: $textTrailing, currencySymbol: "$", currencyPosition: .trailing, placeholder: "0", keyboardType: .decimalPad, sanitize: { $0 })
    }
}
