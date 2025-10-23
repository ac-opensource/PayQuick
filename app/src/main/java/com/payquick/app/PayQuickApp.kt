package com.payquick.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.payquick.app.auth.LoginScreen
import com.payquick.app.auth.MfaEnrollScreen
import com.payquick.app.auth.MfaVerifyScreen
import com.payquick.app.designsystem.PayQuickTheme
import com.payquick.app.home.HomeScreen
import com.payquick.app.navigation.Home
import com.payquick.app.navigation.Login
import com.payquick.app.navigation.PayQuickRoute
import com.payquick.app.navigation.Splash
import com.payquick.app.navigation.Receive
import com.payquick.app.navigation.Send
import com.payquick.app.navigation.TransactionDetails
import com.payquick.app.navigation.Transactions
import com.payquick.app.navigation.MfaEnroll
import com.payquick.app.navigation.MfaVerify
import com.payquick.app.receive.ReceiveScreen
import com.payquick.app.send.SendScreen
import com.payquick.app.session.SessionEvent
import com.payquick.app.session.SessionViewModel
import com.payquick.app.transactions.details.TransactionDetailsScreen
import com.payquick.app.transactions.TransactionsScreen
import com.payquick.app.splash.SplashScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayQuickApp() {
    PayQuickTheme {
        val sessionViewModel: SessionViewModel = hiltViewModel()
        val sessionState by sessionViewModel.state.collectAsStateWithLifecycle()
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        val hasCompletedSplash = rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(sessionState.session, hasCompletedSplash.value) {
            if (sessionState.isLoading) return@LaunchedEffect
            if (!hasCompletedSplash.value) return@LaunchedEffect
            val target = if (sessionState.session == null) Login else Home
            val currentRoute = navController.currentDestination?.route
            if (target == Home) {
                val mfaRoutes = setOf(
                    MfaEnroll::class.qualifiedName ?: MfaEnroll::class.simpleName,
                    MfaVerify::class.qualifiedName ?: MfaVerify::class.simpleName
                )
                if (currentRoute != null && currentRoute in mfaRoutes) {
                    return@LaunchedEffect
                }
            }
            val targetRoute = target::class.qualifiedName ?: target::class.simpleName
            if (navController.currentDestination?.route == targetRoute) return@LaunchedEffect
            navController.navigate(target) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = true
                    saveState = target != Login
                }
                launchSingleTop = true
                restoreState = target != Login
            }
        }

        LaunchedEffect(Unit) {
            sessionViewModel.events.collect { event ->
                when (event) {
                    is SessionEvent.Error -> snackbarHostState.showSnackbar(event.message)
                }
            }
        }

        if (sessionState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@PayQuickTheme
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            PayQuickNavHost(
                padding = innerPadding,
                onShowSnackbar = { message -> snackbarHostState.showSnackbar(message) },
                onLogout = { sessionViewModel.logout() },
                navController = navController,
                onSplashFinished = { route ->
                    hasCompletedSplash.value = true
                    navController.navigate(route) {
                        popUpTo(Splash) { inclusive = true }
                        launchSingleTop = true
                        restoreState = route != Login
                    }
                }
            )
        }
    }
}


@Composable
private fun PayQuickNavHost(
    padding: PaddingValues,
    onShowSnackbar: suspend (String) -> Unit,
    onLogout: () -> Unit,
    navController: NavHostController,
    onSplashFinished: (PayQuickRoute) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Splash,
        modifier = Modifier.padding(padding)
    ) {
        composable<Splash> {
            SplashScreen(
                onNavigate = onSplashFinished
            )
        }
        composable<Login> {
            LoginScreen(
                onNavigateToMfaEnroll = {
                    navController.navigate(MfaEnroll) {
                        launchSingleTop = true
                    }
                },
                onNavigateToMfaVerify = {
                    navController.navigate(MfaVerify) {
                        launchSingleTop = true
                    }
                },
                onShowSnackbar = onShowSnackbar
            )
        }
        composable<MfaEnroll> {
            MfaEnrollScreen(
                onSetupComplete = {
                    navController.navigate(Home) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable<MfaVerify> {
            MfaVerifyScreen(
                onVerificationComplete = {
                    navController.navigate(Home) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable<Home> {
            HomeScreen(
                onSendMoney = { navController.navigate(Send) },
                onRequestMoney = { navController.navigate(Receive) },
                onViewAllActivity = { navController.navigate(Transactions) },
                onTransactionClick = { navController.navigate(it) },
                onLogout = { onLogout() },
                onShowSnackbar = onShowSnackbar
            )
        }
        composable<Send> {
            SendScreen(
                onNavigateHome = {
                    navController.navigate(Home) {
                        popUpTo(Home) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onShowSnackbar = onShowSnackbar
            )
        }
        composable<Receive> {
            ReceiveScreen(
                onNavigateHome = {
                    navController.navigate(Home) {
                        popUpTo(Home) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onShowSnackbar = onShowSnackbar
            )
        }
        composable<Transactions> {
            TransactionsScreen(
                onTransactionClick = { navController.navigate(it) },
                onNavigateBack = { navController.popBackStack() },
                onShowSnackbar = onShowSnackbar
            )
        }
        composable<TransactionDetails> {
            val details = it.toRoute<TransactionDetails>()
            TransactionDetailsScreen(
                details = details,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
