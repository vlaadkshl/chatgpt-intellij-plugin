package com.sytoss.aiHelper.ui.components

import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JPanel

class ScrollWithInsets(createComponent: () -> JComponent) : JBScrollPane(
    object : JPanel(BorderLayout()) {
        init {
            add(createComponent())
        }

        override fun getInsets(): Insets = JBUI.insets(10)
    }
)