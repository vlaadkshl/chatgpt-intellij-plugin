package com.sytoss.aiHelper.bom

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory

data class PackageFinderDetails(
    var folders: MutableList<PsiDirectory> = mutableListOf(),
    val files: MutableList<VirtualFile> = mutableListOf()
)
