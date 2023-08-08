package com.sytoss.aiHelper.bom.chat

data class ChatMessageClassData(
    val fileName: String,
    val content: String,
    val type: String,
    var rules: List<String> = mutableListOf()
)