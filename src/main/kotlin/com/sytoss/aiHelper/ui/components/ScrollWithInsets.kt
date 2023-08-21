package com.sytoss.aiHelper.ui.components

import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Insets
import javax.swing.JComponent

class ScrollWithInsets(createComponent: () -> JComponent) : JBScrollPane(
    object : BorderLayoutPanel() {
        init {
            addToCenter(createComponent())
            border = null
        }

        override fun getInsets(): Insets = JBUI.insets(10)
    }
)