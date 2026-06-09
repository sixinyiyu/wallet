// Copyright (c). Gem Wallet. All rights reserved.

import Assets
import AssetsService
import Components
import Localization
import Primitives
import PrimitivesComponents
import Store
import Style
import SwiftUI
import Transactions

struct TransactionsNavigationStack: View {
    @Environment(\.navigationState) private var navigationState
    @Environment(\.assetsEnabler) private var assetsEnabler
    @Environment(\.assetsService) private var assetsService
    @Environment(\.priceAlertService) private var priceAlertService
    @Environment(\.activityService) private var activityService
    @Environment(\.assetSearchService) private var assetSearchService
    @Environment(\.avatarService) private var avatarService
    @Environment(\.navigationPresenter) private var presenter

    @State private var model: TransactionsViewModel

    init(model: TransactionsViewModel) {
        _model = State(wrappedValue: model)
    }

    private var navigationPath: Binding<NavigationPath> {
        navigationState.activity.binding
    }

    var body: some View {
        NavigationStack(path: navigationPath) {
            TransactionsScene(model: model)
                .bindQuery(model.filterModel.query)
                .onChange(
                    of: model.currentWallet,
                    initial: true,
                    model.onChangeWallet,
                )
                .toolbar {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        FilterButton(
                            isActive: model.filterModel.isAnyFilterSpecified,
                            action: model.onSelectFilterButton,
                        )
                    }
                }
                .navigationBarTitleDisplayMode(.inline)
                .navigationTitle(model.title)
                .navigationDestination(for: Scenes.Transaction.self) {
                    TransactionNavigationView(
                        model: TransactionSceneViewModel(
                            transaction: $0.transaction,
                            walletId: model.wallet.id,
                            onHeaderAction: onSelectTransactionHeaderAction,
                        ),
                    )
                }
                .toast(message: $model.isPresentingToastMessage)
                .sheet(item: $model.isPresentingSheet) { type in
                    switch type {
                    case .filter:
                        NavigationStack {
                            TransactionsFilterScene(model: $model.filterModel)
                        }
                        .presentationDetentsForCurrentDeviceSize(expandable: true)
                        .presentationDragIndicator(.visible)
                        .presentationBackground(Colors.grayBackground)
                    case let .selectAsset(selectType):
                        SelectAssetSceneNavigationStack(
                            model: SelectAssetViewModel(
                                wallet: model.wallet,
                                selectType: selectType,
                                searchService: assetSearchService,
                                assetsEnabler: assetsEnabler,
                                priceAlertService: priceAlertService,
                                activityService: activityService,
                            ),
                        )
                    }
                }
        }
    }
}

// MARK: - Actions

extension TransactionsNavigationStack {
    private func onSelectTransactionHeaderAction(_ action: TransactionHeaderAction) {
        Task {
            do {
                    try await presenter.handleTransactionHeaderAction(
                        action,
                        wallet: model.wallet,
                        navigationState: navigationState,
                        assetsService: assetsService,
                    )            } catch {
                model.isPresentingToastMessage = .error(Localized.Errors.errorOccured)
            }
        }
    }
}