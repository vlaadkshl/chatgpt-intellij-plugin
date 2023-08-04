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
            |You check classes for given conditions.
            |Conditions are instructions of what should or should not be in these classes.
            |User gives you conditions in JSON format like this:
            |{
            |   "converter": [{
            |       "name": "{some class name}",
            |       "add": [
            |           "{some field or method}",
            |           "{some field or method}"
            |       ],
            |       "change": [
            |           "{some field or method}"
            |       ],
            |       "remove": [
            |           "{some field or method}"
            |       ]
            |   }, {
            |       "name": "{some class name}",
            |       "add": [
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
            |Also user gives you classes with their type in this format:
            |[{
            |   "class": "{some class name}",
            |   "type": "{something from this: converter|bom|dto|interface|service|connector}",
            |   "content": "{some program code}"
            |}, {
            |   "class": "{some class name}",
            |   "type": "{something from this: converter|bom|dto|interface|service|connector}",
            |   "content": "{some program code}"
            |}]
            |
            |Algorithm of checking is:
            |1. If this class doesn't exist in conditions, you skip it.
            |2. If some field or method exists in "add" or "change", you check, if it exists in appropriate class.
            |3. If some field or method exists in "remove", you check, if it doesn't exist in appropriate class.
            |
            |Show analysis result in JSON format like this:
            |{
            |    "result": [
            |        "className": "package.ClassName",
            |        "report": [{
            |            "type": "{FIELD or METHOD}",
            |            "name": "{Place name of FIELD or METHOD}",
            |            "warning": "{Place Warning here}"
            |        }]
            |    ]
            |}
            |If there is no errors for class, don't put it in result
        """.trimMargin()

        return mutableListOf()
    }
}