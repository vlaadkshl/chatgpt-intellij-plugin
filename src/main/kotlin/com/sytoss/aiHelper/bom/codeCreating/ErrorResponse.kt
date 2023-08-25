package com.sytoss.aiHelper.bom.codeCreating

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(

    @SerialName("FileName")
    val fileName: String,

    val body: String
)