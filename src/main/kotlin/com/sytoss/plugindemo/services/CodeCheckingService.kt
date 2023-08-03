package com.sytoss.plugindemo.services

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.Label
import com.sytoss.plugindemo.bom.ClassFile
import com.sytoss.plugindemo.bom.rules.Rule
import com.sytoss.plugindemo.bom.warnings.ClassGroup
import com.sytoss.plugindemo.bom.warnings.WarningsResult
import com.theokanning.openai.client.OpenAiApi
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import com.theokanning.openai.service.OpenAiService
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.time.Duration
import javax.swing.JPanel

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
            ChatMessageRole.USER.value(),
            """
                |Class Name: ${file.fileName}
                |Rules: ${file.rules.joinToString(separator = "\n", prefix = "- ")}
                |Code for checking:
                |${file.content}
            """.trimMargin()
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
            |            "warning": "{Place Warning here}",
            |            "lineInCode": "{place line with error here}"
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
        messages.addAll(createErrorAnalysisRequest(selectedFiles))

        val request = buildCodeCheckingRequest(messages)
        val response = sendRequestToChat(request)

        val decodedResponse = Json.decodeFromString<WarningsResult>(response)
        decodedResponse.result = decodedResponse.result.filter { group -> group.warnings.isNotEmpty() }.toMutableList()

        return decodedResponse
    }

    private fun getClassVirtualFile(warningClass: ClassGroup, project: Project): VirtualFile? {
        val (qualifiedName) = warningClass
        val psiClass = JavaPsiFacade.getInstance(project).findClass(
            qualifiedName,
            GlobalSearchScope.projectScope(project)
        )

        return psiClass?.containingFile?.virtualFile
    }

    fun buildReportLabelText(report: List<ClassGroup>, project: Project): JPanel {
        val panel = JPanel(GridBagLayout())

        val constraints = GridBagConstraints()
        constraints.gridx = 0
        constraints.gridy = GridBagConstraints.RELATIVE
        constraints.anchor = GridBagConstraints.WEST

        if (report.isEmpty()) {
            panel.add(Label("No errors were found."), constraints)
        } else {
            for (classGroup in report) {
                val classPanel = JPanel(GridBagLayout())

                val file: VirtualFile? = getClassVirtualFile(classGroup, project)

                if (file != null) {
                    classPanel.add(ActionLink(classGroup.className) {
                        FileEditorManager.getInstance(project).openFile(file, true)
                    }, constraints)
                } else {
                    classPanel.add(JBLabel("${classGroup.className} (NO LINK)"), constraints)
                }

                for (warning in classGroup.warnings) {
                    val warningPanel = JPanel()
                    warningPanel.add(Label("Warning: ", null, null, true))
                    warningPanel.add(Label(warning.warning))

                    val linePanel = JPanel()
                    linePanel.add(Label("Line: ", null, null, true))
                    linePanel.add(Label(warning.lineInCode))

                    classPanel.add(warningPanel, constraints)
                    classPanel.add(linePanel, constraints)
                }

                panel.add(classPanel, constraints)
            }
        }


        return panel
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
        val response = openAiApi.createChatCompletion(request).blockingGet()

        return response?.choices?.get(0)?.message?.content ?: throw RuntimeException("Can't get response")
    }
}