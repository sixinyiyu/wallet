package com.gemwallet.android.blockchain.operators.gemstone

import com.gemwallet.android.blockchain.operators.LoadPrivateKeyOperator
import com.gemwallet.android.ext.keystoreId
import com.gemwallet.android.ext.v4KeystorePasswordBytes
import com.wallet.core.primitives.Chain
import com.wallet.core.primitives.Wallet
import uniffi.gemstone.GemKeystore

class GemLoadPrivateKeyOperator(
    private val baseDir: String,
) : LoadPrivateKeyOperator {

    override suspend fun invoke(wallet: Wallet, chain: Chain, password: String): ByteArray {
        return GemKeystore(baseDir).use { keystore ->
            keystore.privateKey(wallet.keystoreId, chain.string, password.v4KeystorePasswordBytes())
        }
    }
}
