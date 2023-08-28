package com.sytoss.aiHelper.services.codeCreating

import com.sytoss.aiHelper.bom.codeCreating.CreateRequest
import com.sytoss.aiHelper.bom.codeCreating.CreateResponse
import com.sytoss.aiHelper.bom.codeCreating.ErrorResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object RequestSender {

    suspend fun sendRequest(request: CreateRequest): CreateResponse? {
        val reqString = Json.encodeToString(request)

        val httpClient = HttpClient.newHttpClient()
        val httpRequest = HttpRequest
            .newBuilder(URI.create("http://192.168.32.111:8000/process"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(reqString))
            .timeout(Duration.ofMinutes(5L))
            .build()

        val httpResponse: String? = withContext(Dispatchers.IO) {
            val response = httpClient.sendAsync(
                httpRequest,
                HttpResponse.BodyHandlers.ofString()
            ).await()

            if (response.statusCode() == 200) response.body()
            else null
        }

        return try {
            httpResponse?.let { Json.decodeFromString<CreateResponse>(it) }
        } catch (_: IllegalArgumentException) {
            val errorJson = httpResponse?.let { Json.decodeFromString<ErrorResponse>(it) }
            errorJson?.let { ErrorResponseParser.parse(it) }
        }
    }
}