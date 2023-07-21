package com.sytoss.plugindemo.ui.components

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.util.containers.stream
import javax.swing.JButton

class PackagePicker(
    private val project: Project,
    var module: Module = ModuleManager.getInstance(project).modules[0]
) {

    data class FolderSearchingElems(var isFolderSearchDone: Boolean = false, var folder: PsiDirectory?)

    private val pyramidElems: MutableMap<String, FolderSearchingElems> = mutableMapOf(
        "bom" to FolderSearchingElems(false, null),
        "converter" to FolderSearchingElems(false, null),
        "dto" to FolderSearchingElems(false, null),
        "service" to FolderSearchingElems(false, null),
    )

    fun fileButton(): JButton {
        val button = JButton("Select Source Package")
        button.addActionListener { event -> choosePackage() }
        return button
    }

    private fun getAllFilesInDirectory(directory: PsiDirectory) {
        directory.subdirectories.stream().forEach { dir -> println(dir.name) }
        directory.files.stream().forEach { file -> println(file.name) }

        for (subdirectory in directory.subdirectories) {
            getAllFilesInDirectory(subdirectory)
        }

        for (file in directory.files) {
//            files.add(file.virtualFile)
        }
    }

    private fun getSuitedDirectory(directory: PsiDirectory?, name: String, elem: FolderSearchingElems) {
        if (elem.isFolderSearchDone) {
            return
        }

        if (directory == null) {
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

    private fun choosePackage() {
        for ((_, value) in pyramidElems) {
            value.folder = null
            value.isFolderSearchDone = false
        }

        val selectedFolder = ModuleRootManager.getInstance(module).sourceRoots.find { root ->
            root.path.contains("main") && !root.path.contains("resources")
        }

        if (selectedFolder != null) {
            val psiDirectory = PsiManager.getInstance(project).findDirectory(selectedFolder)
            for ((name, value) in pyramidElems) {
                getSuitedDirectory(psiDirectory, name, value)
            }
        }

        pyramidElems.forEach { (name, value) -> println("$name: ${value.folder?.virtualFile?.path ?: "not found"}") }
    }
}