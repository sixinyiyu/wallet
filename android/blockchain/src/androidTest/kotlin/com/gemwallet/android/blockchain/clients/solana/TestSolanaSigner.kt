package com.gemwallet.android.blockchain.clients.solana

import uniffi.gemstone.GemTransactionLoadMetadata

import androidx.test.core.app.ApplicationProvider
import com.gemwallet.android.blockchain.services.SignService
import com.gemwallet.android.ext.asset
import com.gemwallet.android.math.append0x
import com.gemwallet.android.math.hex
import com.gemwallet.android.model.ConfirmParams
import com.gemwallet.android.model.DestinationAddress
import com.gemwallet.android.model.Fee
import com.gemwallet.android.testkit.TEST_PHRASE
import com.gemwallet.android.testkit.gemstoneTestAccount
import com.gemwallet.android.testkit.gemstoneTestPrivateKey
import com.gemwallet.android.testkit.includeGemstoneLibs
import com.wallet.core.primitives.Account
import com.wallet.core.primitives.Asset
import com.wallet.core.primitives.AssetId
import com.wallet.core.primitives.AssetType
import com.wallet.core.primitives.Chain
import com.wallet.core.primitives.FeePriority
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.math.BigInteger

class TestSolanaSigner {
    companion object {
        init {
            includeGemstoneLibs()
        }
    }

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val privateKey = gemstoneTestPrivateKey(context, Chain.Solana, TEST_PHRASE)
    val senderAddress = gemstoneTestAccount(context, Chain.Solana, TEST_PHRASE).address

    @Test
    fun testSolana_sign_message() {
        val message = "hello".toByteArray()
        val expected = "2JsztDwuNN4zdm6FzjXmJo3644YCnPJWgCBKhLL5tjsY8vw51otaY2NaP9YvfUsy8DmnJrAM6uvHfMsrVthK5PLc"

        val result = runBlocking {
            SignService().signMessage(Chain.Solana, message, privateKey)
        }

        assertEquals(expected, result.decodeToString())
    }

    @Test
    fun testSolana_native_transfer() {
        val signer = SignService()
        val params = ConfirmParams.Builder(
            asset = Chain.Solana.asset(),
            from = Account(Chain.Solana, senderAddress, ""),
            amount = BigInteger.valueOf(10_000_000)
        )
            .transfer(destination = DestinationAddress("4Yu2e1Wz5T1Ci2hAPswDqvMgSnJ1Ftw7ZZh8x7xKLx7S")) as ConfirmParams.TransferParams.Native
        val metadata = GemTransactionLoadMetadata.Solana(
            blockHash = "kiEPF6aKvEsj5nbi4FBvgRRm9ha36Y3cgDU9qnUKt32",
            recipientTokenAddress = null,
            senderTokenAddress = "",
            tokenProgram = uniffi.gemstone.SolanaTokenProgramId.TOKEN,
            nft = null
        )
        val result = runBlocking {
            signer.signNativeTransfer(
                params,
                metadata,
                BigInteger.ZERO,
                Fee.Solana(
                    amount = BigInteger("105005000"),
                    minerFee = BigInteger("1050000000"),
                    maxGasPrice = BigInteger("5000"),
                    unitFee = BigInteger("5000"),
                    limit = BigInteger("100000"),
                    feeAssetId = AssetId(Chain.Solana),
                    priority = FeePriority.Normal,
                    options = emptyMap(),
                ),
                privateKey
            )
        }
        assertEquals(
            "0x415771316f41593841613775302f4e6c6544385a4e68486b42305a34374e565934412b73324b7665" +
                    "3238513432564b696641442b39494a544a7147623852502f2b70514e555462716f57676835383854" +
                    "79306f7257514942414149444e4c376b396a6e634255344677426f4e4757727462626164567554716b" +
                    "677338416930764456764f63366b44426b5a76355345584d762f73726270797735766e76497a6c75" +
                    "385833456d7373513573365141414141414141414141414141414141414141414141414141414141" +
                    "4141414141414141414141414141414141414141414141437a4b594e457067635849545235376f61" +
                    "315859525157494d584843362f6b71576f74676f6d4179536b45444151414a413467544141414141" +
                    "414141415141464171434741514143416741414441494141414141414141414141414141413d3d", result.first().hex.append0x()
        )
    }

