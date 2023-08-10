package com.sytoss.aiHelper.ui.components

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import java.awt.FlowLayout
import java.awt.Label
import javax.swing.JPanel

class FileChooserCreateComponent(text: String, project: Project) : JPanel(FlowLayout(FlowLayout.LEFT)) {
    val selectedFiles = mutableListOf<VirtualFile>()

    private val showButton = JButtonWithListener("Show Selected", AllIcons.Actions.ToggleVisibility) {
        Messages.showInfoMessage(selectedFiles.joinToString { it.name }, "Selected Files")
    }

    private val removeButton = JButtonWithListener("Clear Selection", AllIcons.Diff.Remove) {
        selectedFiles.clear()
        changeButtonsStateToAppropriate()
    }

    private val chooser = JButtonWithListener("Select Files") {
        FileChooser.chooseFiles(
            FileChooserDescriptorFactory.createMultipleFilesNoJarsDescriptor(),
            project,
            null
        ) {
            selectedFiles.clear()
            selectedFiles.addAll(it)

            changeButtonsStateToAppropriate()
        }
    }

    private fun changeButtonsStateToAppropriate() {
        showButton.isEnabled = selectedFiles.isNotEmpty()
        removeButton.isEnabled = selectedFiles.isNotEmpty()
    }

    init {
        changeButtonsStateToAppropriate()

        add(Label(text))
        add(chooser)
        add(showButton)
        add(removeButton)
    }
}