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

object PyramidService {
    fun selectPyramid(sourceButton: JButton, project: Project): VirtualFile? {
        var returnFile: VirtualFile? = null

        FileChooser.chooseFile(
            FileChooserDescriptorFactory.createSingleFileDescriptor("json"), project, null
        ) { file ->
            run {
                sourceButton.text = "Select Pyramid: ${file.name}"
                returnFile = file
            }
        }

        return returnFile
    }

    fun parseJson(file: VirtualFile): Pyramid? {
        try {
            return Json.decodeFromString<Pyramid>(Files.readString(file.toNioPath()))
        } catch (e: Exception) {
            Messages.showErrorDialog(e.stackTraceToString(), "Error: Parsing JSON")
        }
        return null
    }
}