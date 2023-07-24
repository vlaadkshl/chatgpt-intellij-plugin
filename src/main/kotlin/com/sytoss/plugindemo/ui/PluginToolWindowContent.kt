package com.sytoss.plugindemo.ui

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.dsl.builder.panel
import com.sytoss.plugindemo.bom.pyramid.Pyramid
import com.sytoss.plugindemo.converters.FileConverter
import com.sytoss.plugindemo.services.CodeCheckingService
import com.sytoss.plugindemo.ui.components.PackagePicker
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.nio.file.Files
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JPanel

class PluginToolWindowContent(project: Project) {

    val contentPanel = JPanel()

    private val modules = ModuleManager.getInstance(project).modules.asList()

    private val folderPicker = PackagePicker(project)

    private var pyramid: Pyramid? = null

    init {
        contentPanel.layout = GridLayout()
        contentPanel.add(form())
    }

    private fun form() = panel {
        row {
            val combo = comboBox(modules)

            combo.component.selectedItem = modules[0]
            combo.component.addActionListener { event -> selectModule(event) }
        }
        row {
            button("Select Pyramid JSON") { event -> selectJsonFile(event) }
        }
        row {
            button("Errors Analysis") { analyseErrors() }
            button("Pyramid Matching Analysis") { analysePyramid() }
        }
    }

    private fun selectModule(event: ActionEvent) {
        val selectedItem: Module = (event.source as JComboBox<*>).selectedItem as Module
        folderPicker.module = selectedItem
        folderPicker.getPackages()
    }

    private fun selectJsonFile(event: ActionEvent) {
        FileChooser.chooseFile(
            FileChooserDescriptorFactory.createSingleFileDescriptor("json"), null, null
        ) { file -> consumeJsonPyramid(file, event) }
    }

    private fun consumeJsonPyramid(file: VirtualFile, event: ActionEvent) {
        (event.source as JButton).text = "Select Pyramid: ${file.name}"
        try {
            pyramid = Json.decodeFromString<Pyramid>(Files.readString(file.toNioPath()))
        } catch (e: Exception) {
            Messages.showErrorDialog(e.stackTraceToString(), "Error: Parsing JSON")
        }
    }

    private fun analyseErrors() {
        val fileContent = FileConverter.filesToClassFile(folderPicker.pyramidElems)
        val report = CodeCheckingService.analyseErrors(fileContent)

        Messages.showMessageDialog(null, report, "Error Review Results", Messages.getInformationIcon())
    }

    private fun analysePyramid() {
        if (pyramid != null) {
            val fileContent = FileConverter.filesToClassFile(folderPicker.pyramidElems)
            val report = CodeCheckingService.analysePyramid(fileContent)

            Messages.showMessageDialog(null, report, "Pyramid Review Results", Messages.getInformationIcon())
        } else {
            Messages.showErrorDialog("First select the \"pyramid.json\" file!", "Error: Analysing Pyramid")
        }
    }
}