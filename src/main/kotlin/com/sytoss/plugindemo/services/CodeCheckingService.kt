package com.sytoss.plugindemo.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
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
        openAiApi = OpenAiService.buildApi(key, Duration.ofSeconds(60L))
    }

    private fun createErrorAnalysisRequest(selectedFiles: List<ClassFile>): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

        selectedFiles.forEach { file -> messages.add(convertFileToRequest(file)) }

        return messages
    }

    private fun convertFileToRequest(file: ClassFile): ChatMessage {
        return ChatMessage(
            ChatMessageRole.USER.value(), """
Class Name: ${file.fileName}
Rules: ${file.rules.joinToString(separator = "\n", prefix = "- ")}
Code for checking:
${file.content}
"""
        )
    }

    private fun createPyramidAnalysisRequest(): String {
        val requestBuilder = StringBuilder()

        return requestBuilder.toString()
    }

    private fun buildCodeCheckingRequest(messages: List<ChatMessage>): ChatCompletionRequest =
        ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo-16k")
            .messages(messages)
            .build()

    fun analyseErrors(selectedFiles: MutableList<ClassFile>, rules: List<Rule>): WarningsResult {
        for (file in selectedFiles) {
            val applicableRules = rules.filter { rule -> rule.fileTypes.contains(file.type) }
            file.rules = applicableRules.map { rule -> rule.rule }
        }

        selectedFiles.removeIf { file -> file.rules.isEmpty() }

        val systemMessage = """
You are a helpful assistant.
You search for code errors according to the rules in prompt.
Don't analyze imports of classes.

Show errors in JSON format like this:
{
    "result": [
        "className": "package.ClassName",
        "warnings": [{
            "warning": "{Place Warning here}",
            "lineInCode": "{place line with error here}"
            "lineNumber": {number_of_line_with_error}
        }]
    ]
}
If there is no errors for class, don't put it in result""".trimIndent()

        val messages = mutableListOf(
            ChatMessage(
                ChatMessageRole.SYSTEM.value(), systemMessage
            ),
            ChatMessage(
                ChatMessageRole.SYSTEM.value(),
                "Analyse only content of classes. Don't watch on \"package\" and \"import\" statements."
            )
        )
        messages.addAll(createErrorAnalysisRequest(selectedFiles))

        val request = buildCodeCheckingRequest(messages)
        val response = sendRequestToChat(request)

        val decodedResponse = Json.decodeFromString<WarningsResult>(response)
        decodedResponse.result = decodedResponse.result.filter { group -> group.warnings.isNotEmpty() }.toMutableList()

        return decodedResponse
    }

    private fun getClassPath(warningClass: ClassGroup, project: Project): String? {
        val (qualifiedName) = warningClass
        val psiClass = JavaPsiFacade.getInstance(project).findClass(
            qualifiedName,
            GlobalSearchScope.projectScope(project)
        )

        return if (psiClass != null) "file:///${psiClass.containingFile?.virtualFile?.path}" else null
    }

    fun buildReportLabelText(report: List<ClassGroup>, project: Project): String {
        val reportBuilder = StringBuilder("<html><head></head><body>")

        for (classGroup in report) {
            val path = getClassPath(classGroup, project)

            reportBuilder.append(
                """
<p><a href="$path"><b>${classGroup.className}</b></a></p>
            """.trimIndent()
            )

            for (warning in classGroup.warnings) {
                reportBuilder.append(
                    """
<p>
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