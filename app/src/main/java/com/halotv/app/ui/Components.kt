package com.halotv.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.halotv.app.data.Channel
import com.halotv.app.ui.theme.HaloColors
import com.halotv.app.ui.theme.Inter
import kotlinx.coroutines.delay

val CardWidth = 196.dp
private val CardShape = RoundedCornerShape(14.dp)

/** tvOS-style tile: lifts and glows when focused, logo on a soft gradient. */
@Composable
fun ChannelCard(
    channel: Channel,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier.width(CardWidth),
    onFocused: () -> Unit = {},
    focusRequester: FocusRequester? = null,
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier) {
        Card(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused()
                },
            shape = CardDefaults.shape(CardShape),
            colors = CardDefaults.colors(
                containerColor = HaloColors.Surface,
                focusedContainerColor = HaloColors.SurfaceFocused,
            ),
            scale = CardDefaults.scale(focusedScale = 1.1f),
            border = CardDefaults.border(
                focusedBorder = Border(BorderStroke(2.dp, Color.White.copy(alpha = 0.85f)), shape = CardShape),
            ),
            glow = CardDefaults.glow(
                focusedGlow = Glow(elevationColor = Color.Black, elevation = 24.dp),
            ),
        ) {
            ChannelArt(channel, isFavorite)
        }
        Text(
            text = channel.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (focused) HaloColors.TextPrimary else HaloColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = if (focused) 16.dp else 10.dp, start = 2.dp),
        )
    }
}

@Composable
fun ChannelArt(channel: Channel, isFavorite: Boolean = false, logoPadding: Dp = 18.dp) {
    val context = LocalContext.current
    var failed by remember(channel.logo) { mutableStateOf(false) }
    Box(
        Modifier.fillMaxSize().background(HaloColors.CardGradient),
        contentAlignment = Alignment.Center,
    ) {
        if (channel.logo.isNotBlank() && !failed) {
            AsyncImage(
                model = remember(channel.logo) {
                    ImageRequest.Builder(context)
                        .data(channel.logo)
                        .size(320, 180)       // decode small: saves RAM on the Fire Stick
                        .crossfade(150)
                        .build()
                },
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize().padding(horizontal = logoPadding + 4.dp, vertical = logoPadding),
            )
        } else {
            Text(
                text = initials(channel.name),
                fontFamily = Inter,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = HaloColors.TextSecondary,
            )
        }
        if (isFavorite) {
            Text(
                "★",
                color = HaloColors.Gold,
                fontSize = 15.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            )
        }
    }
}

fun initials(name: String): String =
    name.split(' ', '-', '_', '|', ':')
        .filter { it.isNotBlank() && it[0].isLetterOrDigit() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "TV" }

@Composable
fun HaloButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = ButtonDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ButtonDefaults.colors(
            containerColor = HaloColors.Surface,
            contentColor = HaloColors.TextPrimary,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 10.dp))
    }
}

@Composable
fun Spinner(modifier: Modifier = Modifier, diameter: Dp = 44.dp, color: Color = Color.White) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "angle",
    )
    Canvas(modifier.size(diameter)) {
        val stroke = 4.dp.toPx()
        rotate(angle) {
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 280f,
                useCenter = false,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

/**
 * D-pad friendly text field: focusing it does NOT pop the keyboard
 * (so you can scroll past it). Press OK to start typing, Done to finish.
 */
@Composable
fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onDone: () -> Unit = {},
) {
    var editing by remember { mutableStateOf(false) }
    var fieldHadFocus by remember { mutableStateOf(false) }
    val fieldFocus = remember { FocusRequester() }
    val surfaceFocus = remember { FocusRequester() }
    val shape = RoundedCornerShape(12.dp)

    Surface(
        onClick = { editing = true },
        modifier = modifier.height(58.dp).focusRequester(surfaceFocus),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = HaloColors.Surface,
            contentColor = HaloColors.TextPrimary,
            focusedContainerColor = HaloColors.SurfaceFocused,
            focusedContentColor = HaloColors.TextPrimary,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = shape),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentAlignment = Alignment.CenterStart) {
            if (editing) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(color = HaloColors.TextPrimary, fontSize = 18.sp, fontFamily = Inter),
                    cursorBrush = SolidColor(Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = keyboardType),
                    keyboardActions = KeyboardActions(onDone = {
                        editing = false
                        runCatching { surfaceFocus.requestFocus() }
                        onDone()
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(fieldFocus)
                        .onFocusChanged {
                            if (it.isFocused) fieldHadFocus = true
                            else if (fieldHadFocus) {
                                fieldHadFocus = false
                                editing = false
                            }
                        },
                )
            } else {
                Text(
                    text = value.ifEmpty { placeholder },
                    color = if (value.isEmpty()) HaloColors.TextSecondary else HaloColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    LaunchedEffect(editing) {
        if (editing) {
            delay(50)
            runCatching { fieldFocus.requestFocus() }
        }
    }
}
