package com.sytoss.plugindemo.bom

import kotlinx.serialization.Serializable

@Serializable
data class FileTypes(
    val fileTypes: List<String>
)
