package com.sytoss.aiHelper.ui

import com.intellij.openapi.project.Project
import com.sytoss.aiHelper.ui.components.ScrollWithInsets
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel

class CodeCreatingToolWindowContent(private val project: Project) {

    private val mainPanel = JPanel(GridBagLayout())

    val contentPanel = ScrollWithInsets { mainPanel }

    private val constraints = GridBagConstraints()

    init {
        constraints.gridx = 0
        constraints.anchor = GridBagConstraints.PAGE_START
    }
}