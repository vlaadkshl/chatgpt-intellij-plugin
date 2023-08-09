package com.sytoss.aiHelper.services

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.JButton

object PumlDiagramChooser {

    private var diagramFile: VirtualFile? = null

    fun selectFile(sourceButton: JButton, project: Project) {
        FileChooser.chooseFile(
            FileChooserDescriptorFactory.createSingleFileDescriptor("puml"), project, null
        ) { file ->
            run {
                sourceButton.text = "Choose PlantUML file: ${file.name}"
                diagramFile = file
            }
        }
    }
}