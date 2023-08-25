package com.sytoss.aiHelper.services.codeCreating

import com.intellij.openapi.ui.Messages
import com.sytoss.aiHelper.bom.codeCreating.CreateRequest
import com.sytoss.aiHelper.bom.codeCreating.CreateResponse
import com.sytoss.aiHelper.bom.codeCreating.ErrorResponse
import com.sytoss.aiHelper.services.CommonFields.applicationManager
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.SocketTimeoutException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object RequestSender {

    fun sendRequest(request: CreateRequest): CreateResponse? {
        val reqString = Json.encodeToString(request)

        val httpClient = HttpClient.newHttpClient()
        val httpRequest = HttpRequest
            .newBuilder(URI.create("http://192.168.32.111:8000/process"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(reqString))
            .timeout(Duration.ofMinutes(5L))
            .build()

        var httpResponse: HttpResponse<String>? = null
        try {
            httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            return Json.decodeFromString<CreateResponse>(httpResponse.body())
        } catch (e: Exception) {
            if (e is IllegalArgumentException) {
                val errorJson = httpResponse?.let { Json.decodeFromString<ErrorResponse>(it.body()) }
                return errorJson?.let { ErrorResponseParser.parse(it) }
            }

            applicationManager.invokeLater {
                when (e) {
                    is SocketTimeoutException -> Messages.showErrorDialog(
                        "Timeout. Try again.",
                        "Request Sending Error"
                    )

                    is InterruptedException -> {}
                    else -> Messages.showErrorDialog(e.message, "Request Sending Error")
                }
            }
        }

        return null
    }
}