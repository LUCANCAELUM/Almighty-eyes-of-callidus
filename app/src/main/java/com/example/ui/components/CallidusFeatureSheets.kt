package com.example.ui.components

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.credentials.CredentialManager
import kotlin.math.cos
import kotlin.math.sin
import com.example.R
import com.example.data.firebase.CallidusAiModel
import com.example.data.firebase.DonationRecord
import com.example.data.firebase.IntelChatMessage
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.onGoogleSignInClicked
import com.example.domain.model.CctvCameraTarget
import com.example.domain.model.EarthquakeTarget
import com.example.domain.model.OpticMode
import com.example.domain.model.SeismicConnectionStatus
import com.example.domain.model.SeismicStreamTelemetry
import com.example.domain.model.WorldometersCategory
import com.example.domain.model.WorldometersStat
import com.example.ui.theme.CallidusCrimson
import com.example.ui.theme.CallidusCyan
import com.example.ui.theme.CallidusEmerald
import com.example.ui.theme.CallidusGold
import com.example.ui.theme.ObsidianPanel
import com.example.ui.theme.ObsidianPanelElevated
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONTokener

@Composable
fun Animated3DCallidusLogo(
    size: Dp = 144.dp,
    accent: Color = CallidusCyan,
    secondaryAccent: Color = CallidusGold,
    dramaticEntrance: Boolean = true,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    val infinite = rememberInfiniteTransition(label = "callidus_logo_3d")

    val orbitPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_phase"
    )
    val counterPhase by infinite.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_phase"
    )
    val pulseGlow by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    val introScale = remember { Animatable(if (dramaticEntrance) 0.18f else 1f) }
    val introYaw = remember { Animatable(if (dramaticEntrance) -210f else 0f) }
    val introPitch = remember { Animatable(if (dramaticEntrance) 46f else 0f) }

    LaunchedEffect(dramaticEntrance) {
        if (dramaticEntrance) {
            introScale.animateTo(
                1f,
                animationSpec = spring(dampingRatio = 0.56f, stiffness = Spring.StiffnessLow)
            )
        }
    }
    LaunchedEffect(dramaticEntrance) {
        if (dramaticEntrance) {
            introYaw.animateTo(
                0f,
                animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessVeryLow)
            )
        }
    }
    LaunchedEffect(dramaticEntrance) {
        if (dramaticEntrance) {
            introPitch.animateTo(
                0f,
                animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessLow)
            )
        }
    }

    val yawRad = Math.toRadians(orbitPhase.toDouble())
    val liveRotY = (sin(yawRad) * 18.0).toFloat() + introYaw.value
    val liveRotX = (cos(yawRad * 0.7) * 14.0).toFloat() + introPitch.value

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = introScale.value
                scaleY = introScale.value
                rotationY = liveRotY
                rotationX = liveRotX
                cameraDistance = 11f * density
            },
        contentAlignment = Alignment.Center
    ) {
        // Multi-plane 3D Gyroscopic Orbital Rings & Holographic Flare Sweep
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = this.size.width * 0.5f
            val cy = this.size.height * 0.5f
            val radius = this.size.minDimension * 0.48f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.36f * pulseGlow),
                        secondaryAccent.copy(alpha = 0.14f * pulseGlow),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = radius * 1.15f
                ),
                radius = radius * 1.15f,
                center = Offset(cx, cy)
            )

            // Outer tilted 3D gyro ring (Cyan)
            rotate(degrees = orbitPhase, pivot = Offset(cx, cy)) {
                drawArc(
                    color = accent.copy(alpha = 0.85f),
                    startAngle = 0f,
                    sweepAngle = 255f,
                    useCenter = false,
                    topLeft = Offset(cx - radius, cy - radius * 0.52f),
                    size = Size(radius * 2f, radius * 1.04f),
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Inner counter-rotating 3D gyro ring (Gold)
            rotate(degrees = counterPhase, pivot = Offset(cx, cy)) {
                drawArc(
                    color = secondaryAccent.copy(alpha = 0.85f),
                    startAngle = 35f,
                    sweepAngle = 210f,
                    useCenter = false,
                    topLeft = Offset(cx - radius * 0.84f, cy - radius * 0.38f),
                    size = Size(radius * 1.68f, radius * 0.76f),
                    style = Stroke(width = 2.1.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Equatorial perimeter reticle ring + orbiting photon node
            drawCircle(
                color = accent.copy(alpha = 0.35f + 0.3f * pulseGlow),
                radius = radius * 0.72f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.4.dp.toPx())
            )
            val nodeRad = Math.toRadians(orbitPhase.toDouble() * 1.5)
            val nx = cx + cos(nodeRad).toFloat() * radius * 0.72f
            val ny = cy + sin(nodeRad).toFloat() * radius * 0.72f
            drawCircle(
                color = secondaryAccent,
                radius = 3.8.dp.toPx(),
                center = Offset(nx, ny)
            )
        }

        // Core 3D App Emblem with Counter-Depth Parallax
        Image(
            painter = painterResource(id = R.drawable.ic_callidus_foreground_1795036339722),
            contentDescription = "Almighty Eyes of Callidus 3D Emblem",
            modifier = Modifier
                .size(size * 0.62f)
                .graphicsLayer {
                    rotationY = -liveRotY * 0.35f
                    rotationX = -liveRotX * 0.35f
                    scaleX = 0.96f + 0.05f * pulseGlow
                    scaleY = 0.96f + 0.05f * pulseGlow
                    cameraDistance = 12f * density
                }
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun CallidusAuthScreen(
    onAuthSuccess: () -> Unit,
    onContinueWithoutSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = { },
            scope = scope
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF02040A), Color(0xFF081426), ObsidianVoid)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            color = ObsidianPanelElevated,
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.3.dp, CallidusCyan.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Animated3DCallidusLogo(
                    size = 154.dp,
                    accent = CallidusCyan,
                    secondaryAccent = CallidusGold,
                    dramaticEntrance = true
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "THE ALMIGHTY EYES OF CALLIDUS",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "3D Orbital Telemetry • Live CCTV • Maritime AIS • Three.js • Gemini AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = CallidusCyan,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Launch straight into the live global command deck, or optionally sign in with Google to sync bookmarks, AI briefs, and Paybill 522522 records to the cloud.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))

                if (errorMessage != null) {
                    Surface(
                        color = CallidusCrimson.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CallidusCrimson.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CallidusCrimson,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = onContinueWithoutSignIn,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("continue_guest_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CallidusCyan,
                        contentColor = ObsidianVoid
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "ENTER COMMAND DECK",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = { isLoading = false }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("google_sign_in_button"),
                    border = BorderStroke(1.2.dp, CallidusGold.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = CallidusGold,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, tint = CallidusGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "SIGN IN WITH GOOGLE (OPTIONAL)",
                            style = MaterialTheme.typography.labelMedium,
                            color = CallidusGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GeminiChatbotSheet(
    visible: Boolean,
    messages: List<IntelChatMessage>,
    streamingReply: String?,
    isSending: Boolean,
    errorText: String?,
    selectedModel: CallidusAiModel,
    targetLat: Double,
    targetLon: Double,
    accent: Color,
    onSelectModel: (CallidusAiModel) -> Unit,
    onSendPrompt: (String) -> Unit,
    onClearHistory: () -> Unit,
    onOpenInMaps: (Double, Double) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val density = LocalDensity.current.density

    LaunchedEffect(messages.size, streamingReply) {
        val total = messages.size + (if (streamingReply != null) 1 else 0)
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .widthIn(max = 620.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(10.dp)
                .graphicsLayer {
                    rotationX = 2f
                    cameraDistance = 14f * density
                },
            color = ObsidianPanelElevated,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.3.dp, accent.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accent)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("CALLIDUS GEMINI ANALYST", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                text = String.format("Maps/OSINT Grounded • %.2f°, %.2f°", targetLat, targetLon),
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                    }
                    Row {
                        IconButton(onClick = onClearHistory, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear History", tint = TextSecondary)
                        }
                        IconButton(onClick = onClose, modifier = Modifier.size(44.dp).testTag("close_chat_button")) {
                            Icon(Icons.Default.Close, contentDescription = "Close Chat", tint = TextSecondary)
                        }
                    }
                }

                // Model selector chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CallidusAiModel.entries.forEach { model ->
                        val active = model == selectedModel
                        FilterChip(
                            selected = active,
                            onClick = { onSelectModel(model) },
                            label = { Text(model.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent.copy(alpha = 0.24f),
                                selectedLabelColor = accent
                            )
                        )
                    }
                }

                // Quick prompts
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickPrompts = listOf(
                        "Brief me on current coordinates & nearby airspace",
                        "Notable landmarks & Google Maps places near target",
                        "Assess seismic & weather hazards here"
                    )
                    quickPrompts.forEach { qp ->
                        Surface(
                            onClick = { if (!isSending) onSendPrompt(qp) },
                            color = Color(0x55060D1A),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.75.dp, accent.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = qp,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Conversation thread
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    if (messages.isEmpty() && streamingReply == null) {
                        item {
                            Text(
                                text = "Ask Callidus AI about live flights, orbital satellites, nearby landmarks, or seismic activity around your 3D crosshair.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    items(messages, key = { it.id.ifEmpty { "${it.role}_${it.content.hashCode()}" } }) { msg ->
                        val isUser = msg.role == "user"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                color = if (isUser) accent.copy(alpha = 0.2f) else Color(0x8008111F),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    0.75.dp,
                                    if (isUser) accent.copy(alpha = 0.55f) else TextMuted.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.widthIn(max = 310.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (isUser) "OPERATOR" else "CALLIDUS (${msg.modelId})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isUser) accent else CallidusGold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(msg.content, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                    if (!isUser) {
                                        Spacer(Modifier.height(6.dp))
                                        Surface(
                                            onClick = { onOpenInMaps(msg.targetLat, msg.targetLon) },
                                            color = accent.copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Map, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("View Area in Google Maps", style = MaterialTheme.typography.labelSmall, color = accent)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (streamingReply != null) {
                        item(key = "streaming_msg") {
                            Surface(
                                color = Color(0x8008111F),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(0.75.dp, CallidusGold.copy(alpha = 0.5f)),
                                modifier = Modifier.widthIn(max = 310.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("CALLIDUS (${selectedModel.modelId}) • STREAMING", style = MaterialTheme.typography.labelSmall, color = CallidusGold)
                                    Spacer(Modifier.height(4.dp))
                                    Text(streamingReply, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                }
                            }
                        }
                    }
                }

                if (errorText != null) {
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.labelSmall,
                        color = CallidusCrimson,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Input Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gemini_chat_input"),
                        placeholder = { Text("Ask Callidus AI...", style = MaterialTheme.typography.bodyMedium) },
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = accent.copy(alpha = 0.35f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val trimmed = inputText.trim()
                            if (trimmed.isNotEmpty() && !isSending) {
                                inputText = ""
                                onSendPrompt(trimmed)
                            }
                        },
                        enabled = !isSending,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.22f))
                            .testTag("send_chat_button")
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = accent, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DonationPaybillSheet(
    visible: Boolean,
    donations: List<DonationRecord>,
    sqlSnapshotCount: Int,
    isAuthenticated: Boolean,
    accent: Color,
    onRecordDonation: (amountKes: Long, phone: String, reference: String) -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedAmount by remember { mutableStateOf("500") }
    var phoneInput by remember { mutableStateOf("") }
    var refInput by remember { mutableStateOf("") }
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    fun copyToClipboard(label: String, value: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, value))
        copiedNotice = "$label copied: $value"
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.84f)
                .widthIn(max = 600.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(10.dp),
            color = ObsidianPanelElevated,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.3.dp, CallidusGold.copy(alpha = 0.65f))
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = CallidusGold)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("SUPPORT CALLIDUS", style = MaterialTheme.typography.headlineMedium, color = CallidusGold)
                                Text("M-Pesa Lipa na M-Pesa Paybill Portal", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                        IconButton(onClick = onClose, modifier = Modifier.size(48.dp).testTag("close_donations_button")) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }
                }

                // Official Paybill & Account Card
                item {
                    Surface(
                        color = Color(0x80081220),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.2.dp, CallidusGold.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("PAYBILL BUSINESS NUMBER", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text("522522", style = MaterialTheme.typography.displayLarge, color = CallidusGold)
                                }
                                OutlinedButton(
                                    onClick = { copyToClipboard("Paybill", DonationRecord.MPESA_PAYBILL) },
                                    modifier = Modifier.testTag("copy_paybill_button"),
                                    border = BorderStroke(1.dp, CallidusGold)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CallidusGold, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("COPY", color = CallidusGold, style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("ACCOUNT NUMBER", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text("1355643775", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                                }
                                OutlinedButton(
                                    onClick = { copyToClipboard("Account", DonationRecord.MPESA_ACCOUNT_NUMBER) },
                                    modifier = Modifier.testTag("copy_account_button"),
                                    border = BorderStroke(1.dp, accent)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("COPY", color = accent, style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            if (copiedNotice != null) {
                                Text(copiedNotice.orEmpty(), style = MaterialTheme.typography.labelSmall, color = CallidusEmerald)
                            }

                            Text(
                                text = "M-Pesa Steps: M-Pesa → Lipa na M-Pesa → Pay Bill → Business No: 522522 → Account No: 1355643775 → Amount → PIN.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Log Contribution to Cloud Firestore
                item {
                    Text("LOG YOUR CONTRIBUTION (CLOUD SYNCED)", style = MaterialTheme.typography.labelMedium, color = accent)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("100", "500", "1000", "2500", "5000").forEach { amt ->
                            FilterChip(
                                selected = selectedAmount == amt,
                                onClick = { selectedAmount = amt },
                                label = { Text("KES $amt", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = selectedAmount,
                        onValueChange = { selectedAmount = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Amount (KES)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("donation_amount_input")
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("M-Pesa Phone (optional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = refInput,
                            onValueChange = { refInput = it.uppercase() },
                            label = { Text("M-Pesa Code (optional)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val amt = selectedAmount.toLongOrNull() ?: 0L
                            if (amt in 10L..500_000L) {
                                onRecordDonation(amt, phoneInput, refInput)
                                refInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("record_donation_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CallidusGold, contentColor = ObsidianVoid),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("SAVE DONATION RECORD", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }

                if (donations.isNotEmpty()) {
                    item {
                        Text("YOUR RECORDED DONATIONS (${donations.size})", style = MaterialTheme.typography.labelMedium, color = CallidusEmerald)
                    }
                    items(donations, key = { it.id.ifEmpty { "${it.amountKes}_${it.referenceCode}" } }) { don ->
                        Surface(
                            color = Color(0x55060D1A),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.75.dp, CallidusGold.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("KES ${don.amountKes} → Paybill ${don.paybill}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text("Acct: ${don.accountNumber} • ${don.referenceCode.ifEmpty { "Pledge" }}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CallidusEmerald, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(don.status, style = MaterialTheme.typography.labelSmall, color = CallidusEmerald)
                                }
                            }
                        }
                    }
                }

                // Local SQL Cache & Account Sign-Out
                item {
                    Surface(
                        color = Color(0x55060D1A),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.75.dp, accent.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Storage, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Local Room SQL Snapshots: $sqlSnapshotCount", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                            }
                            if (isAuthenticated) {
                                OutlinedButton(
                                    onClick = onSignOut,
                                    modifier = Modifier.testTag("sign_out_button"),
                                    border = BorderStroke(1.dp, CallidusCrimson.copy(alpha = 0.6f))
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = CallidusCrimson, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("SIGN OUT", color = CallidusCrimson, style = MaterialTheme.typography.labelSmall)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onSignIn,
                                    modifier = Modifier.testTag("optional_sign_in_button"),
                                    border = BorderStroke(1.dp, CallidusGold.copy(alpha = 0.8f))
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, tint = CallidusGold, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("GOOGLE SYNC", color = CallidusGold, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun opticColorFilter(opticMode: OpticMode): ColorFilter? {
    val matrix = when (opticMode) {
        OpticMode.EO_TACTICAL -> return null
        OpticMode.FLIR_THERMAL -> ColorMatrix(
            floatArrayOf(
                1.4f, 0.35f, 0.1f, 0f, 20f,
                0.25f, 0.7f, 0.15f, 0f, -12f,
                0.05f, 0.1f, 0.55f, 0f, -20f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        OpticMode.NVG_PHOSPHOR -> ColorMatrix(
            floatArrayOf(
                0.1f, 0.25f, 0.05f, 0f, -10f,
                0.35f, 1.35f, 0.25f, 0f, 16f,
                0.1f, 0.35f, 0.15f, 0f, -6f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        OpticMode.CRT_MONITOR -> ColorMatrix(
            floatArrayOf(
                0.25f, 0.35f, 0.2f, 0f, -8f,
                0.2f, 1.0f, 0.45f, 0f, 4f,
                0.35f, 0.5f, 1.35f, 0f, 14f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        OpticMode.NOIR_SURVEILLANCE -> run {
            val c = 1.35f
            val r = 0.2126f * c
            val g = 0.7152f * c
            val b = 0.0722f * c
            val t = (-0.5f * c + 0.5f) * 255f
            ColorMatrix(
                floatArrayOf(
                    r, g, b, 0f, t,
                    r, g, b, 0f, t,
                    r, g, b, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        OpticMode.ARCTIC_SNOW -> ColorMatrix(
            floatArrayOf(
                0.95f, 0.1f, 0.2f, 0f, 16f,
                0.08f, 1.05f, 0.2f, 0f, 18f,
                0.15f, 0.2f, 1.25f, 0f, 24f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        OpticMode.WIREFRAME_HOLO -> ColorMatrix(
            floatArrayOf(
                0.15f, 0.2f, 0.45f, 0f, -15f,
                0.2f, 0.9f, 0.9f, 0f, 8f,
                0.4f, 0.7f, 1.45f, 0f, 22f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        OpticMode.SOLAR_TERMINATOR -> ColorMatrix(
            floatArrayOf(
                1.25f, 0.2f, 0f, 0f, 8f,
                0.15f, 0.92f, 0.05f, 0f, -6f,
                0f, 0.15f, 1.15f, 0f, -8f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }
    return ColorFilter.colorMatrix(matrix)
}

@Composable
fun LiveCctvSnapshotBox(
    imageUrl: String,
    cameraName: String,
    opticMode: OpticMode,
    accent: Color,
    fetchSnapshotBytes: suspend (String) -> ByteArray?,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(imageUrl) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(imageUrl) { mutableStateOf(true) }
    var failed by remember(imageUrl) { mutableStateOf(false) }
    var refreshTick by remember(imageUrl) { mutableIntStateOf(0) }

    LaunchedEffect(imageUrl, refreshTick) {
        while (true) {
            isLoading = bitmap == null
            val bytes = fetchSnapshotBytes(imageUrl)
            if (bytes != null && bytes.isNotEmpty()) {
                val decoded = withContext(Dispatchers.Default) {
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
                if (decoded != null) {
                    bitmap = decoded
                    failed = false
                } else if (bitmap == null) {
                    failed = true
                }
            } else if (bitmap == null) {
                failed = true
            }
            isLoading = false
            delay(5_000L)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF040811)),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = "Live CCTV feed for $cameraName",
                contentScale = ContentScale.Crop,
                colorFilter = opticColorFilter(opticMode),
                modifier = Modifier.fillMaxSize()
            )
        } else if (isLoading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = accent, strokeWidth = 2.dp)
                Spacer(Modifier.height(6.dp))
                Text("ACQUIRING LIVE FEED...", style = MaterialTheme.typography.labelSmall, color = accent)
            }
        } else if (failed) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                Text("FEED TEMPORARILY UNAVAILABLE", style = MaterialTheme.typography.labelSmall, color = CallidusCrimson)
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { refreshTick++ },
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("RETRY", style = MaterialTheme.typography.labelSmall, color = accent)
                }
            }
        }

        // Optical scanline & target reticle overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (opticMode == OpticMode.CRT_MONITOR || opticMode == OpticMode.WIREFRAME_HOLO) {
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = accent.copy(alpha = 0.14f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.1f
                    )
                    y += 5f
                }
            }
            val cx = size.width * 0.5f
            val cy = size.height * 0.5f
            val c = accent.copy(alpha = 0.55f)
            drawLine(c, Offset(cx - 12f, cy), Offset(cx - 4f, cy), 1.3f)
            drawLine(c, Offset(cx + 4f, cy), Offset(cx + 12f, cy), 1.3f)
            drawLine(c, Offset(cx, cy - 12f), Offset(cx, cy - 4f), 1.3f)
            drawLine(c, Offset(cx, cy + 4f), Offset(cx, cy + 12f), 1.3f)
        }

        Surface(
            color = Color(0xBB04070D),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(0.75.dp, accent.copy(alpha = 0.55f)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Text(
                text = "REC • ${opticMode.code}",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }

        IconButton(
            onClick = { refreshTick++ },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0x9904070D))
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh Snapshot", tint = accent, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun ThreeJsTacticalViewport(
    targetLat: Double,
    targetLon: Double,
    theaterName: String,
    opticMode: OpticMode,
    accent: Color,
    modifier: Modifier = Modifier
) {
    ThreeJsWebViewWrapper(
        targetLat = targetLat,
        targetLon = targetLon,
        theaterName = theaterName,
        opticMode = opticMode,
        accent = accent,
        modifier = modifier
    )
}

private data class WorldometersMeta(
    val label: String,
    val category: WorldometersCategory
)

private val WORLDOMETERS_KEY_MAP: Map<String, WorldometersMeta> = mapOf(
    "current_population" to WorldometersMeta("Current World Population", WorldometersCategory.POPULATION),
    "births_today" to WorldometersMeta("Births Today", WorldometersCategory.POPULATION),
    "births_this_year" to WorldometersMeta("Births This Year", WorldometersCategory.POPULATION),
    "dth1s_today" to WorldometersMeta("Deaths Today", WorldometersCategory.POPULATION),
    "dth1s_this_year" to WorldometersMeta("Deaths This Year", WorldometersCategory.POPULATION),
    "absolute_growth" to WorldometersMeta("Net Population Growth Today", WorldometersCategory.POPULATION),
    "absolute_growth_year" to WorldometersMeta("Net Population Growth This Year", WorldometersCategory.POPULATION),
    "public_healthcare_expenditure_today" to WorldometersMeta("Public Healthcare Expenditure Today ($)", WorldometersCategory.GOVERNMENT_ECON),
    "public_education_expenditure_today" to WorldometersMeta("Public Education Expenditure Today ($)", WorldometersCategory.GOVERNMENT_ECON),
    "public_military_expenditure_today" to WorldometersMeta("Public Military Expenditure Today ($)", WorldometersCategory.GOVERNMENT_ECON),
    "cars_produced_this_year" to WorldometersMeta("Cars Produced This Year", WorldometersCategory.GOVERNMENT_ECON),
    "bicycles_produced_this_year" to WorldometersMeta("Bicycles Produced This Year", WorldometersCategory.GOVERNMENT_ECON),
    "computers_produced_this_year" to WorldometersMeta("Computers Produced This Year", WorldometersCategory.GOVERNMENT_ECON),
    "new_books_this_year" to WorldometersMeta("New Book Titles Published This Year", WorldometersCategory.SOCIETY_MEDIA),
    "newspapers_circulated_today" to WorldometersMeta("Newspapers Circulated Today", WorldometersCategory.SOCIETY_MEDIA),
    "tv_sets_sold_today" to WorldometersMeta("TV Sets Sold Worldwide Today", WorldometersCategory.SOCIETY_MEDIA),
    "cellular_phones_sold_today" to WorldometersMeta("Cellular Phones Sold Today", WorldometersCategory.SOCIETY_MEDIA),
    "money_spent_videogames_today" to WorldometersMeta("Money Spent on Video Games Today ($)", WorldometersCategory.SOCIETY_MEDIA),
    "internet_users" to WorldometersMeta("Internet Users in the World", WorldometersCategory.SOCIETY_MEDIA),
    "emails_sent_today" to WorldometersMeta("Emails Sent Today", WorldometersCategory.SOCIETY_MEDIA),
    "blog_posts_written_today" to WorldometersMeta("Blog Posts Written Today", WorldometersCategory.SOCIETY_MEDIA),
    "tweets_sent_today" to WorldometersMeta("Tweets Sent Today", WorldometersCategory.SOCIETY_MEDIA),
    "google_searches_today" to WorldometersMeta("Google Searches Today", WorldometersCategory.SOCIETY_MEDIA),
    "forest_loss_this_year" to WorldometersMeta("Forest Loss This Year (hectares)", WorldometersCategory.ENVIRONMENT),
    "land_lost_soil_erosion_this_year" to WorldometersMeta("Land Lost to Soil Erosion (hectares)", WorldometersCategory.ENVIRONMENT),
    "co2_emissions_this_year" to WorldometersMeta("CO2 Emissions This Year (tons)", WorldometersCategory.ENVIRONMENT),
    "desertification_this_year" to WorldometersMeta("Desertification This Year (hectares)", WorldometersCategory.ENVIRONMENT),
    "toxic_chemicals_released_this_year" to WorldometersMeta("Toxic Chemicals Released (tons)", WorldometersCategory.ENVIRONMENT),
    "undernourished" to WorldometersMeta("Undernourished People in the World", WorldometersCategory.FOOD_WATER),
    "overweight" to WorldometersMeta("Overweight People in the World", WorldometersCategory.FOOD_WATER),
    "obese" to WorldometersMeta("Obese People in the World", WorldometersCategory.FOOD_WATER),
    "people_died_hunger_today" to WorldometersMeta("People Who Died of Hunger Today", WorldometersCategory.FOOD_WATER),
    "money_spent_obesity_usa_today" to WorldometersMeta("Spent on Obesity Diseases USA Today ($)", WorldometersCategory.FOOD_WATER),
    "money_spent_weight_loss_usa_today" to WorldometersMeta("Spent on Weight Loss Programs USA Today ($)", WorldometersCategory.FOOD_WATER),
    "water_used_this_year" to WorldometersMeta("Water Used This Year (million L)", WorldometersCategory.FOOD_WATER),
    "deaths_water_related_this_year" to WorldometersMeta("Water-Related Deaths This Year", WorldometersCategory.FOOD_WATER),
    "no_access_safe_drinking_water" to WorldometersMeta("People Without Safe Drinking Water", WorldometersCategory.FOOD_WATER),
    "energy_used_today" to WorldometersMeta("Energy Used Today (MWh)", WorldometersCategory.ENERGY),
    "non_renewable_energy_today" to WorldometersMeta("From Non-Renewable Sources Today (MWh)", WorldometersCategory.ENERGY),
    "renewable_energy_today" to WorldometersMeta("From Renewable Sources Today (MWh)", WorldometersCategory.ENERGY),
    "solar_energy_striking_earth_today" to WorldometersMeta("Solar Energy Striking Earth Today (MWh)", WorldometersCategory.ENERGY),
    "oil_pumped_today" to WorldometersMeta("Oil Pumped Today (barrels)", WorldometersCategory.ENERGY),
    "oil_left" to WorldometersMeta("Oil Left (barrels)", WorldometersCategory.ENERGY),
    "days_to_end_of_oil" to WorldometersMeta("Days to the End of Oil", WorldometersCategory.ENERGY),
    "gas_left" to WorldometersMeta("Natural Gas Left (boe)", WorldometersCategory.ENERGY),
    "days_to_end_of_gas" to WorldometersMeta("Days to the End of Natural Gas", WorldometersCategory.ENERGY),
    "coal_left" to WorldometersMeta("Coal Left (boe)", WorldometersCategory.ENERGY),
    "days_to_end_of_coal" to WorldometersMeta("Days to the End of Coal", WorldometersCategory.ENERGY),
    "cd_deaths_this_year" to WorldometersMeta("Communicable Disease Deaths This Year", WorldometersCategory.HEALTH),
    "seasonal_flu_deaths_this_year" to WorldometersMeta("Seasonal Flu Deaths This Year", WorldometersCategory.HEALTH),
    "dth1s_under5_this_year" to WorldometersMeta("Deaths of Children Under 5 This Year", WorldometersCategory.HEALTH),
    "ab_this_year" to WorldometersMeta("Abortions This Year", WorldometersCategory.HEALTH),
    "dth1s_mothers_this_year" to WorldometersMeta("Deaths of Mothers During Birth This Year", WorldometersCategory.HEALTH),
    "hiv_infected" to WorldometersMeta("HIV/AIDS Infected People", WorldometersCategory.HEALTH),
    "hiv_deaths_this_year" to WorldometersMeta("Deaths Caused by HIV/AIDS This Year", WorldometersCategory.HEALTH),
    "cancer_deaths_this_year" to WorldometersMeta("Deaths Caused by Cancer This Year", WorldometersCategory.HEALTH),
    "malaria_deaths_this_year" to WorldometersMeta("Deaths Caused by Malaria This Year", WorldometersCategory.HEALTH),
    "cigarettes_smoked_today" to WorldometersMeta("Cigarettes Smoked Today", WorldometersCategory.HEALTH),
    "smoking_deaths_this_year" to WorldometersMeta("Deaths Caused by Smoking This Year", WorldometersCategory.HEALTH),
    "alcohol_deaths_this_year" to WorldometersMeta("Deaths Caused by Alcohol This Year", WorldometersCategory.HEALTH),
    "suicides_this_year" to WorldometersMeta("Suicides This Year", WorldometersCategory.HEALTH),
    "money_spent_illegal_drugs_this_year" to WorldometersMeta("Money Spent on Illegal Drugs ($)", WorldometersCategory.HEALTH),
    "traffic_deaths_this_year" to WorldometersMeta("Road Traffic Accident Fatalities This Year", WorldometersCategory.HEALTH)
)

private fun mapWorldometersStat(key: String, value: String, rawLabel: String): WorldometersStat {
    val known = WORLDOMETERS_KEY_MAP[key]
    if (known != null) {
        return WorldometersStat(key = key, label = known.label, value = value, category = known.category)
    }
    val lower = key.lowercase()
    val category = when {
        "pop" in lower || "birth" in lower || "growth" in lower -> WorldometersCategory.POPULATION
        "co2" in lower || "forest" in lower || "erosion" in lower || "desert" in lower || "toxic" in lower -> WorldometersCategory.ENVIRONMENT
        "oil" in lower || "gas" in lower || "coal" in lower || "energy" in lower || "solar" in lower -> WorldometersCategory.ENERGY
        "water" in lower || "hunger" in lower || "obese" in lower || "overweight" in lower || "undernourished" in lower -> WorldometersCategory.FOOD_WATER
        "internet" in lower || "email" in lower || "tweet" in lower || "google" in lower || "book" in lower || "phone" in lower -> WorldometersCategory.SOCIETY_MEDIA
        "expenditure" in lower || "produced" in lower -> WorldometersCategory.GOVERNMENT_ECON
        else -> WorldometersCategory.HEALTH
    }
    val cleanLabel = rawLabel.trim()
        .takeIf { it.length in 3..72 && it != value }
        ?: key.split("_").filter { it.isNotBlank() }.joinToString(" ") { part ->
            part.replaceFirstChar { ch -> ch.uppercase() }
        }
    return WorldometersStat(key = key, label = cleanLabel, value = value, category = category)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WorldometersLiveBridge(
    onStatsUpdated: (List<WorldometersStat>, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageLoaded by remember { mutableStateOf(false) }
    var retryToken by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.stopLoading()
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    LaunchedEffect(webViewRef, pageLoaded, retryToken) {
        val wv = webViewRef ?: return@LaunchedEffect
        val pollJs = """
            (function() {
                var nodes = document.querySelectorAll('.rts-counter[rel]');
                var out = [];
                for (var i = 0; i < nodes.length; i++) {
                    var el = nodes[i];
                    var rel = el.getAttribute('rel') || '';
                    var txt = (el.innerText || el.textContent || '').trim();
                    if (!rel || !txt || txt.toLowerCase().indexOf('retrieving') !== -1) continue;
                    var label = '';
                    var row = el.closest('.counter-item, .rts-counter-row, tr, div');
                    if (row) {
                        var lblEl = row.querySelector('.counter-title, .rts-counter-title, .counter-label, a');
                        if (lblEl && lblEl !== el) {
                            label = (lblEl.innerText || lblEl.textContent || '').trim();
                        }
                    }
                    out.push({ k: rel, v: txt, l: label });
                }
                return JSON.stringify(out);
            })();
        """.trimIndent()

        var ticksWithoutStats = 0
        while (true) {
            wv.evaluateJavascript(pollJs) { raw ->
                if (!raw.isNullOrBlank() && raw != "null") {
                    runCatching {
                        val decoded = JSONTokener(raw).nextValue()
                        val jsonString = (decoded as? String) ?: raw
                        if (jsonString.startsWith("[")) {
                            val arr = JSONArray(jsonString)
                            val parsed = ArrayList<WorldometersStat>(arr.length())
                            val seen = HashSet<String>()
                            for (i in 0 until arr.length()) {
                                val obj = arr.optJSONObject(i) ?: continue
                                val k = obj.optString("k").trim()
                                val v = obj.optString("v").trim()
                                val l = obj.optString("l").trim()
                                if (k.isNotEmpty() && v.isNotEmpty() && seen.add(k)) {
                                    parsed.add(mapWorldometersStat(k, v, l))
                                }
                            }
                            if (parsed.isNotEmpty()) {
                                ticksWithoutStats = 0
                                onStatsUpdated(parsed, "LIVE")
                            }
                        }
                    }
                }
            }
            ticksWithoutStats++
            if (ticksWithoutStats >= 18) {
                ticksWithoutStats = 0
                onStatsUpdated(emptyList(), "RETRYING")
                wv.loadUrl("https://www.worldometers.info/")
            }
            delay(1_000L)
        }
    }

    AndroidView(
        modifier = modifier
            .size(1.dp)
            .alpha(0.01f),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadsImagesAutomatically = false
                settings.blockNetworkImage = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        pageLoaded = true
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            onStatsUpdated(emptyList(), "RETRYING")
                            retryToken++
                        }
                    }
                }
                loadUrl("https://www.worldometers.info/")
                webViewRef = this
            }
        }
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WorldometersLiveSheet(
    visible: Boolean,
    stats: List<WorldometersStat>,
    status: String,
    accent: Color,
    onAskGemini: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<WorldometersCategory?>(null) }
    var showWebPortal by remember { mutableStateOf(false) }

    val filteredStats = remember(stats, selectedCategory) {
        val cat = selectedCategory
        if (cat == null) stats else stats.filter { it.category == cat }
    }
    val populationLead = remember(stats) {
        stats.firstOrNull { it.key == "current_population" }
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .widthIn(max = 640.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(10.dp),
            color = ObsidianPanelElevated,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.3.dp, accent.copy(alpha = 0.65f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = CallidusEmerald)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "WORLDOMETERS LIVE STREAM",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "worldometers.info • $status • ${stats.size} Live Counters",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (status == "LIVE") CallidusEmerald else CallidusGold
                            )
                        }
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_worldometers_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Worldometers", tint = TextSecondary)
                    }
                }

                // Mode Switch + Gemini Analyst Brief Trigger
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !showWebPortal,
                        onClick = { showWebPortal = false },
                        label = { Text("LIVE HUD MATRIX", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(alpha = 0.24f),
                            selectedLabelColor = accent
                        )
                    )
                    FilterChip(
                        selected = showWebPortal,
                        onClick = { showWebPortal = true },
                        label = { Text("WORLDOMETERS.INFO SITE", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CallidusGold.copy(alpha = 0.24f),
                            selectedLabelColor = CallidusGold
                        )
                    )
                    OutlinedButton(
                        onClick = {
                            val summaryLines = stats.take(8).joinToString(", ") { "${it.label}: ${it.value}" }
                            onAskGemini(
                                "Analyze today's real-time global indicators from worldometers.info ($summaryLines) and highlight key demographic, energy, and environmental trends."
                            )
                        },
                        enabled = stats.isNotEmpty(),
                        modifier = Modifier.height(36.dp).testTag("ask_gemini_worldometers_button"),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.7f)),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("GEMINI BRIEF", style = MaterialTheme.typography.labelSmall, color = accent)
                    }
                }

                if (showWebPortal) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
                        color = ObsidianVoid
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.builtInZoomControls = true
                                    settings.displayZoomControls = false
                                    webChromeClient = WebChromeClient()
                                    webViewClient = WebViewClient()
                                    loadUrl("https://www.worldometers.info/")
                                }
                            }
                        )
                    }
                } else {
                    // Category Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("ALL (${stats.size})", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CallidusEmerald.copy(alpha = 0.22f),
                                selectedLabelColor = CallidusEmerald
                            )
                        )
                        WorldometersCategory.entries.forEach { cat ->
                            val count = stats.count { it.category == cat }
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text("${cat.title} ($count)", style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accent.copy(alpha = 0.22f),
                                    selectedLabelColor = accent
                                )
                            )
                        }
                    }

                    if (filteredStats.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(20.dp)
                            ) {
                                CircularProgressIndicator(color = CallidusEmerald, strokeWidth = 2.5.dp)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "STREAMING REAL-TIME COUNTERS FROM WORLDOMETERS.INFO...",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accent,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Executing live counter telemetry directly from the Worldometers DOM. You can also open the full site view above.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            if (populationLead != null && (selectedCategory == null || selectedCategory == WorldometersCategory.POPULATION)) {
                                item(key = "hero_population") {
                                    Surface(
                                        color = Color(0x88071628),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.3.dp, CallidusEmerald.copy(alpha = 0.7f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "LIVE CURRENT WORLD POPULATION",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CallidusEmerald
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = populationLead.value,
                                                style = MaterialTheme.typography.headlineLarge,
                                                color = CallidusGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "REAL-TIME TICKER • WORLDOMETERS.INFO",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                }
                            }

                            items(
                                items = filteredStats.filterNot {
                                    it.key == "current_population" && (selectedCategory == null || selectedCategory == WorldometersCategory.POPULATION)
                                },
                                key = { it.key }
                            ) { stat ->
                                val catColor = when (stat.category) {
                                    WorldometersCategory.POPULATION -> CallidusEmerald
                                    WorldometersCategory.ENVIRONMENT -> Color(0xFFFF7043)
                                    WorldometersCategory.ENERGY -> CallidusGold
                                    WorldometersCategory.FOOD_WATER -> Color(0xFF4FC3F7)
                                    WorldometersCategory.SOCIETY_MEDIA -> accent
                                    WorldometersCategory.GOVERNMENT_ECON -> Color(0xFFBA68C8)
                                    WorldometersCategory.HEALTH -> CallidusCrimson
                                }
                                Surface(
                                    color = Color(0x66060D1A),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(0.9.dp, catColor.copy(alpha = 0.42f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                            Text(
                                                text = stat.category.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = catColor
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = stat.label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextPrimary
                                            )
                                        }
                                        Text(
                                            text = stat.value,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = catColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeismicMonitorSheet(
    visible: Boolean,
    earthquakes: List<EarthquakeTarget>,
    streamTelemetry: SeismicStreamTelemetry,
    accent: Color,
    onFlyToQuake: (EarthquakeTarget) -> Unit,
    onAskGemini: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filterMode by remember { mutableStateOf("ALL") }
    val pulseInfinite = rememberInfiniteTransition(label = "seismic_sheet_pulse")
    val liveDotAlpha by pulseInfinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "seismic_dot_alpha"
    )

    val filteredQuakes = remember(earthquakes, filterMode) {
        when (filterMode) {
            "LIVE" -> earthquakes.filter { it.isRealtimeAlert }
            "M25" -> earthquakes.filter { it.magnitude >= 2.5 }
            "M45" -> earthquakes.filter { it.magnitude >= 4.5 }
            "TSUNAMI" -> earthquakes.filter { it.tsunamiAlert }
            else -> earthquakes
        }
    }

    val statusColor = when (streamTelemetry.status) {
        SeismicConnectionStatus.LIVE_STREAM -> CallidusEmerald
        SeismicConnectionStatus.POLLING -> CallidusGold
        SeismicConnectionStatus.CONNECTING -> accent
        SeismicConnectionStatus.RECONNECTING -> CallidusCrimson
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .widthIn(max = 640.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(10.dp),
            color = ObsidianPanelElevated,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.3.dp, CallidusCrimson.copy(alpha = 0.65f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Sensors, contentDescription = null, tint = CallidusCrimson)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "REAL-TIME SEISMIC LISTENER",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(statusColor.copy(alpha = liveDotAlpha))
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${streamTelemetry.status.label} • EMSC WS + USGS 1H • ${earthquakes.size} Nodes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_seismic_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Seismic Monitor", tint = TextSecondary)
                    }
                }

                // Latest Live Tremor Highlight Banner
                streamTelemetry.latestLiveTremor?.let { latest ->
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        onClick = { onFlyToQuake(latest) },
                        color = CallidusCrimson.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CallidusGold.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "LATEST LIVE TREMOR • ${latest.sourceNetwork}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CallidusGold
                                )
                                Text(
                                    text = "M${String.format(java.util.Locale.US, "%.1f", latest.magnitude)} — ${latest.place}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "LOCATE 3D",
                                style = MaterialTheme.typography.labelSmall,
                                color = CallidusGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Filters + Gemini Seismologist Brief
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "ALL" to "ALL (${earthquakes.size})",
                        "LIVE" to "LIVE <20M",
                        "M25" to "M2.5+",
                        "M45" to "M4.5+",
                        "TSUNAMI" to "TSUNAMI"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = filterMode == key,
                            onClick = { filterMode = key },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CallidusCrimson.copy(alpha = 0.24f),
                                selectedLabelColor = CallidusCrimson
                            )
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val topSummary = earthquakes.take(6).joinToString("; ") {
                                "M${String.format(java.util.Locale.US, "%.1f", it.magnitude)} ${it.place} (${it.depthKm.toInt()}km)"
                            }
                            onAskGemini("Provide a global tectonic & seismic hazard briefing for these active tremors: $topSummary")
                        },
                        enabled = earthquakes.isNotEmpty(),
                        modifier = Modifier.height(36.dp).testTag("ask_gemini_seismic_button"),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.7f)),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("GEMINI BRIEF", style = MaterialTheme.typography.labelSmall, color = accent)
                    }
                }

                if (filteredQuakes.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (earthquakes.isEmpty()) "Listening for live global seismic tremors..." else "No tremors match filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                } else {
                    val nowMs = System.currentTimeMillis()
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filteredQuakes, key = { it.id }) { quake ->
                            val minsAgo = ((nowMs - quake.timeMs).coerceAtLeast(0L) / 60_000L).toInt()
                            val timeAgoText = when {
                                minsAgo < 1 -> "JUST NOW"
                                minsAgo < 60 -> "${minsAgo}m ago"
                                else -> "${minsAgo / 60}h ${minsAgo % 60}m ago"
                            }
                            val severityColor = when {
                                quake.tsunamiAlert -> CallidusGold
                                quake.magnitude >= 5.0 -> CallidusCrimson
                                quake.isRealtimeAlert -> Color(0xFFFF7043)
                                else -> accent
                            }

                            Surface(
                                onClick = { onFlyToQuake(quake) },
                                color = Color(0x66060D1A),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, severityColor.copy(alpha = if (quake.isRealtimeAlert) 0.75f else 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = severityColor.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, severityColor),
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = String.format(java.util.Locale.US, "M%.1f", quake.magnitude),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = severityColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = quake.place,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${timeAgoText} • Depth ${String.format(java.util.Locale.US, "%.1f", quake.depthKm)} km • ${quake.sourceNetwork}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (quake.isRealtimeAlert) CallidusGold else TextSecondary
                                        )
                                        if (quake.tsunamiAlert) {
                                            Text(
                                                text = "TSUNAMI WATCH ADVISORY",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CallidusGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Icon(Icons.Default.ViewInAr, contentDescription = "Fly to 3D Node", tint = severityColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


