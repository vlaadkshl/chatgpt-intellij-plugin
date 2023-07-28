package com.sytoss.plugindemo.bom.rules

import kotlinx.serialization.Serializable

@Serializable
data class Rules(
    val rules: List<Rule>
)
