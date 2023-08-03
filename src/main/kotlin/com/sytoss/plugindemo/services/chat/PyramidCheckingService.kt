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

        val systemMessage = """
            |You are a helpful assistant.
            |You check classes for compliance with given conditions
            |Conditions are instructions of what should or should not be in these classes
            |User gives you conditions in JSON format like this:
            |{
            |   "converter": [{
            |       "name": "{some class name}",
            |       "add": [
            |           "{some field or method}",
            |           "{some field or method}"
            |       ],
            |       "remove": [
            |           "{some field or method}"
            |       ]
            |   }],
            |   "bom": [{the same as in converter}],
            |   "dto": [{the same as in converter}],
            |   "interface": [{the same as in converter}],
            |   "service": [{the same as in converter}],
            |   "connector": [{the same as in converter}]
            |}
            |
            |Show comments in JSON format like this:
            |{
            |    "result": [
            |        "className": "package.ClassName",
            |        "warnings": [{
            |            "warning": "{Place Warning here}",
            |            "lineInCode": "{place line with error here}"
            |        }]
            |    ]
            |}
            |If there is no errors for class, don't put it in result
        """.trimMargin()

        return mutableListOf()
    }
}