package com.sytoss.plugindemo.services.chat

import com.intellij.openapi.vfs.VirtualFile
import com.sytoss.plugindemo.bom.ClassFile
import com.sytoss.plugindemo.bom.pyramid.Pyramid
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole

object PyramidCheckingService : ChatAbstractService() {

    var pyramidFile: VirtualFile? = null

    var pyramid: Pyramid? = null

    fun analysePyramid(selectedFiles: List<ClassFile>): String {
        val request = ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(
                listOf(
                    ChatMessage(
                        ChatMessageRole.USER.value(), createPyramidAnalysisRequest()
                    )
                )
            )
            .build()

        return sendRequestToChat(request)
    }

    private fun createPyramidAnalysisRequest(): String {
        val requestBuilder = StringBuilder()

        return requestBuilder.toString()
    }
}