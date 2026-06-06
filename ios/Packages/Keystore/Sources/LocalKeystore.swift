import Foundation
import GemstonePrimitives
import Primitives

internal import Gemstone

public final class LocalKeystore: Keystore, @unchecked Sendable {
    private let gemKeystore: GemKeystore
    private let keystoreURL: URL
    private let keystorePassword: KeystorePassword
    private let queue = DispatchQueue(label: "com.gemwallet.keystore", qos: .userInitiated)

    public init(
        directory: String = "keystore",
        keystorePassword: KeystorePassword = LocalKeystorePassword(),
    ) {
        do {
            // migrate keystore from documents directory to applocation support directory
            // TODO: delete in 2026
            let fileMigrator = FileMigrator()
            let keystoreURL = try fileMigrator.migrate(
                name: directory,
                fromDirectory: .documentDirectory,
                toDirectory: .applicationSupportDirectory,
                isDirectory: true,
            )
            self.keystoreURL = keystoreURL
            gemKeystore = try GemKeystore(baseDir: keystoreURL.path)
        } catch {
            fatalError("keystore initialization error: \(error)")
        }

        self.keystorePassword = keystorePassword
    }

    public func createWallet() throws -> [String] {
        try Mnemonic.generateWords(wordCount: 12)
    }

    public func importPreview(type: KeystoreImportType) async throws -> WalletImport {
        if case let .address(address, chain) = type {
            return WalletImport(
                walletId: .view(chain: chain, address: address),
                walletType: .view,
                accounts: [
                    Account(
                        chain: chain,
                        address: address,
                        derivationPath: .empty,
                        extendedPublicKey: "",
                    ),
                ],
            )
        }
        guard let importType = type.gemWalletImport else {
            throw AnyError("Unsupported keystore import type")
        }
        return try await queue.asyncTask { [gemKeystore] in
            try gemKeystore.importWallet(import: importType).mapToPreview()
        }
    }

    public func importWallet(
        name: String,
        type: KeystoreImportType,
        isWalletsEmpty: Bool,
        source: WalletSource,
    ) async throws -> Primitives.Wallet {
        let password = try await getOrCreatePassword(createPasswordIfNone: isWalletsEmpty)

        return try await queue.asyncTask { [gemKeystore] in
            switch type {
            case let .address(address, chain):
                return Wallet.makeView(name: name, chain: chain, address: address)
            case .phrase,
                 .single,
                 .privateKey:
                guard let importType = type.gemWalletImport else {
                    throw AnyError("Unsupported keystore import type")
                }
                return try withV4Password(password) { passwordBytes in
                    try gemKeystore
                        .createWallet(import: importType, password: passwordBytes)
                        .mapToWallet(name: name, source: source)
                }
            }
        }
    }

    public func setupChains(chains: [Primitives.Chain], for wallets: [Primitives.Wallet]) throws -> [Primitives.Wallet] {
        let filteredWallets = wallets.filter {
            let enabled = Set($0.accounts.map(\.chain)).intersection(chains).map(\.self)
            let missing = Set(chains).subtracting(enabled)
            return missing.isNotEmpty
        }
        guard filteredWallets.isNotEmpty else {
            return []
        }
        let password = try keystorePassword.getPassword()

        return try filteredWallets
            .prefix(25)
            .compactMap { wallet -> Primitives.Wallet? in
                let existingChains = wallet.accounts.map(\.chain)
                let newChains = chains.asSet().subtracting(existingChains.asSet()).asArray()
                // Pre-v4 wallets are skipped; the startup migration writes the v4 file first.
                guard v4KeystoreExists(wallet.keystoreId) else {
                    return nil
                }
                let accounts = try withV4Password(password) { passwordBytes in
                    try queue.sync {
                        try gemKeystore.addAccounts(
                            keystoreId: wallet.keystoreId,
                            password: passwordBytes,
                            chains: newChains.map(\.rawValue),
                        )
                    }
                }
                return try wallet.adding(accounts: accounts.map { try $0.mapToAccount() })
            }
    }

    public func migrateV3Keystore(for wallet: Primitives.Wallet) async throws -> String? {
        switch wallet.type {
        case .view:
            return nil
        case .multicoin, .single, .privateKey:
            let password = try await getPassword()
            guard !password.isEmpty else { return nil }
            return try await queue.asyncTask { [gemKeystore, keystoreURL] in
                // Filesystem is the migration state: a legacy file keyed by the pre-v4 id means not-yet-migrated.
                guard let v3URL = Self.findV3File(in: keystoreURL, matching: wallet.legacyV3Id) else {
                    return nil
                }
                // iOS v3 password = UTF-8 of the hex string; v4 = decoded bytes. Deterministic id, idempotent re-run.
                var v3Password = password.v3PasswordBytes()
                var newPassword = try password.v4KeystorePasswordBytes()
                defer {
                    v3Password.zeroize()
                    newPassword.zeroize()
                }
                let migration = try gemKeystore.migrateV3(
                    v3Path: v3URL.path,
                    v3Password: v3Password,
                    newPassword: newPassword,
                    keystoreId: wallet.keystoreId,
                )
                // Drop the legacy file only after the v4 write succeeds; the deterministic id makes a crash here re-runnable.
                Self.moveV3FileToBackup(v3URL, in: keystoreURL)
                return migration.keystoreId
            }
        }
    }

