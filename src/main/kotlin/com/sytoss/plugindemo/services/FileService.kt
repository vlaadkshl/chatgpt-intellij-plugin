package com.sytoss.plugindemo.services

import com.intellij.psi.PsiPackage
import com.intellij.util.containers.stream
import com.sytoss.plugindemo.data.ClassFile
import java.nio.file.Files
import java.nio.file.Path

object FileService {
    fun readFileContents(psiPackage: PsiPackage): List<ClassFile> {
        return psiPackage.classes.stream()
            .map { file -> file.containingFile }
            .map { file -> ClassFile(file.name, Files.readString(Path.of(file.virtualFile.path))) }
            .toList()
    }
}