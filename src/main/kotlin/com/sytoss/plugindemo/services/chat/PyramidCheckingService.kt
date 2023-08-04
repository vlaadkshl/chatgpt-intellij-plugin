package com.sytoss.plugindemo.services.chat

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.Label
import com.sytoss.plugindemo.bom.ClassFile
import com.sytoss.plugindemo.bom.PyramidAnalysisResult
import com.sytoss.plugindemo.bom.PyramidClassReport
import com.sytoss.plugindemo.bom.PyramidReport
import com.sytoss.plugindemo.bom.pyramid.Pyramid
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel

object PyramidCheckingService : ChatAnalysisAbstractService() {

    var pyramidFile: VirtualFile? = null

    var pyramid: Pyramid? = null

    fun analyse(selectedFiles: List<ClassFile>): PyramidAnalysisResult {
        if (pyramid == null) {
            throw IllegalStateException("No pyramid was found!")
        }

        val systemMessage = """
            |You are a helpful assistant.
            |You check classes for given conditions.
            |Conditions are instructions of what should or should not be in these classes.
            |User gives you conditions in JSON format like this:
            |{
            |   "converter": [{
            |       "name": "{some class name}",
            |       "add": [
            |           "{some field or method}",
            |           "{some field or method}"
            |       ],
            |       "change": [
            |           "{some field or method}"
            |       ],
            |       "remove": [
            |           "{some field or method}"
            |       ]
            |   }, {
            |       "name": "{some class name}",
            |       "add": [
            |           "{some field or method}"
            |       ],
            |       "remove": [
            |           "{some field or method}"
            |       ]
            |   }],
            |   "bom": [{the same as in converter}],
            |   "dto": [{the same as in converter}],
            |   "interface": [{the same as in converter}],
            |   "service": [{the same as in converter}],
            |   "connector": [{the same as in converter}]
            |}
            |
            |Also user gives you classes with their type in this format:
            |Class: {some class name},
            |Type: {something from this: converter|bom|dto|interface|service|connector},
            |Content:
            |```
            |{some program code multi-line}
            |```
            |
            |Algorithm of checking is:
            |1. If this class doesn't exist in conditions, you skip it.
            |2. If some field or method exists in "add" or "change", you check, if it exists in appropriate class.
            |3. If some field or method exists in "remove", you check, if it doesn't exist in appropriate class.
            |
            |Show analysis result in JSON format like this:
            |{
            |    "result": [
            |        "className": "package.ClassName",
            |        "report": [{
            |            "type": "{FIELD or METHOD}",
            |            "name": "{Place name of FIELD or METHOD}",
            |            "warning": "{Place Warning here}"
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

        val decodedResponse = Json.decodeFromString<PyramidAnalysisResult>(response)
        decodedResponse.result = decodedResponse.result.filter { it.report.isNotEmpty() }.toMutableList()

        return decodedResponse
    }

    override fun createUserMessages(selectedFiles: List<ClassFile>): List<ChatMessage> {
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

    fun buildReportUi(report: List<PyramidClassReport>, project: Project): JPanel {
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

                for (warning in classGroup.report) {
                    val linePanel = JPanel()
                    linePanel.add(
                        Label(
                            if (warning.type == PyramidReport.ReportClassType.FIELD)
                                warning.name
                            else
                                "${warning.name}()",
                            null,
                            null,
                            true
                        )
                    )
                    linePanel.add(Label(warning.warning))

                    classPanel.add(linePanel, constraints)
                }

                panel.add(classPanel, constraints)
            }
        }

        return panel
    }
}