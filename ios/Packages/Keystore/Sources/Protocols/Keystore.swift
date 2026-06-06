// Copyright (c). Gem Wallet. All rights reserved.

import Foundation
import Primitives

internal import SwiftUI

public protocol Keystore: Sendable {
    func createWallet() throws -> [String]
    func importPreview(type: KeystoreImportType) async throws -> WalletImport
    @discardableResult
    func importWallet(name: String, type: KeystoreImportType, isWalletsEmpty: Bool, source: WalletSource) async throws -> Wallet
    func setupChains(chains: [Chain], for wallets: [Wallet]) throws -> [Wallet]
    /// Migrates a v3 keystore to v4; returns the new keystore_id, or nil if already v4.
    func migrateV3Keystore(for wallet: Wallet) async throws -> String?
    func deleteKey(for wallet: Wallet) async throws
    func getPrivateKey(wallet: Wallet, chain: Chain) async throws -> Data
    func getPrivateKeyEncoded(wallet: Wallet, chain: Chain) async throws -> String
    func getMnemonic(wallet: Wallet) async throws -> [String]
    func getPasswordAuthentication() throws -> KeystoreAuthentication
    func destroy() throws
}
