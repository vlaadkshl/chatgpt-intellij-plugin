package com.sytoss.aiHelper.ui

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.OnePixelSplitter
import com.intellij.util.ui.components.BorderLayoutPanel
import com.sytoss.aiHelper.bom.codeCreating.CreateResponse
import com.sytoss.aiHelper.services.UiBuilder
import com.sytoss.aiHelper.services.codeCreating.BomFromPumlCreator
import com.sytoss.aiHelper.ui.components.DefaultConstraints
import com.sytoss.aiHelper.ui.components.FileChooserCreateComponent
import com.sytoss.aiHelper.ui.components.JButtonWithListener
import com.sytoss.aiHelper.ui.components.ScrollWithInsets
import java.awt.FlowLayout
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.concurrent.thread

class CodeCreatingToolWindowContent(private val project: Project) {

    val contentPanel = OnePixelSplitter()

    private val mainPanel = JPanel(GridBagLayout())

    private val elementsPanel = JPanel(GridBagLayout())

    private val loadingLabel = JLabel("Loading...", AnimatedIcon.Default(), SwingConstants.LEFT)

    private val pumlChooser = FileChooserCreateComponent("Choose PlantUML file", "puml", project)

    private val generateBtn = JButtonWithListener("Generate Files") {
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

    init {
        val mainBorderLayout = BorderLayoutPanel()

        addWithConstraints(pumlChooser)

        val mainPanelWrapper = JPanel(FlowLayout(FlowLayout.LEFT))
        mainPanelWrapper.add(mainPanel)

        val btnPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        btnPanel.add(generateBtn)

        mainBorderLayout.addToCenter(mainPanelWrapper)
        mainBorderLayout.addToBottom(btnPanel)

        contentPanel.firstComponent = ScrollWithInsets { mainBorderLayout }

        elementsPanel.add(loadingLabel)
        loadingLabel.isVisible = false
        contentPanel.secondComponent = ScrollWithInsets { elementsPanel }
    }

    private fun addWithConstraints(component: JComponent) {
        mainPanel.add(component, DefaultConstraints.topLeftColumn)
    }
}