package com.sytoss.aiHelper.bom.codeCreating

import kotlinx.serialization.Serializable

@Serializable
data class CreateRequest(
    val model: ModelType,
    val prompt: String,
    val example: String
)