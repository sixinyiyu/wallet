// Copyright (c). Gem Wallet. All rights reserved.

import Components
import Foundation
import Localization
import Primitives
import PrimitivesComponents
import Store
import Style
import SwiftUI

public struct WalletImageScene: View {
    enum Tab: Equatable {
        case emoji
    }

    @Environment(\.dismiss) private var dismiss
    @State private var model: WalletImageViewModel

    public init(model: WalletImageViewModel) {
        _model = State(initialValue: model)
    }

    public var body: some View {
        VStack {
            if let dbWallet = model.dbWallet {
                AvatarView(
                    avatarImage: WalletViewModel(wallet: dbWallet).avatarImage,
                    size: model.emojiViewSize,
                    action: setDefaultAvatar,
                )
                .padding(.top, .medium)
                .padding(.bottom, .extraLarge)
            }
            listView
        }
        .bindQuery(model.walletQuery)
        .navigationTitle(model.title)
        .navigationBarTitleDisplayMode(.inline)
        .background(Colors.grayBackground)
    }

    private var listView: some View {
        ScrollView {
            LazyVGrid(
                columns: model.getColumns(for: .emoji),
                alignment: .center,
                spacing: .medium,
            ) {
                emojiListView
            }
            .padding(.horizontal, .medium)
        }
    }

    private var emojiListView: some View {
        ForEach(model.emojiList) { value in
            NavigationCustomLink(
                with: EmojiView(color: value.color, emoji: value.emoji),
            ) {
                model.setAvatarEmoji(value: value)
                onDismiss()
            }
            .frame(maxWidth: .infinity)
            .transition(.opacity)
        }
    }
}

// MARK: - Actions

private extension WalletImageScene {
    func setDefaultAvatar() {
        model.setDefaultAvatar()
        onDismiss()
    }

    func onDismiss() {
        switch model.source {
        case .onboarding: dismiss()
        case .wallet: break
        }
    }
}