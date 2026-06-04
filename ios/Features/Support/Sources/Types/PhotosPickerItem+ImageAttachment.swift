// Copyright (c). Gem Wallet. All rights reserved.

import PhotosUI
import SupportChatService
import SwiftUI

extension PhotosPickerItem {
    func imageAttachment() async throws -> ImageAttachment? {
        guard let data = try await loadTransferable(type: Data.self) else { return nil }
        let utType = supportedContentTypes.first { $0.conforms(to: .image) } ?? .jpeg
        let fileExtension = utType.preferredFilenameExtension ?? "jpg"
        return ImageAttachment(data: data, fileName: "image-\(UUID().uuidString).\(fileExtension)")
    }
}
