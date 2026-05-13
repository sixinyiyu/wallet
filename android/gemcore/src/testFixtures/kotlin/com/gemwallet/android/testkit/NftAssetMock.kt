package com.gemwallet.android.testkit

import com.gemwallet.android.ext.toIdentifier
import com.wallet.core.primitives.NFTAsset
import com.wallet.core.primitives.NFTAssetId
import com.wallet.core.primitives.NFTAttribute
import com.wallet.core.primitives.NFTCollectionId
import com.wallet.core.primitives.NFTImages
import com.wallet.core.primitives.NFTResource
import com.wallet.core.primitives.NFTType

fun mockNftAsset(
    id: NFTAssetId = mockNftAssetId(),
    collectionId: NFTCollectionId = mockNftCollectionId(),
    name: String = id.toIdentifier(),
    description: String? = null,
    tokenType: NFTType = NFTType.ERC721,
    resource: NFTResource = NFTResource("", ""),
    images: NFTImages = NFTImages(NFTResource("", "")),
    attributes: List<NFTAttribute> = emptyList(),
) = NFTAsset(
    id = id,
    collectionId = collectionId,
    chain = id.chain,
    contractAddress = id.contractAddress,
    tokenId = id.tokenId,
    tokenType = tokenType,
    name = name,
    description = description,
    resource = resource,
    images = images,
    attributes = attributes,
)
