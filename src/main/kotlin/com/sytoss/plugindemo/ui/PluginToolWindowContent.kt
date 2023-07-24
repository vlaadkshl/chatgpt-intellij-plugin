package com.sytoss.plugindemo.ui

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.dsl.builder.panel
import com.sytoss.plugindemo.services.CodeCheckingService
import com.sytoss.plugindemo.services.FileService
import com.sytoss.plugindemo.ui.components.PackagePicker
import java.awt.GridLayout
import java.awt.event.ActionEvent
import javax.swing.JComboBox
import javax.swing.JPanel

class PluginToolWindowContent(project: Project) {

    val contentPanel = JPanel()

    private val modules = ModuleManager.getInstance(project).modules.asList()

    private val folderPicker = PackagePicker(project)

    init {
        contentPanel.layout = GridLayout()
        contentPanel.add(form())
    }

    private fun form() = panel {
        row {
            val combo = comboBox(modules)

            combo.component.selectedItem = modules[0]
            combo.component.addActionListener { event -> listenComboChanged(event) }
        }
        row("Find Files") {
            cell(folderPicker.fileButton())
        }
        row {
            button("Show") { _ -> showReport() }
        }
    }

    private fun listenComboChanged(event: ActionEvent) {
        val selectedItem: Module = (event.source as JComboBox<*>).selectedItem as Module
        folderPicker.module = selectedItem
    }

    private fun showReport() {
        Messages.showMessageDialog(
            null,
            CodeCheckingService.generateReport(FileService.readFileContents(folderPicker.pyramidElems)),
            "Review Results",
            Messages.getInformationIcon()
        )
    }
}