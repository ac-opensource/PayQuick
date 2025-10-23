package com.payquick.app.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class BackNavigationAction(
    val onBack: () -> Unit,
    val isEnabled: Boolean
)

@Composable
fun rememberBackNavigationAction(
    onNavigateBack: () -> Unit,
    cooldownMillis: Long = 500L
): BackNavigationAction {
    val scope = rememberCoroutineScope()
    var isCoolingDown by rememberSaveable { mutableStateOf(false) }
    val navigateBackState by rememberUpdatedState(onNavigateBack)

    val onBack = remember {
        {
            if (isCoolingDown) return@remember

            isCoolingDown = true
            navigateBackState()

            scope.launch {
                delay(cooldownMillis)
                isCoolingDown = false
            }
        }
    }

    return BackNavigationAction(
        onBack = onBack,
        isEnabled = !isCoolingDown
    )
}
