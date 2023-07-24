package com.sytoss.plugindemo.converters

import com.sytoss.plugindemo.bom.ClassFile
import com.sytoss.plugindemo.bom.FolderSearchingElems
import java.nio.file.Files

object FileConverter {

    fun filesToClassFile(pyramidElems: MutableMap<String, FolderSearchingElems>): List<ClassFile> {
        val fileList = mutableListOf<ClassFile>()

        for ((_, value) in pyramidElems) {
            if (value.files.isNotEmpty()) {
                for (file in value.files) {
                    fileList.add(
                        ClassFile(
                            file.nameWithoutExtension,
                            Files.readString(file.toNioPath())
                        )
                    )
                }
            }
        }

        return fileList
    }
}