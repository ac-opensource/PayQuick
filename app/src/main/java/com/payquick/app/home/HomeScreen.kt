package com.payquick.app.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.payquick.R
import com.payquick.app.common.SquigglyLoadingIndicator
import com.payquick.app.common.TransactionGroupHeader
import com.payquick.app.common.TransactionListCard
import com.payquick.app.common.TransactionListItemUi
import com.payquick.app.common.EmptyStateCard
import com.payquick.app.common.RetryErrorCard
import com.payquick.app.navigation.TransactionDetails

@Composable
fun HomeScreen(
    onSendMoney: () -> Unit,
    onRequestMoney: () -> Unit,
    onViewAllActivity: () -> Unit,
    onTransactionClick: (TransactionDetails) -> Unit,
    onLogout: () -> Unit,
    onShowSnackbar: suspend (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.ShowMessage -> onShowSnackbar(event.message)
            }
        }
    }

    HomeContent(
        state = state,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMoreTransactions,
        onSendMoney = onSendMoney,
        onRequestMoney = onRequestMoney,
        onViewAllActivity = onViewAllActivity,
        onTransactionClick = onTransactionClick,
        onLogout = onLogout,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun HomeContent(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onSendMoney: () -> Unit,
    onRequestMoney: () -> Unit,
    onViewAllActivity: () -> Unit,
    onTransactionClick: (TransactionDetails) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val lazyListState = rememberLazyListState()
    val isScrolledToEnd by remember {
        derivedStateOf {
            val lastVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index == lazyListState.layoutInfo.totalItemsCount - 1
        }
    }

    LaunchedEffect(isScrolledToEnd) {
        if (isScrolledToEnd && !state.isLoading && !state.isFetchingMore && !state.endReached) {
            onLoadMore()
        }
    }

    PullToRefreshBox(
        state = pullToRefreshState,
        isRefreshing = state.isLoading,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            HomeTopBar(userName = state.headline, onLogout = onLogout)

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 54.dp)
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    BalanceCard(state = state)
                }
                item {
                    QuickActionsRow(
                        onSendMoney = onSendMoney,
                        onRequestMoney = onRequestMoney
                    )
                }
                item {
                    SectionHeader(
                        title = "Transactions",
                        trailingContent = {
                            TextButton(onClick = onViewAllActivity) {
                                Text("View all")
                            }
                        }
                    )
                }
            if (state.errorMessage != null && state.transactionGroups.isEmpty()) {
                item {
                    RetryErrorCard(
                        message = state.errorMessage,
                        onRetry = onRefresh
                    )
                }
            } else if (state.transactionGroups.isEmpty() && !state.isLoading) {
                item {
                    EmptyStateCard(
                        title = "No activity yet",
                        body = "When you start sending or receiving money you'll see the latest movement here."
                    )
                }
            } else {
                state.transactionGroups.forEach { group ->
                    stickyHeader {
                        TransactionGroupHeader(monthLabel = group.monthLabel)
                    }
                    items(group.items, key = { it.id }) { transaction ->
                        TransactionListCard(
                            item = transaction.toListItem(),
                            onClick = {
                                val direction = if (transaction.isCredit) {
                                    "Received from ${transaction.title}"
                                } else {
                                    "Sent to ${transaction.title}"
                                }
                                onTransactionClick(
                                    TransactionDetails(
                                        id = transaction.id,
                                        amountLabel = transaction.amountLabel,
                                        isCredit = transaction.isCredit,
                                        counterpartyLabel = transaction.title,
                                        statusLabel = transaction.status,
                                        timestampLabel = transaction.subtitle,
                                        currencyCode = transaction.currencyCode,
                                        directionLabel = direction
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
                                .padding(16.dp),
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
                                .padding(16.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(state: HomeUiState) {
    val colors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
    )
    val gradient = remember {
        Brush.linearGradient(
            colors = colors
        )
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Your balance:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Crossfade(targetState = state.balance, label = "balance") { balance ->
                    Column {
                        Text(
                            text = balance,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Text(
                            text = "Consolidated amount",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                }

            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    onSendMoney: () -> Unit,
    onRequestMoney: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        QuickActionButton(
            label = "Send Money",
            icon = R.drawable.send_money_24px,
            onClick = onSendMoney,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = "Receive Money",
            icon = R.drawable.qr_code_2_add_24px,
            onClick = onRequestMoney,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(painter = painterResource(id = icon), contentDescription = null)
            Text(text = label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        trailingContent?.invoke()
    }
}

private fun HomeTransactionUi.toListItem(): TransactionListItemUi {
    return TransactionListItemUi(
        id = id,
        title = title,
        subtitle = subtitle,
        amountLabel = amountLabel,
        isCredit = isCredit
    )
}

@Composable
private fun HomeTopBar(userName: String, onLogout: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_default_user),
                contentDescription = "Account avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.weight(1f))

        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Rounded.Notifications,
                contentDescription = "Notifications"
            )
        }

        IconButton(onClick = onLogout) {
            Icon(
                imageVector = Icons.Rounded.ExitToApp,
                contentDescription = "Log out"
            )
        }
    }
}
