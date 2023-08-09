package com.sytoss.aiHelper.ui

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.sytoss.aiHelper.services.PumlDiagramChooser
import com.sytoss.aiHelper.ui.components.DefaultConstraints
import com.sytoss.aiHelper.ui.components.JButtonWithListener
import com.sytoss.aiHelper.ui.components.ScrollWithInsets
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class CodeCreatingToolWindowContent(private val project: Project) {

    private val mainPanel = JPanel(GridBagLayout())

    val contentPanel = ScrollWithInsets { mainPanel }

    private val pumlChooserBtn = JButtonWithListener("Choose PlantUML file") {
        PumlDiagramChooser.selectFile(it.source as JButton, project)
    }

    private val selectedBomFiles = mutableListOf<VirtualFile>()

    private val bomChooser = JButtonWithListener("Choose BOM Files") {
        FileChooser.chooseFiles(
            FileChooserDescriptorFactory.createMultipleFilesNoJarsDescriptor(),
            project,
            null
        ) {
            selectedBomFiles.clear()
            selectedBomFiles.addAll(it)
        }
    }

    init {
        addWithConstraints(pumlChooserBtn)
        addWithConstraints(bomChooser)

        addWithConstraints(JButtonWithListener("Show File's Names") {
            Messages.showInfoMessage(selectedBomFiles.joinToString { it.name }, "BOM")
        })
    }

    private fun addWithConstraints(component: JComponent) {
        mainPanel.add(component, DefaultConstraints.topLeftColumn)
    }
}