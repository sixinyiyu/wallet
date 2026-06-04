// Copyright (c). Gem Wallet. All rights reserved.

import Foundation
import Localization
import PhotosUI
import Primitives
import Store
import SupportChatService
import SwiftUI

@Observable
@MainActor
public final class SupportChatSceneViewModel {
    private let service: SupportChatService

    public let query: ObservableQuery<SupportMessagesRequest>

    public init(service: SupportChatService) {
        self.service = service
        query = ObservableQuery(SupportMessagesRequest(), initialValue: [])
    }

    var title: String { Localized.Settings.support }

    var inputBarModel: SupportMessageInputBarViewModel {
        SupportMessageInputBarViewModel(onSend: { [weak self] in self?.onSend($0) })
    }

    var days: [SupportChatDay] {
        SupportChatDayBuilder(
            messages: query.value,
            retryAction: { [weak self] in self?.onRetry($0) },
        ).build()
    }

    var isEmpty: Bool { query.value.isEmpty }

    var emptyTitle: String { "How can we help?" }
    var emptyDescription: String { "Send us a message and we'll reply as soon as we can." }

    func fetch() async {
        do {
            try await service.syncMessages(fromTimestamp: query.value.last.map { Int($0.createdAt.timeIntervalSince1970) } ?? 0)
        } catch {
            debugLog("SupportChatSceneViewModel fetch error: \(error)")
        }
    }

    func onSend(_ input: SupportInputMessage) {
        Task {
            var attachments: [ImageAttachment] = []
            for item in input.attachments {
                guard let attachment = try? await item.imageAttachment() else { continue }
                attachments.append(attachment)
            }
            do {
                try await service.sendMessage(content: input.content, attachments: attachments)
            } catch {
                debugLog("SupportChatSceneViewModel send error: \(error)")
            }
        }
    }

    func onRetry(_ message: SupportMessage) {
        Task {
            do {
                try await service.retryMessage(message)
            } catch {
                debugLog("SupportChatSceneViewModel retry error: \(error)")
            }
        }
    }
}
