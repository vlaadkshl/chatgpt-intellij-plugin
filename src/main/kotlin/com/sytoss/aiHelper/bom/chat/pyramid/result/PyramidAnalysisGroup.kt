package com.sytoss.aiHelper.bom.chat.pyramid.result

import com.sytoss.aiHelper.bom.chat.warnings.ClassGroupTemplate
import kotlinx.serialization.Serializable

@Serializable
data class PyramidAnalysisGroup(
    override val className: String,
    val report: MutableList<PyramidAnalysisContent>
) : ClassGroupTemplate()
