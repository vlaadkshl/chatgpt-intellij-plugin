package com.sytoss.aiHelper.ui.components

import java.awt.event.ActionEvent
import javax.swing.Icon
import javax.swing.JButton

class JButtonWithListener(name: String, actionListener: (event: ActionEvent) -> Unit) : JButton(name) {
    init {
        addActionListener(actionListener)
    }

    constructor(name: String, icon: Icon, actionListener: (event: ActionEvent) -> Unit) : this(name, actionListener) {
        setIcon(icon)
    }
}