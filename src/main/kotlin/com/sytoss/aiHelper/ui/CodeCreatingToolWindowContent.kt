package com.sytoss.aiHelper.ui

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.OnePixelSplitter
import com.sytoss.aiHelper.bom.codeCreating.CreateResponse
import com.sytoss.aiHelper.services.PumlDiagramChooser
import com.sytoss.aiHelper.services.UiBuilder
import com.sytoss.aiHelper.services.codeCreating.BomFromPumlCreator
import com.sytoss.aiHelper.ui.components.DefaultConstraints
import com.sytoss.aiHelper.ui.components.FileChooserCreateComponent
import com.sytoss.aiHelper.ui.components.JButtonWithListener
import com.sytoss.aiHelper.ui.components.ScrollWithInsets
import java.awt.GridBagLayout
import javax.swing.*
import kotlin.concurrent.thread

class CodeCreatingToolWindowContent(private val project: Project) {

    val contentPanel = OnePixelSplitter()

    private val mainPanel = JPanel(GridBagLayout())

    private val elementsPanel = JPanel(GridBagLayout())

    private val loadingLabel = JLabel("Loading...", AnimatedIcon.Default(), SwingConstants.LEFT)

    private val pumlChooserBtn = JButtonWithListener("Choose PlantUML file") {
        PumlDiagramChooser.selectFile(it.source as JButton, project)
        createBomBtn.isEnabled = PumlDiagramChooser.isFileSelected()
    }

    private val createBomBtn = JButtonWithListener("Create BOM") {
        loadingLabel.isVisible = true
        thread {
            var bomClasses: CreateResponse? = null
            try {
                bomClasses = BomFromPumlCreator.createBom()
            } finally {
                DumbService.getInstance(project).smartInvokeLater {
                    loadingLabel.isVisible = false
                    if (bomClasses != null) {
                        UiBuilder.buildCreateClassesPanel(bomClasses, elementsPanel, project)
                    }
                }
            }
        }
    }

    private val bomChooser = FileChooserCreateComponent("Create DTO based on BOM", project)

    init {
        createBomBtn.isEnabled = PumlDiagramChooser.isFileSelected()

        contentPanel.firstComponent = ScrollWithInsets { mainPanel }
        addWithConstraints(pumlChooserBtn)
        addWithConstraints(createBomBtn)
        addWithConstraints(bomChooser)

        elementsPanel.add(loadingLabel)
        loadingLabel.isVisible = false
        contentPanel.secondComponent = ScrollWithInsets { elementsPanel }
    }

    private fun addWithConstraints(component: JComponent) {
        mainPanel.add(component, DefaultConstraints.topLeftColumn)
    }
}