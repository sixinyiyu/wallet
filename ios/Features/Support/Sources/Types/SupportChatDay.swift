// Copyright (c). Gem Wallet. All rights reserved.

import Foundation
import Primitives

struct SupportChatDay: Identifiable {
    let id: String
    let date: Date
    let groups: [SupportChatGroup]
}

enum SupportChatGroup: Identifiable {
    case user(messages: [SupportMessageBubbleViewModel])
    case agent(header: SupportAgentHeader, messages: [SupportMessageBubbleViewModel])

    var id: String {
        switch self {
        case let .user(messages): "user-\(messages.first?.id ?? "")"
        case let .agent(_, messages): "agent-\(messages.first?.id ?? "")"
        }
    }
}

struct SupportAgentHeader {
    let name: String
    let avatarURL: URL?

    init(agent: SupportAgent) {
        name = agent.name
        avatarURL = agent.avatarUrl?.asURL
    }
}
