package com.sytoss.aiHelper.ui.components

import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagConstraints.*

object DefaultConstraints {

    val topLeftColumn = GridBagConstraints(
        0,
        RELATIVE,
        1,
        1,
        0.0,
        0.0,
        WEST,
        NONE,
        JBUI.emptyInsets(),
        0,
        0
    )
}
