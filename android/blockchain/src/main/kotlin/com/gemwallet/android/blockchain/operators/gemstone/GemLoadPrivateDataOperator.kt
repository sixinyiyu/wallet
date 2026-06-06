package com.gemwallet.android.blockchain.operators.gemstone

import com.gemwallet.android.blockchain.operators.LoadPrivateDataOperator
import com.gemwallet.android.ext.keystoreId
import com.gemwallet.android.ext.v4KeystorePasswordBytes
import com.wallet.core.primitives.Wallet
import com.wallet.core.primitives.WalletType
import uniffi.gemstone.GemKeystore

class GemLoadPrivateDataOperator(
    private val baseDir: String,
) : LoadPrivateDataOperator {

    override suspend fun invoke(wallet: Wallet, password: String): String {
        return GemKeystore(baseDir).use { keystore ->
            val passwordBytes = password.v4KeystorePasswordBytes()
            if (wallet.type == WalletType.PrivateKey) {
                val chain = wallet.accounts.firstOrNull()?.chain
                    ?: throw IllegalStateException("No accounts found for wallet ${wallet.id.id}")
                keystore.exportPrivateKey(wallet.keystoreId, chain.string, passwordBytes)
            } else {
                keystore.exportRecoveryPhrase(wallet.keystoreId, passwordBytes).joinToString(" ")
            }
        }
    }
}