    @Test
    fun testSolana_token_transfer() {
        val signer = SignService()
        val params = ConfirmParams.Builder(
            asset = Asset(
                AssetId(Chain.Solana, "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB"),
                "",
                "",
                8,
                AssetType.SPL
            ),
            from = Account(Chain.Solana, senderAddress, ""),
            amount = BigInteger.valueOf(10_000_000)
        )
            .transfer(destination = DestinationAddress("AGkXQZ9qm99xukisDUHvspWHESrcjs8Y4AmQQgef3BRh")) as ConfirmParams.TransferParams.Token
        val metadata = GemTransactionLoadMetadata.Solana(
            blockHash = "kiEPF6aKvEsj5nbi4FBvgRRm9ha36Y3cgDU9qnUKt32",
            recipientTokenAddress = "DVWPV7brSbPDkA7a3qdn6UJsVc3J3DyhQhjNaZeZqwzo",
            senderTokenAddress = "DVWPV7brSbPDkA7a3qdn6UJsVc3J3DyhQhjNaZeZqwzo",
            tokenProgram = uniffi.gemstone.SolanaTokenProgramId.TOKEN,
            nft = null
        )
        val result = runBlocking {
            signer.signTokenTransfer(
                params,
                metadata,
                BigInteger.ZERO,
                Fee.Solana(
                    amount = BigInteger("105005000"),
                    minerFee = BigInteger("1050000000"),
                    maxGasPrice = BigInteger("5000"),
                    unitFee = BigInteger("5000"),
                    limit = BigInteger("100000"),
                    feeAssetId = AssetId(Chain.Solana),
                    priority = FeePriority.Normal,
                    options = emptyMap(),
                ),
                privateKey
            )
        }
        assertEquals(
            "0x41553247526b432f794948526c5a356461667335765242386d376c4431485a584638303341485034" +
                    "4f676448475373334e34416b37794f7048503174366964777a5a672b5a56435a3856566d73717251" +
                    "53356d467767734241414d464e4c376b396a6e634255344677426f4e475772746262616456755471" +
                    "6b677338416930764456764f63366d356d6277492b744e3454586473757149654b725254647a734b" +
                    "644444707a385843635179334a766a50304d3442446d43763762496e4637316a4753395546466f2f" +
                    "6c6c6f7a75344c5378774b657373346549494a6b41775a47622b5568467a4c2f374b323663734f62" +
                    "3537794d3562764639784a724c454f624f6b41414141414733666268313257686b396e4c3455624f" +
                    "36336d73484c5346375639624e3545366a50574666763841715173796d44524b5948467945306565" +
                    "364774563245554669444678777576354b6c714c594b4a674d6b704241774d4143514f4945774141" +
                    "4141414141414d4142514b676867454142415142416745414367774141414141414141414141673d", result.first().hex.append0x()
        )
    }

    @Test
    fun testSolana_token2022_transfer() {
        val signer = SignService()
        val params = ConfirmParams.Builder(
            asset = Asset(
                AssetId(Chain.Solana, "2b1kV6DkPAnxd5ixfnxCpjxmKwqjjaYmCZfHsFu24GXo"),
                "",
                "",
                8,
                AssetType.SPL2022
            ),
            from = Account(Chain.Solana, senderAddress, ""),
            amount = BigInteger.valueOf(10_000_000)
        )
            .transfer(destination = DestinationAddress("AGkXQZ9qm99xukisDUHvspWHESrcjs8Y4AmQQgef3BRh")) as ConfirmParams.TransferParams.Token
        val metadata = GemTransactionLoadMetadata.Solana(
            blockHash = "kiEPF6aKvEsj5nbi4FBvgRRm9ha36Y3cgDU9qnUKt32",
            recipientTokenAddress = "87vTugUvkkepa84mBRfENnvkPQRj5EZSkiG8XyFAhbQQ",
            senderTokenAddress = "87vTugUvkkepa84mBRfENnvkPQRj5EZSkiG8XyFAhbQQ",
            tokenProgram = uniffi.gemstone.SolanaTokenProgramId.TOKEN2022,
            nft = null
        )
        val result = runBlocking {
            signer.signTokenTransfer(
                params,
                metadata,
                BigInteger.ZERO,
                Fee.Solana(
                    amount = BigInteger("105005000"),
                    minerFee = BigInteger("1050000000"),
                    maxGasPrice = BigInteger("5000"),
                    unitFee = BigInteger("5000"),
                    limit = BigInteger("100000"),
                    feeAssetId = AssetId(Chain.Solana),
                    priority = FeePriority.Normal,
                    options = emptyMap(),
                ),
                privateKey
            )
        }
        assertEquals(
            "0x4151495a7138446a6c3554623571776a5144586656722f4158595350442b725151746d6459647338" +
                    "4e636f31324c594c3975383366334e4a4f32626a62357571477663346275442f6f3077453638516d" +
                    "53396d567677734241414d464e4c376b396a6e634255344677426f4e475772746262616456755471" +
                    "6b677338416930764456764f63366c70783737467363375a7773776d4234324a6539304538487a62" +
                    "2b59486131534a74465451777a3853517052655353447473696971487430636467552b566b666b35" +
                    "5849514b6e4f505a394e57366654704c696e536541775a47622b5568467a4c2f374b323663734f62" +
                    "3537794d3562764639784a724c454f624f6b41414141414733666268376e57503368684358627a6b" +
                    "624d33617468723854594f354453662b76666b6f324b474c2f4173796d44524b5948467945306565" +
                    "364774563245554669444678777576354b6c714c594b4a674d6b704241774d4143514f4945774141" +
                    "4141414141414d4142514b676867454142415142416745414367774141414141414141414141673d", result.first().hex.append0x()
        )
    }

}
