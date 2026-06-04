// Copyright (c). Gem Wallet. All rights reserved.

import PhotosUI
import Style
import SwiftUI
import Primitives

struct SupportSelectedItemPreview: View {
    let item: PhotosPickerItem
    let onRemove: () -> Void

    @State private var image: UIImage?

    var body: some View {
        ZStack(alignment: .topTrailing) {
            thumbnail
                .frame(size: .image.semiLarge)
                .clipShape(RoundedRectangle(cornerRadius: .space8))
            removeButton
                .padding(.tiny)
        }
        .task {
            guard image == nil else { return }
            if let data = try? await item.loadTransferable(type: Data.self) {
                image = UIImage(data: data)
            }
        }
    }

    @ViewBuilder
    private var thumbnail: some View {
        if let image {
            Image(uiImage: image)
                .resizable()
                .aspectRatio(contentMode: .fill)
        } else {
            Rectangle()
                .fill(Colors.grayVeryLight)
                .overlay {
                    Image(systemName: SystemImage.photo)
                        .foregroundStyle(Colors.gray)
                }
        }
    }

    private var removeButton: some View {
        Button(action: onRemove) {
            Image(systemName: SystemImage.xmarkCircle)
                .font(.title3)
                .foregroundStyle(Colors.black, Colors.whiteSolid)
                .background(Circle().fill(Colors.whiteSolid))
        }
        .buttonStyle(.plain)
    }
}
