package com.sytoss.plugindemo.bom.chat.warnings

import kotlinx.serialization.Serializable

@Serializable
data class ClassGroup(
    override val className: String,
    val warnings: List<ClassWarning>
) : ClassGroupTemplate()