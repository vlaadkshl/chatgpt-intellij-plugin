package com.sytoss.aiHelper.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent

/*
   * WHEN NEEDED:
   * isn't last
   * was cancelled
   * */

class ContinueAction(
    actionPerformed: (e: AnActionEvent) -> Unit,
    update: (e: AnActionEvent) -> Unit
) : MyAction(
    actionPerformed,
    update,
    "Co&ntinue Generating",
    AllIcons.Actions.Resume
)