// Copyright (c). Gem Wallet. All rights reserved.

@testable import Blockchain
import Foundation
import Gemstone
import Testing

struct ChainCoreErrorTests {
    @Test
    func fromError() {
        #expect(ChainCoreError.fromError(ChainCoreError.dustChange) == .dustChange)
        #expect(ChainCoreError.fromError(ChainCoreError.dustThreshold) == .dustThreshold)
        #expect(ChainCoreError.fromError(Gemstone.GatewayError.PlatformError(msg: "dustChange")) == .dustChange)
        #expect(ChainCoreError.fromError(Gemstone.GatewayError.PlatformError(msg: "dustThreshold")) == .dustThreshold)
        #expect(ChainCoreError.fromError(Gemstone.GatewayError.PlatformError(msg: "garbage")) == nil)
        #expect(ChainCoreError.fromError(NSError(domain: "test", code: 0)) == nil)
    }
}
