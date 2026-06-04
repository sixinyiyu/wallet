// Copyright (c). Gem Wallet. All rights reserved.

import Components
import PhotosUI
import Primitives
import Style
import SwiftUI

struct SupportMessageInputBar: View {
    @State private var model: SupportMessageInputBarViewModel

    init(model: SupportMessageInputBarViewModel) {
        _model = State(initialValue: model)
    }

    var body: some View {
        @Bindable var model = model
        VStack(alignment: .leading, spacing: .small) {
            if model.selectedItems.isNotEmpty {
                previewStrip
            }
            HStack(alignment: .bottom, spacing: .small) {
                attachButton
                textField
                sendButton
            }
        }
        .padding(.horizontal, .medium)
        .padding(.vertical, .small)
        .frame(maxWidth: .infinity)
        .background(Colors.grayBackground.ignoresSafeArea(edges: .bottom))
    }

    private var previewStrip: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: .small) {
                ForEach(model.selectedItems, id: \.self) { item in
                    SupportSelectedItemPreview(item: item, onRemove: { model.removeItem(item) })
                }
            }
        }
    }

    private var attachButton: some View {
        PhotosPicker(selection: $model.selectedItems, matching: .images, photoLibrary: .shared()) {
            AttachButtonLabel()
        }
    }

    private var textField: some View {
        TextField(model.placeholder, text: $model.text, axis: .vertical)
            .lineLimit(1 ... 8)
            .padding(.vertical, .small)
            .padding(.horizontal, .space12)
            .liquidGlass(
                interactive: false,
                in: RoundedRectangle(cornerRadius: .space16),
                fallback: { $0.background(Colors.grayVeryLight).clipShape(RoundedRectangle(cornerRadius: .space16)) },
            )
    }

    private struct AttachButtonLabel: View {
        var body: some View {
            Image(systemName: SystemImage.plus)
                .font(.system(size: .space16, weight: .semibold))
                .foregroundStyle(Colors.gray)
                .frame(size: .space32 + .space6)
                .liquidGlass(fallback: { $0.background(Colors.grayVeryLight).clipShape(Circle()) })
        }
    }

    private var sendButton: some View {
        Button(action: model.send) {
            Image(systemName: SystemImage.arrowUp)
                .font(.system(size: .space16, weight: .semibold))
                .foregroundStyle(model.canSend ? Colors.whiteSolid : Colors.gray)
                .frame(size: .space32 + .space6)
                .liquidGlass(
                    tint: model.canSend ? Colors.blue : nil,
                    interactive: model.canSend,
                    fallback: { $0.background(model.canSend ? Colors.blue : Colors.grayVeryLight).clipShape(Circle()) },
                )
        }
        .disabled(!model.canSend)
    }
}
