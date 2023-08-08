package com.sytoss.plugindemo.services

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.sytoss.plugindemo.bom.ClassFile
import com.sytoss.plugindemo.bom.FileTypes
import com.sytoss.plugindemo.bom.ModuleChooseType
import com.sytoss.plugindemo.bom.PackageFinderDetails
import java.nio.file.Files

object PackageFinder {
    lateinit var project: Project

    lateinit var module: Module

    var moduleChooseType = ModuleChooseType.ALL_MODULES

    private val types = JsonService.fromJsonResourceFile<FileTypes>("/fileTypes.json").fileTypes

    private val filesMap: MutableMap<String, PackageFinderDetails> =
        types.associateBy({ it }, { PackageFinderDetails() }).toMutableMap()

    private fun getAllFilesInDirectory(directory: PsiDirectory): MutableList<VirtualFile> {
        val foundFiles = mutableListOf<VirtualFile>()
        val stack = mutableListOf(directory)
        while (stack.isNotEmpty()) {
            val currentDirectory = stack.removeLast()

            val virtualFiles = currentDirectory.files.map { it.virtualFile }
            foundFiles.addAll(virtualFiles)

            stack.addAll(currentDirectory.subdirectories)
        }
        return foundFiles
    }

    private fun findSubdirectoriesByName(directory: PsiDirectory, name: String): List<PsiDirectory> {
        val foundFolders = mutableListOf<PsiDirectory>()
        val stack = mutableListOf(directory)

        while (stack.isNotEmpty()) {
            val currentDirectory = stack.removeLast()

            if (currentDirectory.name.contains(name, ignoreCase = true)) {
                foundFolders.add(currentDirectory)
            }

            stack.addAll(currentDirectory.subdirectories)
        }

        return foundFolders
    }

    private fun setFilesToMap() {
        for ((_, value) in filesMap) {
            for (folder in value.folders) {
                val files = getAllFilesInDirectory(folder)
                value.files.addAll(files)
            }
        }
    }

    private fun findMapDirectories(selectedFolder: PsiDirectory) {
        for ((name, value) in filesMap) {
            val directoriesWithName = findSubdirectoriesByName(selectedFolder, name)
            value.folders.addAll(directoriesWithName)
        }
    }

    private fun findModuleSourceDir(module: Module): PsiDirectory? {
        val sourceDir = ModuleRootManager
            .getInstance(module)
            .sourceRoots
            .find { it.path.contains("main") && !it.path.contains("resources") }

        return sourceDir?.let { PsiManager.getInstance(project).findDirectory(it) }
    }

    private fun clearPyramid() {
        filesMap.values.forEach {
            it.files.clear()
            it.folders.clear()
        }
    }

    fun isEmpty() = filesMap.values.find { it.files.isNotEmpty() } == null

    fun findPackages() {
        clearPyramid()

        if (moduleChooseType == ModuleChooseType.ONE_MODULE) {
            val sourceDir = findModuleSourceDir(this.module)

            if (sourceDir != null) {
                findMapDirectories(sourceDir)
            }
        } else {
            for (module in ModuleManager.getInstance(project).modules) {
                val sourceDir = findModuleSourceDir(module)

                if (sourceDir != null) {
                    findMapDirectories(sourceDir)
                }
            }
        }

        setFilesToMap()
    }

    fun toClassFiles(): MutableList<ClassFile> {
        val fileList = mutableListOf<ClassFile>()

        for ((type, value) in filesMap) {
            if (value.files.isNotEmpty()) {
                for (file in value.files) {
                    fileList.add(
                        ClassFile(
                            fileName = file.nameWithoutExtension,
                            content = Files.readString(file.toNioPath()),
                            type = type
                        )
                    )
                }
            }
        }

        return fileList
    }

    fun messageFileNames(): String {
        val msgBuilder = StringBuilder()

        for ((name, value) in filesMap) {
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
}