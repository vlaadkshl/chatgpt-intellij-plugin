package com.sytoss.aiHelper.services

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.sytoss.aiHelper.bom.codeCreating.CreateResponse
import com.sytoss.aiHelper.ui.components.DefaultConstraints
import java.awt.GridBagLayout
import java.awt.Label
import javax.swing.JPanel

object UiBuilder {

    fun buildCreateClassesPanel(response: CreateResponse, parent: JPanel, project: Project) {
        parent.removeAll()

        val classes = response.result
        if (classes.isEmpty()) {
            return
        }

        for (createdClass in classes) {
            val classPanel = JPanel(GridBagLayout())

            classPanel.add(Label(createdClass.fileName), DefaultConstraints.topLeftColumn)

            val document = EditorFactory.getInstance().createDocument(createdClass.body)
            val editorPane = EditorFactory.getInstance().createEditor(
                document,
                project,
                JavaClassFileType.INSTANCE,
                false
            )
            classPanel.add(editorPane.component, DefaultConstraints.topLeftColumn)

            parent.add(classPanel, DefaultConstraints.topLeftColumn)
        }
    }
}