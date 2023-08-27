package com.sytoss.aiHelper.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.Icon

open class MyAction(
    val actionPerformedCallback: (e: AnActionEvent) -> Unit,
    val updateCallback: (e: AnActionEvent) -> Unit,
    text: String,
    icon: Icon
) : AnAction(text, null, icon) {
    override fun actionPerformed(e: AnActionEvent) {
        actionPerformedCallback(e)
    }

    override fun update(e: AnActionEvent) {
        updateCallback(e)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT
}