// Copyright (c). Gem Wallet. All rights reserved.

import Foundation
import Primitives

struct SupportChatDayBuilder {
    let messages: [SupportMessage]
    let retryAction: (SupportMessage) -> Void

    func build() -> [SupportChatDay] {
        Dictionary(grouping: messages) { Calendar.current.startOfDay(for: $0.createdAt) }
            .sorted { $0.key < $1.key }
            .map { date, messages in
                SupportChatDay(id: date.ISO8601Format(), date: date, groups: groups(from: messages))
            }
    }
}

// MARK: - Private

private extension SupportChatDayBuilder {
    func groups(from messages: [SupportMessage]) -> [SupportChatGroup] {
        messages
            .chunked(by: sameSender)
            .compactMap(group(from:))
    }

    func group(from messages: [SupportMessage]) -> SupportChatGroup? {
        guard let sender = messages.first?.sender else { return nil }
        let bubbles = messages.map { SupportMessageBubbleViewModel(message: $0, retryAction: retryAction) }
        switch sender {
        case .user:
            return .user(messages: bubbles)
        case let .agent(agent):
            return .agent(header: SupportAgentHeader(agent: agent), messages: bubbles)
        }
    }

    func sameSender(_ lhs: SupportMessage, _ rhs: SupportMessage) -> Bool {
        switch (lhs.sender, rhs.sender) {
        case (.user, .user), (.agent, .agent): true
        case (.user, .agent), (.agent, .user): false
        }
    }
}

private extension Array {
    func chunked(by belongsInSameChunk: (Element, Element) -> Bool) -> [[Element]] {
        var chunks: [[Element]] = []
        for element in self {
            if let last = chunks.last?.last, belongsInSameChunk(last, element) {
                chunks[chunks.count - 1].append(element)
            } else {
                chunks.append([element])
            }
        }
        return chunks
    }
}
