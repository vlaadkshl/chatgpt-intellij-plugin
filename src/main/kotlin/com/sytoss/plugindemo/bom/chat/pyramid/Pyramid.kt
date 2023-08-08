package com.sytoss.plugindemo.bom.chat.pyramid

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Pyramid(
    val converter: List<PyramidElem>? = null,

    val bom: List<PyramidElem>? = null,

    val dto: List<PyramidElem>? = null,

    @SerialName("interface")
    val interfaces: List<PyramidElem>? = null,

    val service: List<PyramidElem>? = null,

    val connector: List<PyramidElem>? = null,
)