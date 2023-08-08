package com.sytoss.plugindemo.services.chat

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.Label
import com.sytoss.plugindemo.bom.ClassFile
import com.sytoss.plugindemo.bom.rules.Rule
import com.sytoss.plugindemo.bom.warnings.ClassGroup
import com.sytoss.plugindemo.bom.warnings.WarningsResult
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel

object CodeCheckingService : ChatAnalysisAbstractService() {

    override fun createUserMessages(selectedFiles: List<ClassFile>): List<ChatMessage> {
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

    fun analyse(selectedFiles: MutableList<ClassFile>, rules: List<Rule>): WarningsResult {
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

    fun buildReportUi(report: List<ClassGroup>, project: Project): JPanel {
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

                val file = getClassVirtualFile(classGroup, project)

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
}