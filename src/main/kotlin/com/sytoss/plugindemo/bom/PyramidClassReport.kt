package com.sytoss.plugindemo.bom

import kotlinx.serialization.Serializable

@Serializable
data class PyramidClassReport(
    val className: String,
    val report: MutableList<PyramidReport>
)
