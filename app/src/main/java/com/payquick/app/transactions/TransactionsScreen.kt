package com.payquick.app.transactions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.payquick.app.common.SquigglyLoadingIndicator
import com.payquick.app.common.TopBar
import com.payquick.app.common.EmptyStateCard
import com.payquick.app.common.RetryErrorCard
import com.payquick.app.common.TransactionGroupHeader
import com.payquick.app.common.TransactionListCard
import com.payquick.app.common.TransactionListItemUi
import com.payquick.app.common.rememberBackNavigationAction
import com.payquick.app.navigation.TransactionDetails

@Composable
fun TransactionsScreen(
    onNavigateBack: () -> Unit,
    onTransactionClick: (TransactionDetails) -> Unit,
    onShowSnackbar: suspend (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val backAction = rememberBackNavigationAction(onNavigateBack)

    BackHandler(enabled = backAction.isEnabled) {
        backAction.onBack()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TransactionsEvent.ShowMessage -> onShowSnackbar(event.message)
                TransactionsEvent.LoggedOut -> backAction.onBack()
            }
        }
    }

    TransactionsContent(
        state = state,
        onNavigateBack = backAction.onBack,
        isBackEnabled = backAction.isEnabled,
        onTransactionClick = onTransactionClick,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onRetry = viewModel::onRetry,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun TransactionsContent(
    state: TransactionsUiState,
    onNavigateBack: () -> Unit,
    isBackEnabled: Boolean,
    onTransactionClick: (TransactionDetails) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (TransactionListFilter) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pullToRefreshState = rememberPullToRefreshState()

    val listState = rememberLazyListState()
    val reachedBottom by remember {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            lastVisibleIndex >= listState.layoutInfo.totalItemsCount - 1
        }
    }

    LaunchedEffect(reachedBottom) {
        if (reachedBottom && !state.isLoading && !state.isFetchingMore && !state.endReached) {
            onLoadMore()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopBar(
            modifier = Modifier.padding(horizontal = 16.dp),
            title = "Transactions",
            leftIcon = Icons.AutoMirrored.Rounded.ArrowBack,
            onLeftIconClick = onNavigateBack,
            leftIconEnabled = isBackEnabled
        )

        PullToRefreshBox(
            state = pullToRefreshState,
            isRefreshing = state.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    TransactionSearchBar(
                        query = state.searchQuery,
                        onQueryChange = onSearchQueryChange
                    )
                }
                item {
                    TransactionFilterRow(
                        selected = state.filter,
                        onFilterChange = onFilterChange
                    )
                }

                if (state.isLoading && state.groups.isEmpty()) {
                    // Handled by Pull-to-refresh indicator
                } else if (state.errorMessage != null && state.groups.isEmpty()) {
                    item {
                        RetryErrorCard(
                            message = state.errorMessage,
                            onRetry = onRetry
                        )
                    }
                } else if (state.groups.isEmpty()) {
                    val emptyBody = if (state.searchQuery.isBlank()) {
                        "As you send or receive money, your transactions will show up here."
                    } else {
                        "No results for \"${state.searchQuery}\". Try a different search."
                    }
                    item {
                        EmptyStateCard(
                            title = "No activity",
                            body = emptyBody
                        )
                    }
                } else {
                    state.groups.forEach { group ->
                        stickyHeader {
                            TransactionGroupHeader(monthLabel = group.monthLabel)
                        }
                        items(group.items, key = { it.id }) { item ->
                            TransactionListCard(
                                item = item.toListItem(),
                                onClick = {
                                    onTransactionClick(
                                        TransactionDetails(
                                            id = item.id,
                                            amountLabel = item.amountLabel,
                                            isCredit = item.isCredit,
                                            counterpartyLabel = item.counterpartyLabel,
                                            statusLabel = item.statusLabel,
                                            timestampLabel = item.subtitle,
                                            currencyCode = item.currencyCode,
                                            directionLabel = item.directionLabel
                                        )
                                    )
                                }
                            )
                        }
                    }
                }

                if (state.isFetchingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            SquigglyLoadingIndicator()
                        }
                    }
                }

                if (state.endReached) {
                    item {
                        Text(
                            text = "You've reached the end of your transactions.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                state.errorMessage?.takeIf { state.groups.isNotEmpty() }?.let { message ->
                    item { TransactionsInlineError(message = message, onRetry = onRetry) }
                }
            }
        }
    }
}

@Composable
private fun TransactionSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search transactions") },
        leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "Clear search")
                }
            }
        },
        shape = CircleShape,
        singleLine = true
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransactionFilterRow(
    selected: TransactionListFilter,
    onFilterChange: (TransactionListFilter) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TransactionListFilter.values().forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(filter.name) }
            )
        }
    }
}

private fun TransactionUiItem.toListItem(): TransactionListItemUi {
    return TransactionListItemUi(
        id = id,
        title = title,
        subtitle = subtitle,
        amountLabel = amountLabel,
        isCredit = isCredit
    )
}

@Composable
private fun TransactionsInlineError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        TextButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}
