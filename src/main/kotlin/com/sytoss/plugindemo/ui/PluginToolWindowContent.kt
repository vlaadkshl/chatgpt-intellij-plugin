package com.sytoss.plugindemo.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import com.sytoss.plugindemo.ui.components.PackagePicker
import java.awt.GridLayout
import javax.swing.JPanel

class PluginToolWindowContent(project: Project) {
    val contentPanel = JPanel()

    private val folderPicker = PackagePicker(project)

//    private val listener = CodePickerListener(FileChooserDescriptorFactory.createSingleFolderDescriptor())

//    private val fileChooser = TreeFileChooserFactory
//        .getInstance(toolWindow.project)
//        .createFileChooser("Choose Folder", null, null, null)

    init {
        contentPanel.layout = GridLayout()
        contentPanel.add(form())
    }

    private fun form() = panel {
        row("Find Files") {
            cell(folderPicker.fileButton())
        }
        row {
            button("Show") { _ -> showReport() }
        }
    }

    private fun showReport() {
//        Messages.showMessageDialog(
//            null, bomPicker.getFilesNames().toString(), "", Messages.getInformationIcon()
//        )

//        Messages.showMessageDialog(
//            null,
//            FileService.readFileContents(fileChooser.selectedPackage).toString(),
//            "",
//            Messages.getInformationIcon()
//        )

//        Messages.showMessageDialog(
//            null,
//            CodeCheckingService.generateReport(FileService.readFileContents(fileChooser.selectedPackage)),
//            "Review Results",
//            Messages.getInformationIcon()
//        )
    }
}