package com.sytoss.plugindemo.bom

import kotlinx.serialization.Serializable

@Serializable
data class PyramidAnalysisResult(
    var result: MutableList<PyramidClassReport>
)