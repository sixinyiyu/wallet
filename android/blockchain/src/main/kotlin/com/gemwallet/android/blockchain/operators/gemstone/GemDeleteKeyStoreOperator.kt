package com.gemwallet.android.blockchain.operators.gemstone

import com.gemwallet.android.application.PasswordStore
import com.gemwallet.android.blockchain.operators.DeleteKeyStoreOperator
import com.gemwallet.android.ext.keystoreId
import com.wallet.core.primitives.Wallet
import uniffi.gemstone.GemKeystore
import java.io.File

class GemDeleteKeyStoreOperator(
    private val baseDir: String,
    private val passwordStore: PasswordStore,
) : DeleteKeyStoreOperator {

    override fun invoke(wallet: Wallet): Boolean {
        runCatching {
            GemKeystore(baseDir).use { keystore ->
                keystore.delete(wallet.keystoreId)
            }
        }
        runCatching {
            val legacyFile = File(baseDir, wallet.id.id)
            if (legacyFile.exists()) {
                legacyFile.delete()
            }
        }
        passwordStore.removePassword(wallet.id.id)
        return true
    }
}
