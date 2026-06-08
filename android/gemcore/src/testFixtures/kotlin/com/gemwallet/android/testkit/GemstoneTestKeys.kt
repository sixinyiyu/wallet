package com.gemwallet.android.testkit

import android.content.Context
import com.gemwallet.android.ext.words
import com.wallet.core.primitives.Account
import com.wallet.core.primitives.Chain
import uniffi.gemstone.GemImportType
import uniffi.gemstone.GemKeystore
import uniffi.gemstone.GemKeystoreAccount
import java.io.File

fun includeGemstoneLibs() {
    System.loadLibrary("gemstone")
}

fun gemstoneTestAccount(context: Context, chain: Chain, phrase: String): Account {
    return GemKeystore(gemstoneTestBaseDir(context)).use { keystore ->
        keystore.importWallet(
            GemImportType.MulticoinPhrase(
                words = phrase.words(),
                chains = listOf(chain.string),
            )
        ).accounts.first().toAccount()
    }
}

fun gemstoneTestAddressForPrivateKey(context: Context, chain: Chain, value: String): String {
    return GemKeystore(gemstoneTestBaseDir(context)).use { keystore ->
        keystore.importWallet(GemImportType.PrivateKey(value = value, chain = chain.string)).accounts.first().address
    }
}

private fun gemstoneTestBaseDir(context: Context): String {
    val dir = File(context.cacheDir, "gemstone-test-keystore")
    dir.mkdirs()
    return dir.absolutePath
}

private fun GemKeystoreAccount.toAccount(): Account {
    return Account(
        chain = chain.toPrimitiveChain(),
        address = address,
        derivationPath = derivationPath,
        extendedPublicKey = publicKey.orEmpty(),
    )
}

private fun String.toPrimitiveChain(): Chain {
    return Chain.entries.firstOrNull { it.string == this }
        ?: throw IllegalArgumentException("Unsupported chain: $this")
}
