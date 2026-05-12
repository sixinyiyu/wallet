package com.gemwallet.android.testkit

import com.gemwallet.android.ext.toIdentifier
import com.wallet.core.primitives.AssetLink
import com.wallet.core.primitives.NFTCollection
import com.wallet.core.primitives.NFTCollectionId
import com.wallet.core.primitives.NFTImages
import com.wallet.core.primitives.NFTResource
import com.wallet.core.primitives.VerificationStatus

fun mockNftCollection(
    id: NFTCollectionId = mockNftCollectionId(),
    name: String = id.toIdentifier(),
    description: String? = null,
    images: NFTImages = NFTImages(NFTResource("", "")),
    status: VerificationStatus = VerificationStatus.Verified,
    links: List<AssetLink> = emptyList(),
) = NFTCollection(
    id = id,
    name = name,
    description = description,
    chain = id.chain,
    contractAddress = id.contractAddress,
    images = images,
    status = status,
    links = links,
)
