package com.example.ui.components

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.domain.model.OpticMode
import com.example.ui.theme.CallidusCrimson
import com.example.ui.theme.CallidusEmerald
import com.example.ui.theme.CallidusGold
import com.example.ui.theme.ObsidianPanel
import com.example.ui.theme.ObsidianPanelElevated
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

enum class ThreeJsSceneMode(val jsKey: String, val label: String) {
    BASIC("basic", "BASIC 3D"),
    RECON_SAT("sat", "RECON SAT"),
    INTERCEPTOR("jet", "INTERCEPTOR"),
    VIEWSHED("globe", "3D VIEWSHED")
}

data class ThreeJsRenderStatus(
    val engineName: String = "INITIALIZING WEBGL...",
    val gpuInfo: String = "",
    val fps: Int = 0,
    val lastError: String? = null,
    val isReady: Boolean = false
)

class ThreeJsSceneController {
    internal var webView: WebView? = null

    fun setSceneMode(mode: ThreeJsSceneMode) {
        webView?.evaluateJavascript("window.setSceneMode && window.setSceneMode('${mode.jsKey}');", null)
    }

    fun setWireframe(enabled: Boolean) {
        webView?.evaluateJavascript("window.setWireframeMode && window.setWireframeMode($enabled);", null)
    }

    fun setAutoRotate(enabled: Boolean) {
        webView?.evaluateJavascript("window.setAutoRotate && window.setAutoRotate($enabled);", null)
    }

    fun resetCamera() {
        webView?.evaluateJavascript("window.resetThreeCamera && window.resetThreeCamera();", null)
    }

    fun setOpticMode(opticMode: OpticMode) {
        webView?.evaluateJavascript("window.setOpticMode && window.setOpticMode('${opticMode.name}');", null)
    }

    fun setTheaterTarget(lat: Double, lon: Double, theaterName: String) {
        val safeName = theaterName.replace("'", "")
        webView?.evaluateJavascript(
            String.format(
                Locale.US,
                "window.setTheaterTarget && window.setTheaterTarget(%.5f, %.5f, '%s');",
                lat,
                lon,
                safeName
            ),
            null
        )
    }

    fun reload() {
        webView?.reload()
    }
}

