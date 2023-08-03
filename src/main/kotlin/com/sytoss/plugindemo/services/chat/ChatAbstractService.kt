package com.sytoss.plugindemo.services.chat

import com.theokanning.openai.client.OpenAiApi
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.service.OpenAiService
import java.time.Duration

abstract class ChatAbstractService {

    private val openAiApi: OpenAiApi

    init {
        val key = javaClass.getResource("/key")?.readText()
            ?: throw IllegalStateException("The OpenAI API key doesn't exist")
        openAiApi = OpenAiService.buildApi(key, Duration.ofSeconds(60L))
    }

    protected fun buildCodeCheckingRequest(messages: List<ChatMessage>): ChatCompletionRequest =
        ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo-16k")
            .messages(messages)
            .build()

    protected fun sendRequestToChat(request: ChatCompletionRequest?): String {
        val response = openAiApi.createChatCompletion(request).blockingGet()

        return response?.choices?.get(0)?.message?.content ?: throw RuntimeException("Can't get response")
    }
}