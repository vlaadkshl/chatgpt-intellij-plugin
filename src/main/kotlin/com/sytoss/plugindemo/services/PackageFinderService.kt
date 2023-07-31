package com.sytoss.plugindemo.services

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.util.containers.stream
import com.sytoss.plugindemo.bom.FileTypes
import com.sytoss.plugindemo.bom.PackageFinderDetails

class PackageFinderService(
    private val project: Project,
    var module: Module = ModuleManager.getInstance(project).modules[0]
) {

    private var fileTypes = JsonService.fromJsonResourceFile<FileTypes>("/fileTypes.json").fileTypes

    val pyramidElems: MutableMap<String, PackageFinderDetails> = mutableMapOf()

    init {
        for (type in fileTypes) {
            pyramidElems[type] = PackageFinderDetails(false, null)
        }
    }

    fun isPyramidEmpty(): Boolean {
        var isEmpty = true

        for ((_, value) in pyramidElems) {
            if (value.files.isNotEmpty()) {
                isEmpty = false
                break
            }
        }

        return isEmpty
    }

    private fun getAllFilesInDirectory(directory: PsiDirectory, files: MutableList<VirtualFile>) {
        directory.subdirectories.stream().forEach { dir -> println(dir.name) }
        directory.files.stream().forEach { file -> println(file.name) }

        for (subdirectory in directory.subdirectories) {
            getAllFilesInDirectory(subdirectory, files)
        }

        for (file in directory.files) {
            files.add(file.virtualFile)
        }
    }

    private fun getSuitedDirectory(directory: PsiDirectory?, name: String, elem: PackageFinderDetails) {
        if (elem.isFolderSearchDone) {
            return
        }

        if (directory == null) {
            return
        }

        if (directory.name.contains(name, ignoreCase = true)) {
            elem.isFolderSearchDone = true
            elem.folder = directory
        } else {
            for (subdirectory in directory.subdirectories) {
                getSuitedDirectory(subdirectory, name, elem)
            }
        }

        return
    }

    fun findPackages() {
        clearPyramid()

        val sourceDir = getModulesSource()

        if (sourceDir != null) {
            getDirectoriesForPyramid(sourceDir)
        }

        setFilesToPyramid()
        messageFileNames()
    }

    private fun messageFileNames() {
        val msgBuilder = StringBuilder()

        for ((name, value) in pyramidElems) {
            if (value.folder != null) {
                msgBuilder.append("TYPE: ${name.uppercase()}\n")
                msgBuilder.append(value.files.map { file -> file.name }.toString()).append("\n\n")
            }
        }

        Messages.showMessageDialog(msgBuilder.toString(), "", Messages.getInformationIcon())
    }

    private fun setFilesToPyramid() {
        for ((_, value) in pyramidElems) {
            if (value.folder != null) {
                getAllFilesInDirectory(value.folder!!, value.files)
            }
        }
    }

    private fun getDirectoriesForPyramid(selectedFolder: VirtualFile) {
        val psiDirectory = PsiManager.getInstance(project).findDirectory(selectedFolder)
        for ((name, value) in pyramidElems) {
            getSuitedDirectory(psiDirectory, name, value)
        }
    }

    private fun getModulesSource() = ModuleRootManager
        .getInstance(module)
        .sourceRoots
        .find { root ->
            root.path.contains("main") && !root.path.contains("resources")
        }

    private fun clearPyramid() {
        for ((_, value) in pyramidElems) {
            value.folder = null
            value.isFolderSearchDone = false
            value.files.clear()
        }
    }
}