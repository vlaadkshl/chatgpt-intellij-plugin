package com.sytoss.plugindemo.converters

import com.sytoss.plugindemo.bom.ClassFile
import com.sytoss.plugindemo.bom.PackageFinderDetails
import java.nio.file.Files

object FileConverter {

    fun filesToClassFiles(pyramidElems: MutableMap<String, PackageFinderDetails>): List<ClassFile> {
        val fileList = mutableListOf<ClassFile>()

        for ((type, value) in pyramidElems) {
            if (value.files.isNotEmpty()) {
                for (file in value.files) {
                    fileList.add(
                        ClassFile(
                            file.nameWithoutExtension,
                            Files.readString(file.toNioPath()),
                            type
                        )
                    )
                }
            }
        }

        return fileList
    }
}