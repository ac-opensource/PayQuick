package com.payquick.app.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun SwipeToSend(
    onSwiped: () -> Unit,
    modifier: Modifier = Modifier
) {
    var swipeOffset by remember { mutableStateOf(0f) }
    var swipeWidth by remember { mutableStateOf(0) }
    val animatedSwipeOffset by animateFloatAsState(targetValue = swipeOffset)
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .onSizeChanged { swipeWidth = it.width }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    val newOffset = swipeOffset + delta
                    swipeOffset = newOffset.coerceIn(0f, (swipeWidth - with(density) { 64.dp.toPx() }))
                },
                onDragStopped = {
                    if (swipeOffset > swipeWidth * 0.7) {
                        onSwiped()
                    }
                    swipeOffset = 0f
                }
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = with(density) { animatedSwipeOffset.toDp() })
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = "Send",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
