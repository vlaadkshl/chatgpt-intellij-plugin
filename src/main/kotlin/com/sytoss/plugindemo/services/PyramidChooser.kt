package com.sytoss.plugindemo.services

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.sytoss.plugindemo.bom.pyramid.Pyramid
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import javax.swing.JButton

object PyramidChooser {

    private var pyramidFile: VirtualFile? = null

    private var pyramid: Pyramid? = null

    fun isFileSelected() = pyramidFile != null

    fun isPyramidSelected() = pyramid != null

    fun clearPyramid() {
        pyramidFile = null
    }

    fun selectFile(sourceButton: JButton, project: Project) {
        FileChooser.chooseFile(
            FileChooserDescriptorFactory.createSingleFileDescriptor("json"), project, null
        ) { file ->
            run {
                sourceButton.text = "Select Pyramid: ${file.name}"
                pyramidFile = file
            }
        }
    }

    fun parsePyramidFromJson() {
        try {
            if (isFileSelected()) {
                pyramid = Json.decodeFromString<Pyramid>(Files.readString(pyramidFile?.toNioPath()))
            } else {
                Messages.showErrorDialog("First select the \"pyramid.json\" file!", "Error: Analysing Pyramid")
            }
        } catch (e: Exception) {
            Messages.showErrorDialog(e.stackTraceToString(), "Error: Parsing JSON")
        }
    }
}