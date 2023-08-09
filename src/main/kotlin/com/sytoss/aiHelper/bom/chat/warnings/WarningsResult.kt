package com.sytoss.aiHelper.bom.chat.warnings

import kotlinx.serialization.Serializable

@Serializable
data class WarningsResult(

    var result: MutableList<ClassGroup>
)