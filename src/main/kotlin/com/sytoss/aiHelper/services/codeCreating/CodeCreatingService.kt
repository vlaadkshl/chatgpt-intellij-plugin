package com.sytoss.aiHelper.services.codeCreating

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.sytoss.aiHelper.bom.codeCreating.CreateResponse
import com.sytoss.aiHelper.bom.codeCreating.ElementType
import com.sytoss.aiHelper.services.CommonFields.applicationManager
import com.sytoss.aiHelper.services.CommonFields.dumbService
import com.sytoss.aiHelper.services.UiBuilder
import com.sytoss.aiHelper.ui.components.CreatedClassesTree
import com.sytoss.aiHelper.ui.components.JButtonWithListener
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode

object CodeCreatingService {


    private lateinit var retryButton: JButtonWithListener

    private lateinit var editorsResultMap: MutableMap<String, Editor>

    private val editors = mutableMapOf<String, Editor>()

    fun create(
        type: ElementType,
        loadingLabel: JBLabel,
        tree: CreatedClassesTree,
        generateFun: ((CreateResponse) -> Unit) -> Unit
    ) {
        dumbService.smartInvokeLater {
            loadingLabel.isVisible = true
            tree.toggleRootVisibility()
        }

        try {
            generateFun { response ->
                applicationManager.invokeAndWait {
                    for (generatedClass in response.result) {
                        tree.insertToTypeRoot(type, DefaultMutableTreeNode(generatedClass))
                    }

                    tree.fillEditorsByType(type, response)
                }
            }
        } catch (e: Throwable) {
            dumbService.smartInvokeLater { Messages.showErrorDialog(e.message, "Error") }
        } finally {
            dumbService.smartInvokeLater {
                loadingLabel.isVisible = false
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

    private fun retryFun(
        generateFun: ((CreateResponse) -> Unit) -> Unit,
        innerPanel: JPanel,
        continueButton: JButtonWithListener,
        loadingLabel: JBLabel
    ) {
        try {
            dumbService.smartInvokeLater {
                loadingLabel.isVisible = true
                innerPanel.isVisible = false
            }

            generateFun { response ->
                applicationManager.invokeAndWait {
                    editorsResultMap.keys.forEach { editors.remove(it) }
                    innerPanel.removeAll()
                    innerPanel.isVisible = true

                    editors.putAll(UiBuilder.buildCreateClassesPanel(response, innerPanel))
                    continueButton.isEnabled = true
                }
            }
        } catch (e: Throwable) {
            dumbService.smartInvokeLater { Messages.showErrorDialog(e.message, "Error") }
        } finally {
            dumbService.smartInvokeLater {
                loadingLabel.isVisible = false
                retryButton.isEnabled = true
            }
        }
    }
}