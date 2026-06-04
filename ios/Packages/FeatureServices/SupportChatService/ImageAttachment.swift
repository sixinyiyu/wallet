// Copyright (c). Gem Wallet. All rights reserved.

import Foundation

public struct ImageAttachment: Sendable {
    public let data: Data
    public let fileName: String

    public init(data: Data, fileName: String) {
        self.data = data
        self.fileName = fileName
    }
}
