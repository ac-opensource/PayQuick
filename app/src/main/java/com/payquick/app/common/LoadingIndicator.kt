package com.payquick.app.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

@Composable
fun DotLoadingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400),
            repeatMode = RepeatMode.Reverse
        )
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, delayMillis = 100),
            repeatMode = RepeatMode.Reverse
        )
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .alpha(alpha1)
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .alpha(alpha2)
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .alpha(alpha3)
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = CircleShape
                )
        )
    }
}
