package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.GlobeImageryStyle
import com.example.domain.model.IntelLayer
import com.example.domain.model.OpticMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.callidusDataStore: DataStore<Preferences> by preferencesDataStore(name = "callidus_prefs")

class CallidusPreferences(private val context: Context) {

    private object Keys {
        val OPTIC_MODE = stringPreferencesKey("optic_mode")
        val IMAGERY_STYLE = stringPreferencesKey("imagery_style")
        val IMAGERY_OPACITY = floatPreferencesKey("imagery_opacity")
        val SHOW_GRATICULE = booleanPreferencesKey("show_graticule")
        val SHOW_ATMOSPHERE = booleanPreferencesKey("show_atmosphere")
        val ENABLED_LAYERS = stringSetPreferencesKey("enabled_layers_v2")
        val PINNED_TARGETS = stringSetPreferencesKey("pinned_targets")
    }

    val opticModeFlow: Flow<OpticMode> = context.callidusDataStore.data.map { prefs ->
        val raw = prefs[Keys.OPTIC_MODE]
        OpticMode.entries.firstOrNull { it.name == raw } ?: OpticMode.EO_TACTICAL
    }

    val imageryStyleFlow: Flow<GlobeImageryStyle> = context.callidusDataStore.data.map { prefs ->
        val raw = prefs[Keys.IMAGERY_STYLE]
        GlobeImageryStyle.entries.firstOrNull { it.name == raw || it.id == raw } ?: GlobeImageryStyle.SATELLITE
    }

    val imageryOpacityFlow: Flow<Float> = context.callidusDataStore.data.map { prefs ->
        (prefs[Keys.IMAGERY_OPACITY] ?: 1.0f).coerceIn(0.15f, 1.0f)
    }

    val showGraticuleFlow: Flow<Boolean> = context.callidusDataStore.data.map { prefs ->
        prefs[Keys.SHOW_GRATICULE] ?: true
    }

    val showAtmosphereFlow: Flow<Boolean> = context.callidusDataStore.data.map { prefs ->
        prefs[Keys.SHOW_ATMOSPHERE] ?: true
    }

    val enabledLayersFlow: Flow<Set<IntelLayer>> = context.callidusDataStore.data.map { prefs ->
        val raw = prefs[Keys.ENABLED_LAYERS]
        if (raw == null) {
            IntelLayer.entries.toSet()
        } else {
            raw.mapNotNull { id -> IntelLayer.entries.firstOrNull { it.id == id } }.toSet()
        }
    }

    val pinnedTargetsFlow: Flow<Set<String>> = context.callidusDataStore.data.map { prefs ->
        prefs[Keys.PINNED_TARGETS] ?: emptySet()
    }

    suspend fun saveOpticMode(mode: OpticMode) {
        context.callidusDataStore.edit { it[Keys.OPTIC_MODE] = mode.name }
    }

    suspend fun saveImageryStyle(style: GlobeImageryStyle) {
        context.callidusDataStore.edit { it[Keys.IMAGERY_STYLE] = style.name }
    }

    suspend fun saveImageryOpacity(opacity: Float) {
        context.callidusDataStore.edit { it[Keys.IMAGERY_OPACITY] = opacity.coerceIn(0.15f, 1.0f) }
    }

    suspend fun saveShowGraticule(show: Boolean) {
        context.callidusDataStore.edit { it[Keys.SHOW_GRATICULE] = show }
    }

    suspend fun saveShowAtmosphere(show: Boolean) {
        context.callidusDataStore.edit { it[Keys.SHOW_ATMOSPHERE] = show }
    }

    suspend fun saveEnabledLayers(layers: Set<IntelLayer>) {
        context.callidusDataStore.edit { prefs ->
            prefs[Keys.ENABLED_LAYERS] = layers.map { it.id }.toSet()
        }
    }

    suspend fun togglePinnedTarget(targetId: String) {
        context.callidusDataStore.edit { prefs ->
            val current = prefs[Keys.PINNED_TARGETS] ?: emptySet()
            prefs[Keys.PINNED_TARGETS] = if (targetId in current) {
                current - targetId
            } else {
                current + targetId
            }
        }
    }
}
