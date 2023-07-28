package com.sytoss.plugindemo.bom.rules

import kotlinx.serialization.Serializable

@Serializable
data class Rule(
    val name: String,
    val rule: String,
    val fileTypes: List<String>
)