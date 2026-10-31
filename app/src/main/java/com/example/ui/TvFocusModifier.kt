package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TvFocusHighlight

/**
 * TV Remote-friendly focus modifier.
 * Automatically scales, adds glowing border, and handles D-Pad clicks.
 */
@Composable
fun Modifier.tvFocusable(
    shape: Shape = RoundedCornerShape(12.dp),
    scaleFocused: Float = 1.04f,
    borderWidth: Dp = 2.5.dp,
    focusedBorderColor: Color = TvFocusHighlight,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) scaleFocused else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "tv_focus_scale"
    )

    return this
        .scale(scale)
        .border(
            width = if (isFocused) borderWidth else 0.dp,
            color = if (isFocused) focusedBorderColor else Color.Transparent,
            shape = shape
        )
        .focusable(interactionSource = interactionSource)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}
