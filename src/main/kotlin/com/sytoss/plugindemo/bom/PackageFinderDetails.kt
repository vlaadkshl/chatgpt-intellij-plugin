package com.sytoss.plugindemo.bom

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory

data class PackageFinderDetails(
    var isFolderSearchDone: Boolean = false,
    var folder: PsiDirectory?,
    val files: MutableList<VirtualFile> = mutableListOf()
)