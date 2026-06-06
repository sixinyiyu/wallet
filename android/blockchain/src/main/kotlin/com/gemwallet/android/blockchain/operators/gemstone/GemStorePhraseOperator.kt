package com.gemwallet.android.blockchain.operators.gemstone

import com.gemwallet.android.blockchain.operators.StorePhraseOperator
import com.gemwallet.android.blockchain.operators.StoredWalletSecret
import com.gemwallet.android.ext.v4KeystorePasswordBytes
import com.gemwallet.android.ext.words
import com.wallet.core.primitives.Wallet
import com.wallet.core.primitives.WalletType
import uniffi.gemstone.GemImportType
import uniffi.gemstone.GemKeystore

class GemStorePhraseOperator(
    private val baseDir: String,
) : StorePhraseOperator {

    override suspend fun invoke(wallet: Wallet, data: String, password: String): Result<StoredWalletSecret> = runCatching {
        val walletImport = when (wallet.type) {
            WalletType.Multicoin -> GemImportType.MulticoinPhrase(
                words = data.words(),
                chains = wallet.accounts.map { it.chain.string },
            )
            WalletType.Single -> GemImportType.SinglePhrase(
                words = data.words(),
                chain = wallet.accounts.first().chain.string,
            )
            WalletType.PrivateKey -> GemImportType.PrivateKey(
                value = data,
                chain = wallet.accounts.first().chain.string,
            )
            WalletType.View -> throw IllegalArgumentException("View wallets do not store secrets")
        }

        GemKeystore(baseDir).use { keystore ->
            val stored = keystore.createWallet(walletImport, password.v4KeystorePasswordBytes())
            require(stored.walletId == wallet.id.id) {
                "Stored wallet id ${stored.walletId} does not match ${wallet.id.id}"
            }
            StoredWalletSecret(stored.keystoreId)
        }
    }
}
