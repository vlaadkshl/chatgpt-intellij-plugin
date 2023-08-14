package com.sytoss.aiHelper.services

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.Label
import com.sytoss.aiHelper.bom.chat.pyramid.result.PyramidAnalysisContent
import com.sytoss.aiHelper.bom.chat.pyramid.result.PyramidAnalysisGroup
import com.sytoss.aiHelper.bom.chat.warnings.ClassGroup
import com.sytoss.aiHelper.bom.codeCreating.CreateResponse
import com.sytoss.aiHelper.services.chat.CodeCheckingService
import com.sytoss.aiHelper.services.chat.PyramidCheckingService
import com.sytoss.aiHelper.ui.components.DefaultConstraints
import java.awt.BorderLayout
import java.awt.GridBagLayout
import java.awt.Label
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.JPanel

object UiBuilder {

    fun buildCreateClassesPanel(response: List<CreateResponse.CreateContent>, parent: JPanel, project: Project) {
        for (createdClass in response) {
            val classPanel = JPanel(GridBagLayout())

            val document = EditorFactory.getInstance().createDocument(createdClass.body)
            val editorPane = EditorFactory.getInstance().createEditor(
                document,
                project,
                JavaClassFileType.INSTANCE,
                false
            )

            val headerPanel = JPanel(BorderLayout())
            headerPanel.add(Label(createdClass.fileName), BorderLayout.WEST)
            headerPanel.add(ActionLink("Copy") {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(StringSelection(editorPane.document.text), null)
            }, BorderLayout.EAST)

            classPanel.add(headerPanel, DefaultConstraints.topLeftColumn)
            classPanel.add(editorPane.component, DefaultConstraints.topLeftColumn)

            parent.add(classPanel, DefaultConstraints.topLeftColumn)
        }
    }

    fun buildPyramidReportPanel(report: List<PyramidAnalysisGroup>, project: Project): JPanel {
        val panel = JPanel(GridBagLayout())

        if (report.isEmpty()) {
            panel.add(com.intellij.ui.components.Label("No errors were found."), PyramidCheckingService.constraints)
        } else {
            for (classGroup in report) {
                val classPanel = JPanel(GridBagLayout())

                val file = PyramidCheckingService.getClassVirtualFile(classGroup, project)

                if (file != null) {
                    classPanel.add(ActionLink(classGroup.className) {
                        FileEditorManager.getInstance(project).openFile(file, true)
                    }, PyramidCheckingService.constraints)
                } else {
                    classPanel.add(JBLabel("${classGroup.className} (NO LINK)"), PyramidCheckingService.constraints)
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
                    linePanel.add(com.intellij.ui.components.Label(warning.warning))

                    classPanel.add(linePanel, PyramidCheckingService.constraints)
                }

                panel.add(classPanel, PyramidCheckingService.constraints)
            }
        }

        return panel
    }

    fun buildCheckingReportPanel(report: List<ClassGroup>, project: Project): JPanel {
        val panel = JPanel(GridBagLayout())

        if (report.isEmpty()) {
            panel.add(com.intellij.ui.components.Label("No errors were found."), CodeCheckingService.constraints)
        } else {
            for (classGroup in report) {
                val classPanel = JPanel(GridBagLayout())

                val file = CodeCheckingService.getClassVirtualFile(classGroup, project)

                if (file != null) {
                    classPanel.add(ActionLink(classGroup.className) {
                        FileEditorManager.getInstance(project).openFile(file, true)
                    }, CodeCheckingService.constraints)
                } else {
                    classPanel.add(JBLabel("${classGroup.className} (NO LINK)"), CodeCheckingService.constraints)
                }

                for (warning in classGroup.warnings) {
                    val warningPanel = JPanel()
                    warningPanel.add(Label("Warning: ", null, null, true))
                    warningPanel.add(com.intellij.ui.components.Label(warning.warning))

                    val linePanel = JPanel()
                    linePanel.add(Label("Line: ", null, null, true))
                    linePanel.add(com.intellij.ui.components.Label(warning.lineInCode))

                    classPanel.add(warningPanel, CodeCheckingService.constraints)
                    classPanel.add(linePanel, CodeCheckingService.constraints)
                }

                panel.add(classPanel, CodeCheckingService.constraints)
            }
        }

        return panel
    }
}