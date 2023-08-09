package com.sytoss.aiHelper.bom.chat.warnings

import kotlinx.serialization.Serializable

@Serializable
abstract class ClassGroupTemplate {
    abstract val className: String
}
