package com.sytoss.aiHelper.services.chat

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.sytoss.aiHelper.bom.chat.ChatMessageClassData
import com.sytoss.aiHelper.bom.chat.warnings.ClassGroupTemplate
import com.sytoss.aiHelper.services.CommonFields.project
import com.sytoss.aiHelper.ui.components.DefaultConstraints
import com.theokanning.openai.client.OpenAiApi
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.service.OpenAiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.time.Duration

abstract class ChatAnalysisAbstractService {

    private val openAiApi: OpenAiApi

    val constraints = DefaultConstraints.topLeftColumn

    init {
        val key = javaClass.getResource("/key")?.readText()
            ?: throw IllegalStateException("The OpenAI API key doesn't exist")
        openAiApi = OpenAiService.buildApi(key, Duration.ofSeconds(60L))
    }

    protected fun buildRequest(messages: List<ChatMessage>): ChatCompletionRequest =
        ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo-16k")
            .temperature(0.0)
            .messages(messages)
            .build()

    protected suspend fun sendRequestToChat(request: ChatCompletionRequest?): String {
        return withContext(Dispatchers.IO) {
            async {
                openAiApi.createChatCompletion(request).blockingGet()
            }.await()
        }?.choices?.get(0)?.message?.content
            ?: throw RuntimeException("Can't get response")
    }

    fun getClassVirtualFile(warningClass: ClassGroupTemplate): VirtualFile? {
        val qualifiedName = warningClass.className
        val psiClass = JavaPsiFacade.getInstance(project).findClass(
            qualifiedName,
            GlobalSearchScope.projectScope(project)
        )
        return psiClass?.containingFile?.virtualFile
    }

    abstract fun createUserMessages(selectedFiles: List<ChatMessageClassData>): List<ChatMessage>
}