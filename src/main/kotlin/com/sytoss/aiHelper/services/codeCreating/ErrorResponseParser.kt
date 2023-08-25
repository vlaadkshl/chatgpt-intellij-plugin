package com.sytoss.aiHelper.services.codeCreating

import com.sytoss.aiHelper.bom.codeCreating.CreateResponse
import com.sytoss.aiHelper.bom.codeCreating.ErrorResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object ErrorResponseParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(response: ErrorResponse): CreateResponse? {
        var decoded: CreateResponse? = null
        try {
            decoded = json.decodeFromString<CreateResponse>(response.body)
        } catch (_: IllegalArgumentException) {
            val text = response.body
            val jsonStartIndex = text.indexOf("```json\n") + 8
            val jsonEndIndex = text.indexOf("```", jsonStartIndex)

            val jsonContent = text.substring(jsonStartIndex, jsonEndIndex)

            try {
                decoded = json.decodeFromString(jsonContent)
            } catch (_: Throwable) {
            }
        }

        return decoded
    }
}