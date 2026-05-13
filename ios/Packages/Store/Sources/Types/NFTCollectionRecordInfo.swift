// Copyright (c). Gem Wallet. All rights reserved.

import Foundation
import GRDB
import Primitives

struct NFTCollectionRecordInfo: Codable, FetchableRecord {
    let collection: NFTCollectionRecord
    let assets: [NFTAssetRecord]
}

extension NFTCollectionRecordInfo {
    func mapToNFTData() -> NFTData {
        NFTData(
            collection: collection.mapToCollection(),
            assets: assets.map { $0.mapToAsset() },
        )
    }
}

extension NFTCollectionRecord {
    func mapToCollection() -> NFTCollection {
        NFTCollection(
            id: id,
            name: name,
            description: description,
            chain: chain,
            contractAddress: contractAddress,
            images: NFTImages(
                preview: NFTResource(
                    url: previewImageUrl,
                    mimeType: previewImageMimeType,
                ),
            ),
            status: status,
            links: links ?? [],
        )
    }
}
