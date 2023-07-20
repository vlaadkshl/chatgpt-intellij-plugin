package com.sytoss.plugindemo.ui.components

import com.intellij.ide.util.PackageChooserDialog
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.util.containers.stream
import java.awt.event.ActionEvent
import javax.swing.JButton

class PackagePicker(private val project: Project) {

    data class FolderSearchingElems(var isFolderSearchDone: Boolean = false, var folder: PsiDirectory?)

    private val files: MutableList<VirtualFile> = mutableListOf()

    private val pyramidElems: MutableMap<String, FolderSearchingElems> = mutableMapOf(
        "bom" to FolderSearchingElems(false, null),
        "converter" to FolderSearchingElems(false, null),
        "dto" to FolderSearchingElems(false, null),
        "service" to FolderSearchingElems(false, null),
    )

    fun getFilesNames(): List<String> {
        return files.map { file -> file.name }
    }

    fun fileButton(): JButton {
        val button = JButton("Select Source Package")
        button.addActionListener { event -> choosePackages(event) }
        return button
    }

    private fun getAllFilesInDirectory(directory: PsiDirectory) {
        directory.subdirectories.stream().forEach { dir -> println(dir.name) }
        directory.files.stream().forEach { file -> println(file.name) }

        for (subdirectory in directory.subdirectories) {
            getAllFilesInDirectory(subdirectory)
        }

        for (file in directory.files) {
            files.add(file.virtualFile)
        }
    }

    private fun getSuitedDirectory(directory: PsiDirectory, name: String, elem: FolderSearchingElems) {
        if (elem.isFolderSearchDone) {
            return
        }

        if (directory.name.lowercase().contains(name.lowercase())) {
            elem.isFolderSearchDone = true
            elem.folder = directory
        } else {
            for (subdirectory in directory.subdirectories) {
                getSuitedDirectory(subdirectory, name, elem)
            }
        }

        return
    }

    private fun choosePackages(event: ActionEvent) {
        val fileChooser = PackageChooserDialog("Select Source Package", project)
        fileChooser.show()

        val selectedFolder = fileChooser.selectedPackage.directories[0]

        for ((name, value) in pyramidElems) {
            getSuitedDirectory(selectedFolder, name, value)
        }

        pyramidElems.forEach { (name, value) -> println("$name: ${value.folder?.virtualFile?.path ?: "not found"}") }

//        (event.source as JButton).text = selectedFolder.name
    }
}