/**
 * Reusable Compose AndroidView WebView wrapper that initializes Three.js with
 * hardware acceleration (`LAYER_TYPE_HARDWARE`), HTTPS base-URL origin loading,
 * Lifecycle resume/pause synchronization, and live WebGL GPU/FPS diagnostics.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ThreeJsWebViewWrapper(
    targetLat: Double,
    targetLon: Double,
    theaterName: String,
    opticMode: OpticMode,
    accent: Color,
    modifier: Modifier = Modifier,
    initialSceneMode: ThreeJsSceneMode = ThreeJsSceneMode.BASIC
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { ThreeJsSceneController() }

    var sceneMode by remember { mutableStateOf(initialSceneMode) }
    var wireframe by remember { mutableStateOf(false) }
    var autoRotate by remember { mutableStateOf(true) }
    var renderStatus by remember { mutableStateOf(ThreeJsRenderStatus()) }
    var reloadNonce by remember { mutableIntStateOf(0) }

    val assetHtml = remember(context) {
        runCatching {
            context.assets.open("callidus_threejs_scene.html").bufferedReader().use { it.readText() }
        }.getOrElse {
            "<html><body style='background:#04070D;color:#00E5FF;font-family:monospace;padding:24px;'>Three.js scene asset unavailable</body></html>"
        }
    }

    LaunchedEffect(targetLat, targetLon, theaterName) {
        controller.setTheaterTarget(targetLat, targetLon, theaterName)
    }

    LaunchedEffect(opticMode) {
        controller.setOpticMode(opticMode)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.webView?.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.webView?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.webView?.apply {
                removeJavascriptInterface("CallidusThreeBridge")
                stopLoading()
                destroy()
            }
            controller.webView = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 152.dp, bottom = 124.dp)
                .testTag("threejs_webview"),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // Hardware layer compositing is essential for WebGL inside Compose AndroidView
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    setBackgroundColor(android.graphics.Color.parseColor("#04070D"))

                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    @Suppress("DEPRECATION")
                    settings.allowFileAccessFromFileURLs = true
                    @Suppress("DEPRECATION")
                    settings.allowUniversalAccessFromFileURLs = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    settings.cacheMode = WebSettings.LOAD_DEFAULT

                    addJavascriptInterface(
                        object {
                            @JavascriptInterface
                            fun onEngineReady(engineName: String, gpuInfo: String) {
                                post {
                                    renderStatus = renderStatus.copy(
                                        engineName = engineName,
                                        gpuInfo = gpuInfo,
                                        isReady = true,
                                        lastError = null
                                    )
                                }
                            }

                            @JavascriptInterface
                            fun onRenderError(errorMsg: String) {
                                post {
                                    renderStatus = renderStatus.copy(lastError = errorMsg)
                                }
                            }

                            @JavascriptInterface
                            fun onFpsSample(fps: Int) {
                                post {
                                    renderStatus = renderStatus.copy(fps = fps)
                                }
                            }
                        },
                        "CallidusThreeBridge"
                    )

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            if (consoleMessage?.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                                renderStatus = renderStatus.copy(lastError = consoleMessage.message())
                            }
                            return super.onConsoleMessage(consoleMessage)
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            controller.setSceneMode(sceneMode)
                            controller.setWireframe(wireframe)
                            controller.setAutoRotate(autoRotate)
                            controller.setTheaterTarget(targetLat, targetLon, theaterName)
                            controller.setOpticMode(opticMode)
                        }
                    }

                    // Load inside HTTPS base URL origin so Three.js & WebGL run cleanly without file:// CORS blocks
                    loadDataWithBaseURL(
                        "https://callidus.three.local/",
                        assetHtml,
                        "text/html",
                        "UTF-8",
                        null
                    )
                    controller.webView = this
                }
            },
            update = { webView ->
                if (reloadNonce > 0) {
                    webView.loadDataWithBaseURL(
                        "https://callidus.three.local/",
                        assetHtml,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            }
        )

        // Top-Left WebGL Engine Status + GPU Telemetry Pill
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 158.dp, start = 12.dp, end = 12.dp)
                .testTag("threejs_engine_badge"),
            color = ObsidianPanel,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (renderStatus.isReady) CallidusEmerald.copy(alpha = 0.6f) else accent.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ViewInAr,
                    contentDescription = "Three.js Engine Status",
                    tint = if (renderStatus.isReady) CallidusEmerald else CallidusGold,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = buildString {
                            append(renderStatus.engineName)
                            if (renderStatus.fps > 0) append(" • ${renderStatus.fps} FPS")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (renderStatus.isReady) CallidusEmerald else CallidusGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = renderStatus.lastError
                            ?: renderStatus.gpuInfo.ifBlank { "TGT: $theaterName • Drag to Orbit, Pinch to Zoom" },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (renderStatus.lastError != null) CallidusCrimson else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Bottom Scene Controls Dock above Main Command Bar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 10.dp, end = 10.dp, bottom = 128.dp),
            color = ObsidianPanelElevated,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.48f))
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThreeJsSceneMode.entries.forEach { mode ->
                        val selected = sceneMode == mode
                        FilterChip(
                            selected = selected,
                            onClick = {
                                sceneMode = mode
                                controller.setSceneMode(mode)
                            },
                            label = {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent.copy(alpha = 0.24f),
                                selectedLabelColor = accent
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("threejs_mode_${mode.jsKey}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.US, "SCENE REF %+.2f°, %+.2f°", targetLat, targetLon),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                wireframe = !wireframe
                                controller.setWireframe(wireframe)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("threejs_wireframe_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridOn,
                                contentDescription = "Toggle Wireframe Mesh",
                                tint = if (wireframe) CallidusGold else TextSecondary
                            )
                        }
                        IconButton(
                            onClick = {
                                autoRotate = !autoRotate
                                controller.setAutoRotate(autoRotate)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("threejs_autorotate_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RotateRight,
                                contentDescription = "Toggle Auto-Spin",
                                tint = if (autoRotate) accent else TextSecondary
                            )
                        }
                        IconButton(
                            onClick = { controller.resetCamera() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("threejs_reset_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CenterFocusStrong,
                                contentDescription = "Reset 3D Camera",
                                tint = accent
                            )
                        }
                        IconButton(
                            onClick = {
                                reloadNonce++
                                controller.reload()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("threejs_reload_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload 3D Renderer",
                                tint = CallidusEmerald
                            )
                        }
                    }
                }
            }
        }
    }
}
