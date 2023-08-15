package com.sytoss.aiHelper.ui.components

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import javax.swing.JComponent

class RulesTableDialog(private val rulesTable: RulesTable) : DialogWrapper(true) {
    init {
        title = "Rules"
        init()
    }

    override fun createCenterPanel(): JComponent {
        return ScrollWithInsets {
            JBScrollPane(rulesTable)
        }
    }
}