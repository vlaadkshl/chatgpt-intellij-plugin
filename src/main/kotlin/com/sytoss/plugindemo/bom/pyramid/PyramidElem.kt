package com.sytoss.plugindemo.bom.pyramid

import kotlinx.serialization.Serializable

@Serializable
data class PyramidElem(
    val name: String,

    val add: List<String>? = null,

    val edit: List<String>? = null,

    val remove: List<String>? = null,
)