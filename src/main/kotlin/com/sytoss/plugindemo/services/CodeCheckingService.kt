package com.sytoss.plugindemo.services

import com.intellij.openapi.ui.Messages
import com.sytoss.plugindemo.bom.ClassFile
import com.sytoss.plugindemo.bom.rules.Rule
import com.sytoss.plugindemo.bom.warnings.ClassGroup
import com.sytoss.plugindemo.bom.warnings.WarningsResult
import com.theokanning.openai.client.OpenAiApi
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatCompletionResult
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import com.theokanning.openai.service.OpenAiService
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
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
        val requestBuilder = StringBuilder("Here you have classes with theirs names and types for analysis\n")

        selectedFiles.forEach { (fileName, content, type): ClassFile ->
            requestBuilder.append(
                """Name: $fileName
Type: $type
Content:
``` 
$content
```

"""
            )
        }

        return requestBuilder.toString()
    }

    private fun createPyramidAnalysisRequest(): String {
        val requestBuilder = StringBuilder()

        return requestBuilder.toString()
    }

    private fun buildCodeCheckingRequest(messages: List<ChatMessage>): ChatCompletionRequest =
        ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(messages)
            .build()

    fun analyseErrors(selectedFiles: List<ClassFile>, rules: List<Rule>): WarningsResult {
        val formattedRules = RuleService.formatRules(rules)

        val mainRequest = """
You are helping the developer with searching for code errors. There are some rules, according to which you need to search errors:
$formattedRules

If you find some errors, please, group them by classes and show them in JSON format like this: {
"result": [{
  "className": "package.ClassName",
  "warnings": [{
    "warning": "{Place Warning here}",
    "lineInCode": "{place line with error here}"
    "lineNumber": {number_of_line_with_error}
  }]
}, {
  "className": "package.ClassName",
  "warnings": [{
    "warning": "{Place Warning here}",
    "lineInCode": "{place line with error here}"
    "lineNumber": {number_of_line_with_error}
  }]
}]
}""".trimIndent()

        val request = buildCodeCheckingRequest(
            listOf(
                ChatMessage(
                    ChatMessageRole.SYSTEM.value(), mainRequest
                ),
                ChatMessage(
                    ChatMessageRole.SYSTEM.value(),
                    "You must apply these rules only to those class types that are specified in the rule description.\n" +
                            "Class types are specified in their description."
                ),
                ChatMessage(
                    ChatMessageRole.SYSTEM.value(),
                    "Analyse only content of classes. Don't watch on \"package\" and \"import\" statements."
                ),
                ChatMessage(
                    ChatMessageRole.USER.value(), createErrorAnalysisRequest(selectedFiles)
                )
            )
        )

        val response = sendRequestToChat(request)

        val decodedResponse = Json.decodeFromString<WarningsResult>(response)
        decodedResponse.result = decodedResponse.result.filter { group -> group.warnings.isNotEmpty() }

        return decodedResponse
    }

    fun buildReportLabelText(report: List<ClassGroup>): String {
        val reportBuilder = StringBuilder("<html><head></head><body>")

        for (classGroup in report) {
            reportBuilder.append(
                """
<p><b>${classGroup.className}</b></p>
            """.trimIndent()
            )

            for (warning in classGroup.warnings) {
                reportBuilder.append(
                    """
<p>
    Line ${warning.lineInCode}:<br>
    Line Number: ${warning.lineNumber}<br>
    Warning: ${warning.warning}
</p>
                """.trimIndent()
                )
            }
        }

        reportBuilder.append("</body></html>")

        return reportBuilder.toString()
    }

    fun analysePyramid(selectedFiles: List<ClassFile>): String {
        val request = ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(
                listOf(
                    ChatMessage(
                        ChatMessageRole.USER.value(), createPyramidAnalysisRequest()
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