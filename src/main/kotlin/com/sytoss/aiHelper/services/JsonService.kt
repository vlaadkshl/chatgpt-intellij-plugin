package com.sytoss.aiHelper.services

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException

object JsonService {

    inline fun <reified T> fromJsonResourceFile(fileName: String): T {
        val file = javaClass.getResource(fileName) ?: throw FileNotFoundException()
        return Json.decodeFromString<T>(file.readText())
    }
}