package com.sytoss.plugindemo.bom.warnings

import kotlinx.serialization.Serializable

@Serializable
abstract class ClassGroupTemplate {
    abstract val className: String
}
