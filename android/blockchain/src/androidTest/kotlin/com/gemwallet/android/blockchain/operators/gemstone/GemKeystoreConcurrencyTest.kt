package com.gemwallet.android.blockchain.operators.gemstone

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gemwallet.android.math.fromHex
import com.gemwallet.android.testkit.includeGemstoneLibs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uniffi.gemstone.GemImportType
import uniffi.gemstone.GemKeystore
import java.io.File
import java.util.concurrent.CyclicBarrier

/**
 * The Rust keystore serializes every file operation through a process-global queue. Android operators
 * create a fresh GemKeystore(baseDir) per call, so this proves the guarantee holds across separate
 * instances and threads through the UniFFI binding: concurrent create/read/delete on one wallet never
 * corrupts the file or produces duplicates, and create stays idempotent.
 */
@RunWith(AndroidJUnit4::class)
class GemKeystoreConcurrencyTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var baseDir: File

    @Before
    fun setUp() {
        baseDir = File(context.cacheDir, "gemk-concurrency").apply { deleteRecursively(); mkdirs() }
    }

    @After
    fun tearDown() {
        baseDir.deleteRecursively()
    }

    @Test
    fun concurrentCreateReadDelete_acrossInstancesAndThreads_isThreadSafe() = runBlocking {
        val password = PASSWORD_HEX.fromHex()
        val import = GemImportType.SinglePhrase(words = PHRASE.split(" "), chain = "ethereum")

        // 1) Same wallet created concurrently from separate GemKeystore instances, all released at once.
        val startCreate = CyclicBarrier(THREADS)
        val created = (0 until THREADS).map {
            async(Dispatchers.IO) {
                startCreate.await()
                GemKeystore(baseDir.path).use { it.createWallet(import, password) }
            }
        }.awaitAll()

        val keystoreId = created.first().keystoreId
        assertTrue("concurrent creates of the same wallet must return one deterministic id", created.all { it.keystoreId == keystoreId })

        val files = File(baseDir, "v4").listFiles { file -> file.extension == "gemk" }.orEmpty()
        assertEquals("the race must leave exactly one keystore file, not duplicates", 1, files.size)

        // 2) Concurrent reads must all decrypt to the original phrase (no torn reads).
        val startRead = CyclicBarrier(THREADS)
        val phrases = (0 until THREADS).map {
            async(Dispatchers.IO) {
                startRead.await()
                GemKeystore(baseDir.path).use { it.exportRecoveryPhrase(keystoreId, password).joinToString(" ") }
            }
        }.awaitAll()
        assertTrue("every concurrent read must return the stored phrase", phrases.all { it == PHRASE })

        // 3) Concurrent deletes must not crash; the file is gone afterward.
        val startDelete = CyclicBarrier(THREADS)
        (0 until THREADS).map {
            async(Dispatchers.IO) {
                startDelete.await()
                GemKeystore(baseDir.path).use { runCatching { it.delete(keystoreId) } }
            }
        }.awaitAll()
        assertFalse("keystore file must be removed after concurrent delete", File(baseDir, "v4/$keystoreId.gemk").exists())
    }

    companion object {
        init {
            includeGemstoneLibs()
        }

        private const val THREADS = 8
        private const val PHRASE = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        // 32-byte v4 password (raw bytes), as decoded hex.
        private const val PASSWORD_HEX = "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
    }
}
