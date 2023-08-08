package com.sytoss.plugindemo.services

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.sytoss.plugindemo.bom.FileTypes
import com.sytoss.plugindemo.bom.ModuleChooseType
import com.sytoss.plugindemo.bom.PackageFinderDetails

class PackageFinderService(
    private val project: Project,
    var module: Module = ModuleManager.getInstance(project).modules[0]
) {

    var moduleChooseType = ModuleChooseType.ALL_MODULES

    private var fileTypes = JsonService.fromJsonResourceFile<FileTypes>("/fileTypes.json").fileTypes

    val pyramidElems: MutableMap<String, PackageFinderDetails> =
        fileTypes.associateBy({ it }, { PackageFinderDetails(false) }).toMutableMap()

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
        for (subdirectory in directory.subdirectories) {
            getAllFilesInDirectory(subdirectory, files)
        }

        for (file in directory.files) {
            files.add(file.virtualFile)
        }
    }

    private fun getSuitedDirectory(directory: PsiDirectory?, name: String, elem: PackageFinderDetails) {
        if (directory == null) {
            return
        }

        if (directory.name.contains(name, ignoreCase = true)) {
            elem.isFolderSearchDone = true
            elem.folders.add(directory)
        } else {
            for (subdirectory in directory.subdirectories) {
                getSuitedDirectory(subdirectory, name, elem)
            }
        }

        return
    }

    fun findPackages() {
        clearPyramid()

        if (moduleChooseType == ModuleChooseType.ONE_MODULE) {
            val sourceDir = getModulesSource()

            if (sourceDir != null) {
                getDirectoriesForPyramid(sourceDir)
            }
        } else if (moduleChooseType == ModuleChooseType.ALL_MODULES) {
            val modules = ModuleManager.getInstance(project).modules.asList()
            for (module in modules) {
                val sourceDir = getModulesSource(module)

                if (sourceDir != null) {
                    getDirectoriesForPyramid(sourceDir)
                }
            }
        }

        setFilesToPyramid()
    }

    fun messageFileNames(): String {
        val msgBuilder = StringBuilder()

        for ((name, value) in pyramidElems) {
            if (value.folders.isNotEmpty()) {
                msgBuilder.append(
                    """
                        TYPE: ${name.uppercase()}
                        ${value.files.joinToString(transform = { it.name })}
                        
                        
                    """.trimIndent()
                )
            }
        }

        return msgBuilder.toString()
    }

    private fun setFilesToPyramid() {
        for ((_, value) in pyramidElems) {
            for (folder in value.folders) {
                getAllFilesInDirectory(folder, value.files)
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

    private fun getModulesSource(module: Module) = ModuleRootManager
        .getInstance(module)
        .sourceRoots
        .find { root ->
            root.path.contains("main") && !root.path.contains("resources")
        }

    private fun clearPyramid() {
        for ((_, value) in pyramidElems) {
            value.isFolderSearchDone = false
            value.folders.clear()
            value.files.clear()
        }
    }
}