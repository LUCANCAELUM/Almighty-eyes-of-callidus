package com.example.data.firebase

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

enum class CallidusAiModel(
    val modelId: String,
    val label: String,
    val badge: String
) {
    FAST("gemini-3.5-flash-lite", "Flash Lite (Fast)", "FAST"),
    BALANCED("gemini-3.8-flash", "3.8 Flash (Maps/OSINT)", "STD"),
    DEEP("gemini-4-argon", "4 Argon (Deep)", "DEEP")
}

sealed interface AiStreamChunk {
    data class Delta(val accumulatedText: String) : AiStreamChunk
    data class Completed(val finalText: String) : AiStreamChunk
    data class Failed(val message: String) : AiStreamChunk
}

class CallidusAiRepository {

    private val systemPrompt = content {
        text(
            """
            You are CALLIDUS-AI, an elite orbital spatial intelligence, aviation, seismic, and geographic analyst embedded inside "The Almighty Eyes of Callidus".
            You receive real-time telemetry context from the user's 3D globe (latitude, longitude, nearby live aircraft, orbital satellites, recent USGS earthquakes, NASA EONET natural events, and Open-Meteo weather).
            Provide concise, high-signal, factual intelligence briefings, geographic & Google Maps location insights, and risk assessments.
            When naming specific geographic locations or landmarks near the target coordinates, include their approximate decimal coordinates in parentheses like (lat, lon) so the operator can lock onto them or open them in Google Maps.
            """.trimIndent()
        )
    }

    private val fastModel by lazy {
        Firebase.ai.generativeModel(
            modelName = CallidusAiModel.FAST.modelId,
            systemInstruction = systemPrompt
        )
    }

    private val balancedModel by lazy {
        Firebase.ai.generativeModel(
            modelName = CallidusAiModel.BALANCED.modelId,
            systemInstruction = systemPrompt
        )
    }

    private val deepModel by lazy {
        Firebase.ai.generativeModel(
            modelName = CallidusAiModel.DEEP.modelId,
            systemInstruction = systemPrompt
        )
    }

    private fun modelFor(choice: CallidusAiModel) = when (choice) {
        CallidusAiModel.FAST -> fastModel
        CallidusAiModel.BALANCED -> balancedModel
        CallidusAiModel.DEEP -> deepModel
    }

    fun streamChatTurn(
        modelChoice: CallidusAiModel,
        historyMessages: List<IntelChatMessage>,
        liveTelemetryContext: String,
        userPrompt: String
    ): Flow<AiStreamChunk> = flow {
        val model = modelFor(modelChoice)
        val history: List<Content> = historyMessages.takeLast(16).map { msg ->
            content(role = if (msg.role == "model") "model" else "user") {
                text(msg.content)
            }
        }
        val chat = model.startChat(history = history)
        val enrichedPrompt = buildString {
            append("[LIVE CALLIDUS TELEMETRY CONTEXT]\n")
            append(liveTelemetryContext)
            append("\n[OPERATOR QUERY]\n")
            append(userPrompt)
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                chat.sendMessageStream(enrichedPrompt).collect { chunk ->
                    val delta = chunk.text.orEmpty()
                    if (delta.isNotEmpty()) {
                        accumulated.append(delta)
                        emit(AiStreamChunk.Delta(accumulated.toString()))
                    }
                }
            }
            val finalStr = accumulated.toString().trim()
            if (finalStr.isNotEmpty()) {
                emit(AiStreamChunk.Completed(finalStr))
            } else {
                emit(AiStreamChunk.Failed("Gemini returned an empty response."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini chat timed out", e)
            emit(AiStreamChunk.Failed("Analysis timed out after 60 seconds. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini chat failed", e)
            emit(AiStreamChunk.Failed(e.localizedMessage ?: "Gemini AI request failed."))
        }
    }

    private companion object {
        const val TAG = "CallidusAiRepo"
    }
}
