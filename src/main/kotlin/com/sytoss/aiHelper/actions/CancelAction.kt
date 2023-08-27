package com.sytoss.aiHelper.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent

class CancelAction(
    actionPerformed: (e: AnActionEvent) -> Unit,
    update: (e: AnActionEvent) -> Unit
) : MyAction(
    actionPerformed,
    update,
    "Cance&l Generating",
    AllIcons.Actions.Cancel
)