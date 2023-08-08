package com.sytoss.plugindemo.bom.chat.warnings

import kotlinx.serialization.Serializable

@Serializable
data class ClassWarning(
    val warning: String,
    val lineInCode: String,
    val lineNumber: Int? = null
)
