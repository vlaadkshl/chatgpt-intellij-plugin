package com.sytoss.aiHelper.ui

import com.intellij.openapi.project.Project
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

    init {
        addWithConstraints(pumlChooserBtn)
    }

    private fun addWithConstraints(component: JComponent) {
        mainPanel.add(component, DefaultConstraints.topLeftColumn)
    }
}