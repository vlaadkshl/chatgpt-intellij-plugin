package com.sytoss.plugindemo.bom.warnings

import kotlinx.serialization.Serializable

@Serializable
data class ClassGroup(
    val className: String,
    val warnings: List<ClassWarning>
) : ClassGroupTemplate(className)