    public func deleteKey(for wallet: Primitives.Wallet) async throws {
        switch wallet.type {
        case .view: break
        case .multicoin, .single, .privateKey:
            try await queue.asyncTask { [gemKeystore, keystoreURL] in
                // Delete returns false when no v4 file exists (a wallet that never migrated).
                if try gemKeystore.delete(keystoreId: wallet.keystoreId) {
                    return
                }
                // Fall back to the legacy v3 file, keyed by the pre-v4 id.
                guard let legacyURL = Self.findV3File(in: keystoreURL, matching: wallet.legacyV3Id) else {
                    return
                }
                try FileManager.default.removeItem(at: legacyURL)
            }
        }
    }

    public func getPrivateKey(wallet: Primitives.Wallet, chain: Primitives.Chain) async throws -> Data {
        let password = try await getPassword()
        return try await queue.asyncTask { [gemKeystore] in
            try withV4Password(password) { passwordBytes in
                try gemKeystore.privateKey(
                    keystoreId: wallet.keystoreId,
                    chain: chain.rawValue,
                    password: passwordBytes,
                )
            }
        }
    }

    public func getPrivateKeyEncoded(wallet: Primitives.Wallet, chain: Primitives.Chain) async throws -> String {
        let password = try await getPassword()
        return try await queue.asyncTask { [gemKeystore] in
            try withV4Password(password) { passwordBytes in
                try gemKeystore.exportPrivateKey(
                    keystoreId: wallet.keystoreId,
                    chain: chain.rawValue,
                    password: passwordBytes,
                )
            }
        }
    }

    public func getMnemonic(wallet: Primitives.Wallet) async throws -> [String] {
        let password = try await getPassword()
        return try await queue.asyncTask { [gemKeystore] in
            try withV4Password(password) { passwordBytes in
                try gemKeystore.exportRecoveryPhrase(
                    keystoreId: wallet.keystoreId,
                    password: passwordBytes,
                )
            }
        }
    }

    public func getPasswordAuthentication() throws -> KeystoreAuthentication {
        try keystorePassword.getAuthentication()
    }

    public func destroy() throws {
        guard FileManager.default.fileExists(atPath: keystoreURL.path) else {
            return
        }
        try FileManager.default.removeItem(at: keystoreURL)
    }

    @MainActor
    private func getPassword() throws -> String {
        try keystorePassword.getPassword()
    }

    private func v4KeystoreExists(_ keystoreId: String) -> Bool {
        let url = keystoreURL.appendingPathComponent("v4").appendingPathComponent("\(keystoreId).gemk")
        return FileManager.default.fileExists(atPath: url.path)
    }

    /// Legacy v3 files sit at the root of keystoreURL (v4/ holds new files); match filename then JSON id.
    private static func findV3File(in directory: URL, matching keystoreId: String) -> URL? {
        let target = keystoreId.lowercased()
        let fileManager = FileManager.default
        guard let contents = try? fileManager.contentsOfDirectory(
            at: directory,
            includingPropertiesForKeys: [.isDirectoryKey],
        ) else {
            return nil
        }
        for url in contents {
            let isDirectory = (try? url.resourceValues(forKeys: [.isDirectoryKey]))?.isDirectory ?? false
            if isDirectory { continue }
            let name = url.lastPathComponent.lowercased()
            if name == target || name.hasSuffix(target) {
                return url
            }
            if let data = try? Data(contentsOf: url),
               let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let fileId = (json["id"] as? String)?.lowercased(),
               fileId == target
            {
                return url
            }
        }
        return nil
    }

    private static func moveV3FileToBackup(_ fileURL: URL, in directory: URL) {
        let backupDirectory = directory.appendingPathComponent("v3_migrated", isDirectory: true)
        let fileManager = FileManager.default
        try? fileManager.createDirectory(at: backupDirectory, withIntermediateDirectories: true)
        let target = backupDirectory.appendingPathComponent(fileURL.lastPathComponent)
        try? fileManager.removeItem(at: target)
        if (try? fileManager.moveItem(at: fileURL, to: target)) == nil {
            try? fileManager.removeItem(at: fileURL)
        }
    }

    @MainActor
    private func getOrCreatePassword(createPasswordIfNone: Bool) throws -> String {
        let password = try keystorePassword.getPassword()

        guard password.isEmpty, createPasswordIfNone else {
            return password
        }
        let newPassword = try SecureRandom.generateKey(length: 32).hex
        try keystorePassword.setPassword(newPassword, authentication: .none)
        return newPassword
    }
}

private func withV4Password<T>(
    _ password: String,
    _ operation: (Data) throws -> T,
) throws -> T {
    var passwordBytes = try password.v4KeystorePasswordBytes()
    defer { passwordBytes.zeroize() }
    return try operation(passwordBytes)
}
