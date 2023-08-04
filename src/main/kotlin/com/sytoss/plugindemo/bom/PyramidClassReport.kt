package com.sytoss.plugindemo.bom

import com.sytoss.plugindemo.bom.warnings.ClassGroupTemplate
import kotlinx.serialization.Serializable

@Serializable
data class PyramidClassReport(
    val className: String,
    val report: MutableList<PyramidReport>
) : ClassGroupTemplate(className)
