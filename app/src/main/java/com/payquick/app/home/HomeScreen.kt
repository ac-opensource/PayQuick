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
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.payquick.R
import com.payquick.app.common.EmptyStateCard
import com.payquick.app.common.RetryErrorCard
import com.payquick.app.common.SquigglyLoadingIndicator
import com.payquick.app.common.TransactionGroupHeader
import com.payquick.app.common.TransactionListCard
import com.payquick.app.common.TransactionListItemUi
import com.payquick.app.common.transactionListSkeleton
import com.payquick.app.designsystem.PayQuickTheme
import com.payquick.app.navigation.TransactionDetails
import java.time.LocalDateTime
import java.time.Month

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
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.ShowMessage -> {
                    val message = event.message ?: event.messageResId?.let(context::getString)
                    if (!message.isNullOrBlank()) {
                        onShowSnackbar(message)
                    }
                }
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
    val isRefreshing = state.isLoading && state.transactionGroups.isNotEmpty()
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
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            HomeTopBar(userName = state.headline, onLogout = onLogout)
            val listError = state.errorMessageResId?.let { stringResource(it) } ?: state.errorMessage

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
                        title = stringResource(R.string.home_transactions_title),
                        trailingContent = {
                            TextButton(onClick = onViewAllActivity) {
                                Text(stringResource(R.string.home_transactions_view_all))
                            }
                        }
                    )
                }


            if (state.isLoading && state.transactionGroups.isEmpty()) {
                transactionListSkeleton()
            } else if (listError != null && state.transactionGroups.isEmpty()) {
                item {
                    RetryErrorCard(
                        message = listError,
                        onRetry = onRefresh
                    )
                }
            } else if (state.transactionGroups.isEmpty() && !state.isLoading) {
                item {
                    EmptyStateCard(
                        title = stringResource(R.string.home_empty_title),
                        body = stringResource(R.string.home_empty_body)
                    )
                }
            } else {
                state.transactionGroups.forEach { group ->
                    stickyHeader {
                        TransactionGroupHeader(monthLabel = group.monthLabel)
                    }
                    items(group.items, key = { it.id }) { transaction ->
                        val direction = if (transaction.isCredit) {
                            stringResource(R.string.home_transaction_direction_received, transaction.title)
                        } else {
                            stringResource(R.string.home_transaction_direction_sent, transaction.title)
                        }
                        TransactionListCard(
                            item = transaction.toListItem(),
                            onClick = {
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
                            text = stringResource(R.string.home_end_of_list),
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
                        text = stringResource(R.string.home_balance_label),
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
                            text = stringResource(R.string.home_balance_subtitle),
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
            label = stringResource(R.string.home_quick_action_send),
            icon = R.drawable.send_money_24px,
            onClick = onSendMoney,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = stringResource(R.string.home_quick_action_receive),
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
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
            contentColor = MaterialTheme.colorScheme.onSurface
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
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_default_user),
                    contentDescription = stringResource(R.string.home_avatar_cd),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Text(
                    text = userName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.weight(1f))

            IconButton(
                onClick = { },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = stringResource(R.string.home_notifications_cd),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = onLogout,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.ExitToApp,
                    contentDescription = stringResource(R.string.home_log_out_cd),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Balance Card")
@Composable
private fun BalanceCardPreview() {
    PayQuickTheme {
        BalanceCard(
            state = PreviewHomeStateLight
        )
    }
}

@Preview(showBackground = true, name = "Quick Actions")
@Composable
private fun QuickActionsRowPreview() {
    PayQuickTheme {
        QuickActionsRow(onSendMoney = {}, onRequestMoney = {})
    }
}

@Preview(showBackground = true, name = "Section Header")
@Composable
private fun SectionHeaderPreview() {
    PayQuickTheme {
        SectionHeader(title = "Recent activity") {
            Text(text = "View all", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Preview(showBackground = true, name = "Home Top Bar")
@Composable
private fun HomeTopBarPreview() {
    PayQuickTheme {
        HomeTopBar(userName = "Jamie Rivera", onLogout = {})
    }
}

private val PreviewHomeTransactions = listOf(
    HomeTransactionGroup(
        monthLabel = "October 2024",
        items = listOf(
            HomeTransactionUi(
                id = "txn-1",
                title = "Groceries",
                subtitle = "Oct 4 · 6:42 PM",
                amountLabel = "-$54.12",
                isCredit = false,
                status = "Completed",
                dateTime = LocalDateTime.of(2024, Month.OCTOBER, 4, 18, 42),
                currencyCode = "USD"
            ),
            HomeTransactionUi(
                id = "txn-2",
                title = "Salary",
                subtitle = "Oct 1 · 9:00 AM",
                amountLabel = "+$2,800.00",
                isCredit = true,
                status = "Completed",
                dateTime = LocalDateTime.of(2024, Month.OCTOBER, 1, 9, 0),
                currencyCode = "USD"
            )
        )
    )
)

private val PreviewHomeStateLight = HomeUiState(
    isLoading = false,
    headline = "Welcome back, Jamie",
    subHeadline = "Here's the latest",
    balance = "$3,482.15",
    lastRefreshedLabel = "Updated moments ago",
    transactionGroups = PreviewHomeTransactions
)

@Preview(showBackground = true, name = "Home - Light")
@Composable
private fun HomeContentPreviewLight() {
    PayQuickTheme {
        HomeContent(
            state = PreviewHomeStateLight,
            onRefresh = {},
            onLoadMore = {},
            onSendMoney = {},
            onRequestMoney = {},
            onViewAllActivity = {},
            onTransactionClick = {},
            onLogout = {}
        )
    }
}

@Preview(showBackground = true, name = "Home - Dark")
@Composable
private fun HomeContentPreviewDark() {
    PayQuickTheme(darkTheme = true) {
        HomeContent(
            state = PreviewHomeStateLight.copy(isFetchingMore = true),
            onRefresh = {},
            onLoadMore = {},
            onSendMoney = {},
            onRequestMoney = {},
            onViewAllActivity = {},
            onTransactionClick = {},
            onLogout = {}
        )
    }
}
