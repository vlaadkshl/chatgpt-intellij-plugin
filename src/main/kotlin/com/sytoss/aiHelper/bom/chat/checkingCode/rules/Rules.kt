package com.sytoss.aiHelper.bom.chat.checkingCode.rules

import kotlinx.serialization.Serializable

@Serializable
data class Rules(
    val rules: List<Rule>
)
