package com.sytoss.plugindemo.services.chat

import com.intellij.openapi.vfs.VirtualFile
import com.sytoss.plugindemo.bom.ClassFile
import com.sytoss.plugindemo.bom.pyramid.Pyramid
import com.theokanning.openai.completion.chat.ChatMessage

object PyramidCheckingService : ChatAnalysisAbstractService() {

    var pyramidFile: VirtualFile? = null

    var pyramid: Pyramid? = null

    fun analysePyramid(selectedFiles: List<ClassFile>): String {
        val messages = createUserMessages(selectedFiles)
        val request = buildRequest(messages)
        return sendRequestToChat(request)
    }

    override fun createUserMessages(selectedFiles: List<ClassFile>): List<ChatMessage> {
        if (pyramid == null) {
            throw IllegalStateException("No pyramid was found!")
        }

        return mutableListOf()
    }
}