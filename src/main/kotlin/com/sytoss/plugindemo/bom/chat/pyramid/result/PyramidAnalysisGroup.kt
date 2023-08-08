package com.sytoss.plugindemo.bom.chat.pyramid.result

import com.sytoss.plugindemo.bom.chat.warnings.ClassGroupTemplate
import kotlinx.serialization.Serializable

@Serializable
data class PyramidAnalysisGroup(
    override val className: String,
    val report: MutableList<PyramidAnalysisContent>
) : ClassGroupTemplate()
