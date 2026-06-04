// Copyright (c). Gem Wallet. All rights reserved.

import Foundation
import PhotosUI
import SwiftUI

@Observable
@MainActor
final class SupportMessageInputBarViewModel {
    var text: String = ""
    var selectedItems: [PhotosPickerItem] = []

    private let onSend: (SupportInputMessage) -> Void

    init(onSend: @escaping (SupportInputMessage) -> Void) {
        self.onSend = onSend
    }

    var placeholder: String { "Message" }

    var inputMessage: SupportInputMessage {
        SupportInputMessage(
            content: text.trimmingCharacters(in: .whitespacesAndNewlines),
            attachments: selectedItems,
        )
    }

    var canSend: Bool { !inputMessage.isEmpty }

    func send() {
        onSend(inputMessage)
        text = ""
        selectedItems = []
    }

    func removeItem(_ item: PhotosPickerItem) {
        selectedItems.removeAll { $0 == item }
    }
}
