package com.sytoss.aiHelper.services.chat

import com.sytoss.aiHelper.bom.chat.ChatMessageClassData
import com.sytoss.aiHelper.bom.chat.pyramid.result.PyramidAnalysisResult
import com.sytoss.aiHelper.services.PyramidChooser
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object PyramidCheckingService : ChatAnalysisAbstractService() {

    suspend fun analyse(selectedFiles: List<ChatMessageClassData>): PyramidAnalysisResult {
        if (!PyramidChooser.isPyramidSelected()) {
            throw NoSuchElementException("No pyramid was found!")
        }

        val systemMessages = mutableListOf(
            """
            You are a helpful assistant.
            You check classes for given conditions and give result of analysis to the user.
        """.trimIndent(),
            """
            Also user gives you classes with their type in this format:
            Class: {some class name},
            Type: {something from this: converter|bom|dto|interface|service|connector},
            Content:
            ```
            {some program code multi-line}
            ```
        """.trimIndent(), """
            Acceptance criteria: 
            - If field or method is in "add" or "change" conditions AND is present in appropriate class.
            - If field or method is in "remove" conditions AND is not present in appropriate class.
            
            If there is no errors for class, don't put it in result.
        """.trimIndent(), """
            IMPORTANT! Show analysis result in JSON format only according to this template:
            {
                "result": [
                    "className": "package.ClassName",
                    "report": [{
                        "type": "{FIELD or METHOD}",
                        "name": "{Place name of FIELD or METHOD}",
                        "warning": "{Place Warning here}"
                    }]
                ]
            }
        """.trimIndent()
        )

        val messages = mutableListOf<ChatMessage>()
        messages.addAll(systemMessages.map {
            ChatMessage(
                ChatMessageRole.SYSTEM.value(), it
            )
        })
        messages.addAll(createUserMessages(selectedFiles))

        val request = buildRequest(messages)
        val response = sendRequestToChat(request)

        val decodedResponse = Json.decodeFromString<PyramidAnalysisResult>(response)
        decodedResponse.result.removeIf { it.report.isEmpty() }

        return decodedResponse
    }

    override fun createUserMessages(selectedFiles: List<ChatMessageClassData>): List<ChatMessage> {
        return selectedFiles.map {
            ChatMessage(
                ChatMessageRole.USER.value(),
                """
                    Class: ${it.fileName},
                    Type: ${it.type},
                    Content:
                    ```
                    ${it.content}
                    ```
                """.trimIndent()
            )
        }
    }
}