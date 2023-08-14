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

    val checkbox = GridBagConstraints(
        0, RELATIVE,
        2, 1,
        1.0, 0.0,
        WEST, HORIZONTAL,
        JBUI.insets(6, 0, 0, 4),
        0, 0
    )

    val checkboxInsets = GridBagConstraints(
        0, RELATIVE,
        2, 1,
        1.0, 0.0,
        WEST, HORIZONTAL,
        JBUI.insets(6, 16, 0, 4),
        0, 0
    )
}
