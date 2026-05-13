package com.gemwallet.android.features.activities.presents.details

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gemwallet.android.features.activities.viewmodels.TransactionDetailsViewModel
import com.gemwallet.android.ui.components.screen.LoadingScene

@Composable
fun TransactionDetailsNavScreen(
    onAction: (TransactionDetailsAction.Navigation) -> Unit,
    viewModel: TransactionDetailsViewModel = hiltViewModel(),
) {
    val transaction by viewModel.data.collectAsStateWithLifecycle()
    var isShowFeeDetails by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun onShare(url: String, name: String) {
        val type = "text/plain"

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = type
        intent.putExtra(Intent.EXTRA_SUBJECT, url)
        intent.putExtra(Intent.EXTRA_TEXT, url)

        context.startActivity(Intent.createChooser(intent, name))
    }

    val model = transaction
    if (model == null) {
        LoadingScene(
            title = "",
            onCancel = { onAction(TransactionDetailsAction.Close) },
        )
        return
    }

    TransactionDetailsScene(
        data = model,
        onAction = {
            when (it) {
                TransactionDetailsAction.Share -> onShare(model.explorer.url, model.explorer.name)
                TransactionDetailsAction.ShowFeeDetails -> isShowFeeDetails = true
                is TransactionDetailsAction.Navigation -> onAction(it)
            }
        },
    )

    FeeDetailsDialog(
        isVisible = isShowFeeDetails,
        model = model.fee,
    ) { isShowFeeDetails = false }
}
