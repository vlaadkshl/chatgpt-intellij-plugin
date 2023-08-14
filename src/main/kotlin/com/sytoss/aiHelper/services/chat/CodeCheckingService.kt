package com.sytoss.aiHelper.services.chat

import com.sytoss.aiHelper.bom.chat.ChatMessageClassData
import com.sytoss.aiHelper.bom.chat.checkingCode.rules.Rule
import com.sytoss.aiHelper.bom.chat.warnings.WarningsResult
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object CodeCheckingService : ChatAnalysisAbstractService() {

    override fun createUserMessages(selectedFiles: List<ChatMessageClassData>): List<ChatMessage> {
        return selectedFiles.map {
            ChatMessage(
                ChatMessageRole.USER.value(),
                """
                    |Class Name: ${it.fileName}
                    |Rules: ${it.rules.joinToString(separator = "\n", prefix = "- ")}
                    |Code for checking:
                    |${it.content}
                """.trimMargin()
            )
        }
    }

    fun analyse(selectedFiles: MutableList<ChatMessageClassData>, rules: List<Rule>): WarningsResult {
        for (file in selectedFiles) {
            val applicableRules = rules.filter { it.fileTypes.contains(file.type) }
            file.rules = applicableRules.map { it.rule }
        }

        selectedFiles.removeIf { it.rules.isEmpty() }

        if (selectedFiles.isEmpty()) {
            return WarningsResult(mutableListOf())
        }

        val systemMessage = """
            |You are a helpful assistant.
            |You search for code errors according to the rules in prompt.
            |Don't analyze imports of classes.
            |
            |Show errors in JSON format like this:
            |{
            |    "result": [
            |        "className": "package.ClassName",
            |        "warnings": [{
            |            "warning": "{Place warning here}",
            |            "lineInCode": "{Place line with error here}"
            |        }]
            |    ]
            |}
            |If there is no errors for class, don't put it in result
        """.trimMargin()

        val messages = mutableListOf(
            ChatMessage(
                ChatMessageRole.SYSTEM.value(), systemMessage
            )
        )
        messages.addAll(createUserMessages(selectedFiles))

        val request = buildRequest(messages)
        val response = sendRequestToChat(request)

        val decodedResponse = Json.decodeFromString<WarningsResult>(response)
        decodedResponse.result = decodedResponse.result.filter { it.warnings.isNotEmpty() }.toMutableList()

        return decodedResponse
    }
}