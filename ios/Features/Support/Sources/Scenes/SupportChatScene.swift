// Copyright (c). Gem Wallet. All rights reserved.

import Components
import Primitives
import Store
import Style
import SwiftUI

public struct SupportChatScene: View {
    @State private var model: SupportChatSceneViewModel

    public init(model: SupportChatSceneViewModel) {
        _model = State(initialValue: model)
    }

    public var body: some View {
        ScrollView {
            LazyVStack(spacing: .small) {
                ForEach(model.days) { day in
                    SupportDateSeparator(date: day.date)
                    ForEach(day.groups) { group in
                        switch group {
                        case let .agent(header, messages):
                            SupportAgentMessageGroup(header: header, messages: messages)
                        case let .user(messages):
                            SupportUserMessageGroup(messages: messages)
                        }
                    }
                }
            }
            .padding(.medium)
        }
        .defaultScrollAnchor(.bottom)
        .bindQuery(model.query)
        .background(Colors.grayBackground)
        .overlay {
            if model.isEmpty {
                StateEmptyView(
                    title: model.emptyTitle,
                    description: model.emptyDescription,
                    image: Image(systemName: SystemImage.bubbleLeftAndBubbleRight),
                )
                .padding(.medium)
            }
        }
        .safeAreaInset(edge: .bottom) {
            SupportMessageInputBar(model: model.inputBarModel)
        }
        .navigationTitle(model.title)
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await model.fetch()
        }
    }
}
