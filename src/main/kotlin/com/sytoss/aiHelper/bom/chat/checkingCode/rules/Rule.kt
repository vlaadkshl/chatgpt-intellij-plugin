package com.sytoss.aiHelper.bom.chat.checkingCode.rules

import kotlinx.serialization.Serializable

@Serializable
data class Rule(
    val name: String,
    val rule: String,
    val fileTypes: List<String>
)