// Copyright (c). Gem Wallet. All rights reserved.

import Components
import Primitives
import Style
import SwiftUI

struct SupportMessageBubble: View {
    let model: SupportMessageBubbleViewModel

    private enum Constants {
        static let maxImageWidth: CGFloat = 240
        static let imageAspectRatio: CGFloat = 4.0 / 3.0
    }

    var body: some View {
        VStack(alignment: .leading, spacing: .tiny) {
            if model.hasImages {
                imagesView
            }
            if model.hasContent {
                textBubble
            }
        }
    }

    private var textBubble: some View {
        HStack(alignment: .lastTextBaseline, spacing: .small) {
            Text(model.content)
                .font(.body)
                .foregroundStyle(model.palette.text)
            statusView
        }
        .padding(.vertical, .small)
        .padding(.horizontal, .space12)
        .background(model.palette.background)
        .clipShape(RoundedRectangle(cornerRadius: .space16))
    }

    private var imagesView: some View {
        VStack(spacing: .tiny) {
            ForEach(model.images, id: \.id) { image in
                imageView(image)
            }
        }
    }

    private func imageView(_ image: SupportMessageImage) -> some View {
        ZStack {
            CachedAsyncImage(url: (image.thumbnailUrl ?? image.url).asURL) { loaded in
                loaded.resizable().aspectRatio(contentMode: .fit)
            } placeholder: {
                Colors.grayLightFaded.aspectRatio(Constants.imageAspectRatio, contentMode: .fit)
            }
            .opacity(model.isSending ? .semiStrong : 1)
            imageOverlay
        }
        .frame(maxWidth: Constants.maxImageWidth)
        .clipShape(RoundedRectangle(cornerRadius: .space12))
    }

    @ViewBuilder
    private var imageOverlay: some View {
        switch model.status {
        case .sending:
            ProgressView()
                .controlSize(.large)
                .tint(Colors.whiteSolid)
        case .failed:
            Button(action: model.retry) {
                Image(systemName: SystemImage.refresh)
                    .font(.title2)
                    .foregroundStyle(Colors.whiteSolid)
                    .padding(.small)
                    .background(Colors.black.opacity(.medium))
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
        case .sent:
            EmptyView()
        }
    }

    @ViewBuilder
    private var statusView: some View {
        switch model.status {
        case .sending:
            ProgressView()
                .controlSize(.small)
                .tint(model.palette.secondary)
        case let .sent(time):
            Text(time)
                .font(.caption2)
                .foregroundStyle(model.palette.secondary)
        case .failed:
            Button(action: model.retry) {
                Image(systemName: SystemImage.refresh)
                    .font(.caption)
                    .foregroundStyle(model.palette.secondary)
            }
            .buttonStyle(.plain)
        }
    }
}
