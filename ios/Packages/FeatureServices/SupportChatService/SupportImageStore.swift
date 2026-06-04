// Copyright (c). Gem Wallet. All rights reserved.

import Foundation

struct SupportImageStore {
    private let directoryName = "support_uploads"
    private var directory: URL {
        get throws {
            let base = try FileManager.default.url(for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
            let directory = base.appendingPathComponent(directoryName, isDirectory: true)
            try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
            return directory
        }
    }

    func store(_ data: Data, id: String) throws -> URL {
        let url = try directory.appendingPathComponent(id)
        try data.write(to: url, options: .atomic)
        return url
    }

    func data(at url: URL) -> Data? {
        guard url.isFileURL else { return nil }
        return try? Data(contentsOf: url)
    }

    func remove(at url: URL) {
        guard url.isFileURL else { return }
        try? FileManager.default.removeItem(at: url)
    }
}
