package com.gemwallet.android.blockchain.operators.gemstone

import com.gemwallet.android.blockchain.operators.AddAccountsOperator
import com.gemwallet.android.ext.keystoreId
import com.gemwallet.android.ext.v4KeystorePasswordBytes
import com.wallet.core.primitives.Account
import com.wallet.core.primitives.Chain
import com.wallet.core.primitives.Wallet
import uniffi.gemstone.GemKeystore

class GemAddAccountsOperator(
    private val baseDir: String,
) : AddAccountsOperator {

    override suspend fun invoke(wallet: Wallet, chains: List<Chain>, password: String): List<Account> =
        GemKeystore(baseDir).use { keystore ->
            keystore.addAccounts(wallet.keystoreId, password.v4KeystorePasswordBytes(), chains.map { it.string })
                .map { it.toAccount() }
        }
}
