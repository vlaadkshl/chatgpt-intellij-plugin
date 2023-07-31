package com.sytoss.plugindemo.bom.warnings

import kotlinx.serialization.Serializable

@Serializable
data class WarningsResult(

    var result: MutableList<ClassGroup>
)