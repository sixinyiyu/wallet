// Copyright (c). Gem Wallet. All rights reserved.

import Foundation
import PhotosUI
import SwiftUI

struct SupportInputMessage {
    let content: String
    let attachments: [PhotosPickerItem]

    var isEmpty: Bool {
        content.isEmpty && attachments.isEmpty
    }
}
