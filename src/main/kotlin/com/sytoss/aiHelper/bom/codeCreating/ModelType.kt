package com.sytoss.aiHelper.bom.codeCreating

import com.sytoss.aiHelper.services.codeCreating.ModelTypeSerializer
import kotlinx.serialization.Serializable

@Serializable(with = ModelTypeSerializer::class)
enum class ModelType(val value: String) {
    WIZARD("wizard"),
    GPT("gpt"),
    DAVINCI("davinci")
}