package com.sytoss.plugindemo.bom.types

import kotlinx.serialization.Serializable

@Serializable
data class FileTypes(
    val fileTypes: List<String>
)
