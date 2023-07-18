package com.sytoss.plugindemo.services

import com.intellij.openapi.vfs.VirtualFile
import com.sytoss.plugindemo.data.ClassFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

object FileService {
    fun readFileContents(file: VirtualFile): ClassFile {
        return try {
            ClassFile(file.nameWithoutExtension, Files.readString(Path.of(file.path)))
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}