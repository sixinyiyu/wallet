package com.gemwallet.android.data.coordinators.asset_select

import com.gemwallet.android.application.asset_select.coordinators.UpdateRecentAsset
import com.gemwallet.android.data.repositories.assets.AssetsRepository
import com.gemwallet.android.data.repositories.session.SessionRepository
import com.gemwallet.android.model.RecentType
import com.wallet.core.primitives.AssetId
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

class UpdateRecentAssetImpl(
    private val sessionRepository: SessionRepository,
    private val assetsRepository: AssetsRepository,
) : UpdateRecentAsset {
    override suspend fun invoke(assetId: AssetId, type: RecentType) {
        val wallet = sessionRepository.session().filterNotNull().first().wallet
        assetsRepository.addRecentActivity(assetId, wallet.id.id, type)
    }
}
