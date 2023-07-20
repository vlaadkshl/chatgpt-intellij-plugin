package com.sytoss.plugindemo.ui

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.vfs.VirtualFile
import com.sytoss.plugindemo.data.ClassFile
import com.sytoss.plugindemo.services.FileService

class CodePickerListener(descriptor: FileChooserDescriptor) : TextBrowseFolderListener(descriptor) {
    val selectedPaths: MutableList<VirtualFile> = mutableListOf()

    override fun onFileChosen(chosenFile: VirtualFile) {
        super.onFileChosen(chosenFile)
        selectedPaths.add(chosenFile)
    }

//    fun getFiles(): List<ClassFile> {
//        return selectedPaths.stream().map { e -> FileService.readFileContents(e) }.toList()
//    }
}