package cn.parentchess

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

val LocalUiClick = staticCompositionLocalOf<() -> Unit> { {} }

/** Share the native click semantics and ripple; add a soft physical press. */
@Composable
fun rememberUiPress(): Pair<MutableInteractionSource, Modifier> {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .94f else 1f,
        spring(dampingRatio = .48f, stiffness = 650f), label = "button press")
    return interaction to Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
}

@Composable
private fun FeedbackButton(kind: Int, onClick: () -> Unit, modifier: Modifier, enabled: Boolean,
                           contentPadding: PaddingValues, content: @Composable RowScope.() -> Unit) {
    val (interaction, press) = rememberUiPress()
    val sound = LocalUiClick.current
    val click = { onClick(); sound() }
    when (kind) {
        0 -> Button(click, modifier.then(press), enabled = enabled, contentPadding = contentPadding, interactionSource = interaction, content = content)
        1 -> TextButton(click, modifier.then(press), enabled = enabled, contentPadding = contentPadding, interactionSource = interaction, content = content)
        2 -> OutlinedButton(click, modifier.then(press), enabled = enabled, contentPadding = contentPadding, interactionSource = interaction, content = content)
        else -> FilledTonalButton(click, modifier.then(press), enabled = enabled, contentPadding = contentPadding, interactionSource = interaction, content = content)
    }
}

@Composable fun PlayfulButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding, content: @Composable RowScope.() -> Unit) =
    FeedbackButton(0, onClick, modifier, enabled, contentPadding, content)
@Composable fun PlayfulTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding, content: @Composable RowScope.() -> Unit) =
    FeedbackButton(1, onClick, modifier, enabled, contentPadding, content)
@Composable fun PlayfulOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding, content: @Composable RowScope.() -> Unit) =
    FeedbackButton(2, onClick, modifier, enabled, contentPadding, content)
@Composable fun PlayfulTonalButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding, content: @Composable RowScope.() -> Unit) =
    FeedbackButton(3, onClick, modifier, enabled, contentPadding, content)

@Composable fun PlayfulChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit) {
    val (interaction, press) = rememberUiPress()
    val sound = LocalUiClick.current
    FilterChip(selected, { onClick(); sound() }, label, modifier = press, interactionSource = interaction)
}

@Composable fun PlayfulSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val (interaction, press) = rememberUiPress()
    val sound = LocalUiClick.current
    Switch(checked, { onCheckedChange(it); sound() }, modifier = press, interactionSource = interaction)
}

@Composable fun PlayfulCard(onClick: () -> Unit, shape: androidx.compose.ui.graphics.Shape,
    color: androidx.compose.ui.graphics.Color, border: BorderStroke?, content: @Composable () -> Unit) {
    val (interaction, press) = rememberUiPress()
    val sound = LocalUiClick.current
    Surface(onClick = { onClick(); sound() }, modifier = press, shape = shape, color = color,
        border = border, interactionSource = interaction, content = content)
}
