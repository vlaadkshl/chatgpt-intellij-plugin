package com.sytoss.plugindemo.services

import com.sytoss.plugindemo.data.ClassFile
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import com.theokanning.openai.service.OpenAiService

object CodeCheckingService {

    private val openAiService: OpenAiService

    init {
        val key = javaClass.getResource("/key")?.readText()
            ?: throw IllegalStateException("The OpenAI API key doesn't exist")
        openAiService = OpenAiService(key)
    }

    private fun createRequestText(selectedFiles: List<ClassFile>): String {
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

        return requestBuilder.toString()
    }

    fun generateReport(selectedFiles: List<ClassFile>): String? {
        val request = ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(
                listOf(
                    ChatMessage(
                        ChatMessageRole.USER.value(), createRequestText(selectedFiles)
                    )
                )
            )
            .build()

        return openAiService.createChatCompletion(request).choices[0].message.content
    }
}