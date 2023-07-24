package com.sytoss.plugindemo.bom.pyramid

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Pyramid(
    val converter: List<PyramidElem>,

    val bom: List<PyramidElem>,

    val dto: List<PyramidElem>,

    @SerialName("interface")
    val interfaces: List<PyramidElem>,

    val service: List<PyramidElem>,

    val connector: List<PyramidElem>,
)
