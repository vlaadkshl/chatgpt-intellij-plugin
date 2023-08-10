package com.sytoss.aiHelper.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.OnePixelSplitter
import com.sytoss.aiHelper.services.PumlDiagramChooser
import com.sytoss.aiHelper.services.UiBuilder
import com.sytoss.aiHelper.services.codeCreating.BomFromPumlCreator
import com.sytoss.aiHelper.ui.components.DefaultConstraints
import com.sytoss.aiHelper.ui.components.FileChooserCreateComponent
import com.sytoss.aiHelper.ui.components.JButtonWithListener
import com.sytoss.aiHelper.ui.components.ScrollWithInsets
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class CodeCreatingToolWindowContent(private val project: Project) {

    val contentPanel = OnePixelSplitter()

    private val mainPanel = JPanel(GridBagLayout())

    private val elementsPanel = JPanel(GridBagLayout())

    private val pumlChooserBtn = JButtonWithListener("Choose PlantUML file") {
        PumlDiagramChooser.selectFile(it.source as JButton, project)
        createBomBtn.isEnabled = PumlDiagramChooser.isFileSelected()
    }

    private val createBomBtn = JButtonWithListener("Create BOM") {
        val bomClasses = BomFromPumlCreator.createBom()
        if (bomClasses != null) {
            UiBuilder.buildCreateClassesPanel(bomClasses, elementsPanel, project)
        }
    }

    private val bomChooser = FileChooserCreateComponent("Create DTO based on BOM", project)

    init {
        createBomBtn.isEnabled = PumlDiagramChooser.isFileSelected()

        contentPanel.firstComponent = ScrollWithInsets { mainPanel }
        addWithConstraints(pumlChooserBtn)
        addWithConstraints(createBomBtn)
        addWithConstraints(bomChooser)

        contentPanel.secondComponent = ScrollWithInsets { elementsPanel }
    }

    private fun addWithConstraints(component: JComponent) {
        mainPanel.add(component, DefaultConstraints.topLeftColumn)
    }
}