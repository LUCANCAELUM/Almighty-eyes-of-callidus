package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.ThreeJsSceneController
import com.example.ui.components.ThreeJsSceneMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ThreeJsWebViewTest {

    @Test
    fun threeJsAssetHtml_isPresentAndInitializesSceneSafely() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val html = context.assets.open("callidus_threejs_scene.html").bufferedReader().use { it.readText() }

        assertTrue(html.contains("THREE.Scene"))
        assertTrue(html.contains("THREE.PerspectiveCamera"))
        assertTrue(html.contains("THREE.WebGLRenderer"))
        assertTrue(html.contains("ResizeObserver"))
        assertTrue(html.contains("CallidusThreeBridge"))
        assertTrue(html.contains("buildBasicStarterGroup"))
    }

    @Test
    fun threeJsControllerAndModes_areConfigured() {
        assertEquals(4, ThreeJsSceneMode.entries.size)
        assertEquals("basic", ThreeJsSceneMode.BASIC.jsKey)
        // Controller calls safely no-op when detached
        val controller = ThreeJsSceneController()
        controller.setSceneMode(ThreeJsSceneMode.BASIC)
        controller.setWireframe(true)
        controller.resetCamera()
    }
}
