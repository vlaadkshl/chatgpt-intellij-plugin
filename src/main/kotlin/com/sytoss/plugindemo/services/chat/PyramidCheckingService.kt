package com.sytoss.plugindemo.services.chat

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.Label
import com.sytoss.plugindemo.bom.chat.ChatMessageClassData
import com.sytoss.plugindemo.bom.chat.pyramid.result.PyramidAnalysisResult
import com.sytoss.plugindemo.bom.chat.pyramid.result.PyramidAnalysisGroup
import com.sytoss.plugindemo.bom.chat.pyramid.result.PyramidAnalysisContent
import com.sytoss.plugindemo.services.PyramidChooser
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel

object PyramidCheckingService : ChatAnalysisAbstractService() {

    fun analyse(selectedFiles: List<ChatMessageClassData>): PyramidAnalysisResult {
        if (!PyramidChooser.isPyramidSelected()) {
            throw NoSuchElementException("No pyramid was found!")
        }

        val systemMessages = mutableListOf(
            """
            You are a helpful assistant.
            You check classes for given conditions and give result of analysis to the user.
            You don't create code for checking, you just analyse given code according to the conditions.
            Conditions are instructions of what should or should not be in these classes.
        """.trimIndent(),
//            """
//            User gives you conditions in JSON format like this:
//            {
//               "converter": [{
//                   "name": "{some class name}",
//                   "add": [
//                       "{some field or method}",
//                       "{some field or method}"
//                   ],
//                   "change": [
//                       "{some field or method}"
//                   ],
//                   "remove": [
//                       "{some field or method}"
//                   ]
//               }, {
//                   "name": "{some class name}",
//                   "add": [
//                       "{some field or method}"
//                   ],
//                   "remove": [
//                       "{some field or method}"
//                   ]
//               }],
//               "bom": [{the same as in converter}],
//               "dto": [{the same as in converter}],
//               "interface": [{the same as in converter}],
//               "service": [{the same as in converter}],
//               "connector": [{the same as in converter}]
//            }
//        """.trimIndent(),
            """
            Also user gives you classes with their type in this format:
            Class: {some class name},
            Type: {something from this: converter|bom|dto|interface|service|connector},
            Content:
            ```
            {some program code multi-line}
            ```
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
            If there is no errors for class, don't put it in result
        """.trimIndent(), """
            Here are the steps you must follow while checking the code:
            1. If this class doesn't exist in conditions, you skip it.
            2. If some field or method exists in "add" or "change", you check, if it exists in appropriate class. If don't, you say it in warning.
            3. If some field or method exists in "remove", you check, if it doesn't exist in appropriate class. If exists, you say it in warning.
            4. If some field or method isn't mentioned in conditions, you skip it.
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

    fun buildReportUi(report: List<PyramidAnalysisGroup>, project: Project): JPanel {
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
                            if (warning.type == PyramidAnalysisContent.ReportClassType.FIELD)
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