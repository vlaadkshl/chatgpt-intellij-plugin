package com.sytoss.aiHelper.ui.components

import com.intellij.ui.components.JBCheckBox
import java.awt.event.ActionEvent

class JBCheckBoxWithListener(title: String, listener: (e: ActionEvent) -> Unit) : JBCheckBox(title) {
    init {
        addActionListener(listener)
    }
}