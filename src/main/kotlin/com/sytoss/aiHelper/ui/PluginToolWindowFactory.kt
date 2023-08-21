package com.sytoss.aiHelper.ui

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.sytoss.aiHelper.services.CommonFields
import com.sytoss.aiHelper.services.PackageFinder

class PluginToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        CommonFields.project = project
        CommonFields.dumbService = DumbService.getInstance(project)

        PackageFinder.module = ModuleManager.getInstance(project).modules[0]

        val codeCreatingContent = ContentFactory.getInstance()
            .createContent(CodeCreatingToolWindowContent().contentPanel, "Create", false)
        toolWindow.contentManager.addContent(codeCreatingContent)

        val codeAnalysisContent = ContentFactory.getInstance()
            .createContent(CodeAnalysisToolWindowContent().contentPanel, "Analyse", false)
        toolWindow.contentManager.addContent(codeAnalysisContent)
    }
}