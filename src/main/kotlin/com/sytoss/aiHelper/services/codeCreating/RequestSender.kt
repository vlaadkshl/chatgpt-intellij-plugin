package com.sytoss.aiHelper.services.codeCreating

import com.intellij.openapi.ui.Messages
import com.sytoss.aiHelper.bom.codeCreating.CreateRequest
import com.sytoss.aiHelper.bom.codeCreating.CreateResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object RequestSender {

    private val json = Json { ignoreUnknownKeys = true }

    fun sendRequest(request: CreateRequest): CreateResponse? {
        val reqString = Json.encodeToString(request)
        println(reqString)

        val httpClient = HttpClient.newHttpClient()
        val httpRequest = HttpRequest
            .newBuilder(URI.create("http://192.168.32.111:8000/process"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(reqString))
            .build()

        val httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())

        var decodedResponse: CreateResponse? = null
        try {
            decodedResponse = json.decodeFromString<CreateResponse>(httpResponse.body())
        } catch (e: Exception) {
            Messages.showErrorDialog(e.message, "Json Decoding Error")
        }

        return decodedResponse
    }
}