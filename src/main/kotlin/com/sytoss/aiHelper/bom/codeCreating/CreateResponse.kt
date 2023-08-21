package com.sytoss.aiHelper.bom.codeCreating

import kotlinx.serialization.Serializable

@Serializable
data class CreateResponse(
    val result: List<CreateContent> = mutableListOf()
) {
    @Serializable
    data class CreateContent(
        val fileName: String,
        val body: String
    ) {
        override fun toString() = fileName
    }
}