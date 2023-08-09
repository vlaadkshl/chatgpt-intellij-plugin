package com.sytoss.aiHelper.bom.chat.pyramid.result

import kotlinx.serialization.Serializable

@Serializable
data class PyramidAnalysisResult(
    var result: MutableList<PyramidAnalysisGroup>
)