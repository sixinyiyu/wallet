// Copyright (c). Gem Wallet. All rights reserved.

import Foundation
import GemAPI
import Primitives
import Store

public final class SupportChatService: Sendable {
    private let store: SupportChatStore
    private let provider: any GemAPISupportService
    private let imageStore = SupportImageStore()

    public init(
        store: SupportChatStore,
        provider: any GemAPISupportService = GemAPIService.shared,
    ) {
        self.store = store
        self.provider = provider
    }

    public func syncMessages(fromTimestamp: Int) async throws {
        try store.addMessages(await provider.getSupportMessages(fromTimestamp: fromTimestamp))
    }

    public func sendMessage(content: String, attachments: [ImageAttachment]) async throws {
        let messages = try pendingMessages(content: content, attachments: attachments)
        try store.addMessages(messages)
        for message in messages {
            await deliver(message)
        }
    }

    public func retryMessage(_ message: SupportMessage) async throws {
        try store.addMessages([message.with(deliveryStatus: .sending)])
        await deliver(message)
    }
}

// MARK: - Private

private extension SupportChatService {
    func pendingMessages(content: String, attachments: [ImageAttachment]) throws -> [SupportMessage] {
        var messages: [SupportMessage] = []
        if content.isNotEmpty {
            messages.append(.userText(content))
        }
        for attachment in attachments {
            let id = UUID().uuidString
            let url = try imageStore.store(attachment.data, id: id)
            messages.append(.userImage(id: id, url: url, fileName: attachment.fileName, fileSize: attachment.data.count))
        }
        return messages
    }

    func deliver(_ message: SupportMessage) async {
        do {
            let sent = try await send(message)
            try store.replace(id: message.id, with: sent)
            removeLocalImage(of: message)
        } catch {
            try? store.addMessages([message.with(deliveryStatus: .failed)])
        }
    }

    func send(_ message: SupportMessage) async throws -> SupportMessage {
        if let image = message.images.first, let url = image.url.asURL, let data = imageStore.data(at: url) {
            return try await provider.sendSupportImage(image: data, fileName: image.fileName ?? "image", mimeType: image.mimeType)
        }
        return try await provider.sendSupportMessage(input: SupportMessageInput(content: message.content))
    }

    func removeLocalImage(of message: SupportMessage) {
        guard let url = message.images.first?.url.asURL else { return }
        imageStore.remove(at: url)
    }
}
