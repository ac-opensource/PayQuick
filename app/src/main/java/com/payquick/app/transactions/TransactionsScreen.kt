package com.payquick.app.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TransactionsScreen(
    onNavigateBack: () -> Unit,
    onShowSnackbar: suspend (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TransactionsEvent.ShowMessage -> onShowSnackbar(event.message)
                TransactionsEvent.LoggedOut -> onNavigateBack()
            }
        }
    }

    TransactionsContent(
        state = state,
        onNextPage = viewModel::onNextPage,
        onPreviousPage = viewModel::onPreviousPage,
        onRetry = viewModel::onRetry,
        onLogout = viewModel::onLogout,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionsContent(
    state: TransactionsUiState,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(imageVector = Icons.Rounded.ExitToApp, contentDescription = "Log out")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            state.isInitialLoading -> LoadingContent(innerPadding)
            state.errorMessage != null && state.groups.isEmpty() -> ErrorContent(
                padding = innerPadding,
                message = state.errorMessage,
                onRetry = onRetry
            )
            else -> TransactionList(
                state = state,
                padding = innerPadding,
                onNextPage = onNextPage,
                onPreviousPage = onPreviousPage,
                onRetry = onRetry
            )
        }
    }
}

@Composable
private fun LoadingContent(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(
    padding: PaddingValues,
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Try again")
        }
    }
}

@Composable
private fun TransactionList(
    state: TransactionsUiState,
    padding: PaddingValues,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            state.groups.forEach { group ->
                item(key = group.monthLabel) {
                    Text(
                        text = group.monthLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(group.items, key = { it.id }) { item ->
                    TransactionCard(item)
                }
            }
        }
        PaginationBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            state = state,
            onNextPage = onNextPage,
            onPreviousPage = onPreviousPage,
            onRetry = onRetry
        )
    }
}

@Composable
private fun TransactionCard(item: TransactionUiItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = item.description, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = item.timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = item.amount,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (item.amount.startsWith("-")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                )
            }
            Text(
                text = item.status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PaginationBar(
    modifier: Modifier,
    state: TransactionsUiState,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
    onRetry: () -> Unit
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onPreviousPage, enabled = state.canLoadPrevious) {
                Text("Previous")
            }
            Text("Page ${state.currentPage} of ${state.totalPages}")
            TextButton(onClick = onNextPage, enabled = state.canLoadNext) {
                Text("Next")
            }
        }
        if (state.isPaginating) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .height(24.dp)
                        .width(24.dp)
                )
            }
        }
        state.errorMessage?.takeIf { state.groups.isNotEmpty() }?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
            TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
                Text("Retry")
            }
        }
    }
}
