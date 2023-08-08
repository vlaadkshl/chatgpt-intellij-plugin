package com.sytoss.plugindemo.bom

import kotlinx.serialization.Serializable

@Serializable
data class PyramidReport(
    val type: ReportClassType,
    val name: String,
    val warning: String
) {
    enum class ReportClassType {
        FIELD,
        METHOD
    }
}
