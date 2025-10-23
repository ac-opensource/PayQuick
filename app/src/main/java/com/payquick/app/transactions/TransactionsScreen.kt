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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.payquick.R
import com.payquick.app.common.EmptyStateCard
import com.payquick.app.common.SquigglyLoadingIndicator
import com.payquick.app.common.TopBar
import com.payquick.app.common.RetryErrorCard
import com.payquick.app.common.TransactionGroupHeader
import com.payquick.app.common.TransactionListCard
import com.payquick.app.common.TransactionListItemUi
import com.payquick.app.common.transactionListSkeleton
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
    val context = LocalContext.current

    BackHandler(enabled = backAction.isEnabled) {
        backAction.onBack()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TransactionsEvent.ShowMessage -> {
                    val message = event.message ?: event.messageResId?.let(context::getString)
                    if (!message.isNullOrBlank()) {
                        onShowSnackbar(message)
                    }
                }
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
    val isRefreshing = state.isLoading && state.groups.isNotEmpty()

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
            title = stringResource(R.string.transactions_title),
            leftIcon = Icons.AutoMirrored.Rounded.ArrowBack,
            onLeftIconClick = onNavigateBack,
            leftIconEnabled = isBackEnabled
        )

        PullToRefreshBox(
            state = pullToRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            val listError = state.errorMessageResId?.let { stringResource(it) } ?: state.errorMessage

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
                    transactionListSkeleton()
                } else if (listError != null && state.groups.isEmpty()) {
                    item {
                        RetryErrorCard(
                            message = listError,
                            onRetry = onRetry
                        )
                    }
                } else if (state.groups.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = stringResource(R.string.transactions_empty_title),
                            body = if (state.searchQuery.isBlank()) {
                                stringResource(R.string.transactions_empty_body)
                            } else {
                                stringResource(R.string.transactions_empty_search, state.searchQuery)
                            }
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
                            text = stringResource(R.string.transactions_end_of_list),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                listError?.takeIf { state.groups.isNotEmpty() }?.let { message ->
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
        placeholder = { Text(stringResource(R.string.transactions_search_placeholder)) },
        leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.transactions_clear_search_cd)
                    )
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
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TransactionListFilter.values().forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onFilterChange(filter) },
                label = {
                    val label = when (filter) {
                        TransactionListFilter.ALL -> stringResource(R.string.transactions_filter_all)
                        TransactionListFilter.SENT -> stringResource(R.string.transactions_filter_sent)
                        TransactionListFilter.RECEIVED -> stringResource(R.string.transactions_filter_received)
                    }
                    Text(label)
                }
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
            Text(stringResource(R.string.transactions_retry))
        }
    }
}
