package com.sytoss.plugindemo.services

import com.intellij.openapi.ui.Messages
import com.sytoss.plugindemo.bom.ClassFile
import com.theokanning.openai.client.OpenAiApi
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatCompletionResult
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import com.theokanning.openai.service.OpenAiService
import java.net.SocketTimeoutException
import java.time.Duration

object CodeCheckingService {

    private val openAiApi: OpenAiApi

    init {
        val key = javaClass.getResource("/key")?.readText()
            ?: throw IllegalStateException("The OpenAI API key doesn't exist")
        openAiApi = OpenAiService.buildApi(key, Duration.ofSeconds(30L))
    }

    private fun createErrorAnalysisRequest(selectedFiles: List<ClassFile>): String {
        val requestBuilder = StringBuilder(
            """
                These are the code rules:
                1. BOM classes can't contain DTOs, services or another non-BOM elements;
                2. We work with repositories only in services.
                Let me know, what are the errors in code snippets below. Group warnings by classes.
            """
        )

        selectedFiles.forEach { (fileName, content): ClassFile ->
            requestBuilder.append(
                """
                    Name: $fileName
                    Content:
                    ``` 
                    $content
                    ```
                    
                """
            )
        }

        requestBuilder.append(
            """
            Show me warnings in JSON format.
            The fields are "fileName", "warnings" (array with fields "warning" and "lineInCode"; the "lineInCode" shows, what line this warning affects)
        """
        )

        return requestBuilder.toString()
    }

    private fun createPyramidAnalysisRequest(selectedFiles: List<ClassFile>): String {
        val requestBuilder = StringBuilder(
            """
                test test
            """
        )

        return requestBuilder.toString()
    }

    fun analyseErrors(selectedFiles: List<ClassFile>): String {
        val request = ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(
                listOf(
                    ChatMessage(
                        ChatMessageRole.USER.value(), createErrorAnalysisRequest(selectedFiles)
                    )
                )
            )
            .build()

        return sendRequestToChat(request)
    }

    fun analysePyramid(selectedFiles: List<ClassFile>): String {
        val request = ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(
                listOf(
                    ChatMessage(
                        ChatMessageRole.USER.value(), createPyramidAnalysisRequest(selectedFiles)
                    )
                )
            )
            .build()

        return sendRequestToChat(request)
    }

    private fun sendRequestToChat(request: ChatCompletionRequest?): String {
        var response: ChatCompletionResult? = null
        try {
            response = openAiApi.createChatCompletion(request).blockingGet()
        } catch (_: SocketTimeoutException) {
            Messages.showInfoMessage(
                "Oops! We have a timeout error. Trying to send request again",
                "RequestTimeout Error"
            )

            try {
                response = openAiApi.createChatCompletion(request).blockingGet()
            } catch (_: SocketTimeoutException) {
                Messages.showErrorDialog(
                    "Sorry, but we can't get a response due to timeout exception. Try again later.",
                    "RequestTimeout Error"
                )
            }
        }

        return response?.choices?.get(0)?.message?.content ?: throw RuntimeException("Can't get response")
    }
}