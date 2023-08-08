package com.sytoss.aiHelper.bom.chat.pyramid.result

import kotlinx.serialization.Serializable

@Serializable
data class PyramidAnalysisContent(
    val type: ReportClassType,
    val name: String,
    val warning: String
) {
    enum class ReportClassType {
        FIELD,
        METHOD
    }
}
