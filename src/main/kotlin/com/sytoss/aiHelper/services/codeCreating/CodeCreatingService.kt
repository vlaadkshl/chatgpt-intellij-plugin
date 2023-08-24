package com.sytoss.aiHelper.services.codeCreating

import com.intellij.openapi.ui.Messages
import com.sytoss.aiHelper.bom.codeCreating.CreateResponse
import com.sytoss.aiHelper.bom.codeCreating.ElementType
import com.sytoss.aiHelper.services.CommonFields.applicationManager
import com.sytoss.aiHelper.services.CommonFields.dumbService
import com.sytoss.aiHelper.ui.components.CreatedClassesTree
import javax.swing.tree.DefaultMutableTreeNode

object CodeCreatingService {
    fun create(
        type: ElementType,
        tree: CreatedClassesTree,
        generateFun: ((CreateResponse) -> Unit) -> Unit
    ) {
        dumbService.smartInvokeLater {
            tree.toggleRootVisibility()
            tree.selectTypeRoot(type)
        }

        try {
            generateFun { response ->
                applicationManager.invokeAndWait {
                    for (generatedClass in response.result) {
                        tree.insertToTypeRoot(type, DefaultMutableTreeNode(generatedClass))
                    }

                    tree.fillEditorsByType(type, response)

                    tree.expandTypeRoot(type)
                }
            }
        } catch (e: Throwable) {
            dumbService.smartInvokeLater { Messages.showErrorDialog(e.message, "Error") }
        } finally {
            dumbService.smartInvokeLater {
                tree.toggleRootVisibility()
            }
        }
    }

    fun createBom(pumlContent: String?, showCallback: ((CreateResponse) -> Unit)) {
        pumlContent?.let { puml ->
            CodeCreatorWithChatServer.generateBomFromPuml(puml)?.let {
                showCallback(it)
            } ?: dumbService.smartInvokeLater {
                Messages.showInfoMessage(
                    "There were no DTOs generated.",
                    "${ElementType.BOM} Generating Error"
                )
            }
        } ?: dumbService.smartInvokeLater {
            Messages.showInfoMessage("No puml file was selected.", "${ElementType.BOM} Generating Error")
        }
    }

    fun createDtoFromBom(codes: List<String>, showCallback: (CreateResponse) -> Unit) {
        if (codes.isEmpty()) {
            dumbService.smartInvokeLater {
                Messages.showInfoMessage(
                    "There were no BOMs generated.",
                    "${ElementType.DTO} Generating Error"
                )
            }
            return
        }

        CodeCreatorWithChatServer.generateDtoFromBom(codes)?.let {
            showCallback(it)
        } ?: dumbService.smartInvokeLater {
            Messages.showInfoMessage(
                "There were no DTOs generated.",
                "${ElementType.DTO} Generating Error"
            )
        }
    }

    fun createDtoFromPuml(pumlContent: String?, showCallback: (CreateResponse) -> Unit) {
        pumlContent?.let { puml ->
            CodeCreatorWithChatServer.generateDtoFromPuml(puml)?.let {
                showCallback(it)
            } ?: dumbService.smartInvokeLater {
                Messages.showInfoMessage(
                    "There were no DTOs generated.",
                    "${ElementType.DTO} Generating Error"
                )
            }
        }
    }

    fun createConverters(bomTexts: List<String>, dtoTexts: List<String>, showCallback: ((CreateResponse) -> Unit)) {
        if (bomTexts.isEmpty() && dtoTexts.isEmpty()) {
            dumbService.smartInvokeLater {
                Messages.showInfoMessage(
                    "There were no BOMs generated.",
                    "${ElementType.CONVERTER} Generating Error"
                )
            }
            return
        }

        CodeCreatorWithChatServer.generateConverters(bomTexts, dtoTexts)?.let {
            showCallback(it)
        } ?: dumbService.smartInvokeLater {
            Messages.showInfoMessage(
                "There were no DTOs generated.",
                "${ElementType.CONVERTER} Generating Error"
            )
        }
    }
}