package com.sytoss.aiHelper.services.chat

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.sytoss.aiHelper.bom.chat.ChatMessageClassData
import com.sytoss.aiHelper.bom.chat.warnings.ClassGroupTemplate
import com.theokanning.openai.client.OpenAiApi
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.service.OpenAiService
import java.time.Duration

abstract class ChatAnalysisAbstractService {

    private val openAiApi: OpenAiApi

    init {
        val key = javaClass.getResource("/key")?.readText()
            ?: throw IllegalStateException("The OpenAI API key doesn't exist")
        openAiApi = OpenAiService.buildApi(key, Duration.ofSeconds(60L))
    }

    protected fun buildRequest(messages: List<ChatMessage>): ChatCompletionRequest =
        ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo-16k")
            .messages(messages)
            .build()

    protected fun sendRequestToChat(request: ChatCompletionRequest?): String {
        val response = openAiApi.createChatCompletion(request).blockingGet()
        return response?.choices?.get(0)?.message?.content ?: throw RuntimeException("Can't get response")
    }

    protected fun getClassVirtualFile(warningClass: ClassGroupTemplate, project: Project): VirtualFile? {
        val qualifiedName = warningClass.className
        val psiClass = JavaPsiFacade.getInstance(project).findClass(
            qualifiedName,
            GlobalSearchScope.projectScope(project)
        )
        return psiClass?.containingFile?.virtualFile
    }

    abstract fun createUserMessages(selectedFiles: List<ChatMessageClassData>): List<ChatMessage>
}