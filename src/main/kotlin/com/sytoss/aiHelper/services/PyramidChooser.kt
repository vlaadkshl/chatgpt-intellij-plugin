package com.sytoss.aiHelper.services

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.sytoss.aiHelper.bom.chat.pyramid.Pyramid
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import javax.swing.JButton

object PyramidChooser {

    private var pyramidFile: VirtualFile? = null

    private var pyramid: Pyramid? = null

    fun isFileSelected() = pyramidFile != null

    fun isPyramidSelected() = pyramid != null

    fun clearPyramid() {
        pyramid = null
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
        if (isFileSelected()) {
            pyramid = Json.decodeFromString<Pyramid>(Files.readString(pyramidFile?.toNioPath()))
        } else {
            throw NoSuchFileException("First select the \"pyramid.json\" file!")
        }
    }
}