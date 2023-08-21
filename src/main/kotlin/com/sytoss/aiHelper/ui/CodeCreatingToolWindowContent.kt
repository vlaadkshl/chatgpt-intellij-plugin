package com.sytoss.aiHelper.ui

import com.intellij.openapi.ui.Messages
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.components.BorderLayoutPanel
import com.sytoss.aiHelper.bom.codeCreating.CreateResponse
import com.sytoss.aiHelper.bom.codeCreating.ElementType
import com.sytoss.aiHelper.services.CommonFields.applicationManager
import com.sytoss.aiHelper.services.CommonFields.dumbService
import com.sytoss.aiHelper.services.codeCreating.CodeCreatingService
import com.sytoss.aiHelper.services.codeCreating.CodeCreatingService.isNeedContinue
import com.sytoss.aiHelper.ui.components.*
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.nio.file.Files
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.concurrent.thread

class CodeCreatingToolWindowContent {
    val contentPanel = OnePixelSplitter()

    private val mainPanel = JPanel(GridBagLayout())

    private val codePanel = BorderLayoutPanel()

    private val loadingLabel = JBLabel("Loading...", AnimatedIcon.Default(), SwingConstants.LEFT)

    private val tree = CreatedClassesTree(codePanel)

    private val pumlChooser = FileChooserCreateComponent("Choose PlantUML file", "puml")

    private val elemsToGenerate = mutableSetOf<ElementType>()

    private var isBomCheckboxSelected = false

    private var isDtoCheckboxSelected = false

    private fun isConverterNeedsEnabling() = isBomCheckboxSelected && isDtoCheckboxSelected

    private val converterCheckBox = JBCheckBoxWithListener(ElementType.CONVERTER.value) {
        val source = it.source as JBCheckBox
        if (source.isSelected && source.isEnabled) {
            elemsToGenerate.add(ElementType.CONVERTER)
        } else {
            elemsToGenerate.remove(ElementType.CONVERTER)
        }
    }

    private val bomCheckBox = JBCheckBoxWithListener(ElementType.BOM.value) {
        isBomCheckboxSelected = (it.source as JBCheckBox).isSelected

        converterCheckBox.isEnabled = isConverterNeedsEnabling()

        if (isBomCheckboxSelected) {
            elemsToGenerate.add(ElementType.BOM)
        } else {
            elemsToGenerate.remove(ElementType.BOM)
        }
    }

    private val dtoCheckBox = JBCheckBoxWithListener(ElementType.DTO.value) {
        isDtoCheckboxSelected = (it.source as JBCheckBox).isSelected

        converterCheckBox.isEnabled = isConverterNeedsEnabling()

        if (isDtoCheckboxSelected) {
            elemsToGenerate.add(ElementType.DTO)
        } else {
            elemsToGenerate.remove(ElementType.DTO)
        }
    }

    private fun checkBeforeGenerating(): Boolean {
        if (elemsToGenerate.isEmpty()) {
            Messages.showInfoMessage("There is no types of files to create.", "Create Error")
            return false
        }
        if (pumlChooser.selectedFiles.isEmpty()) {
            Messages.showInfoMessage("There is no .puml file.", "Create Error")
            return false
        }
        return true
    }

    private fun createElements(
        continuable: Boolean,
        elementType: ElementType,
        generateFun: ((CreateResponse) -> Unit) -> Unit
    ) {
        CodeCreatingService.create(continuable, elementType, loadingLabel, tree) { showCallback ->
            generateFun(
                showCallback
            )
        }
    }

    private fun createBom(showCallback: ((CreateResponse) -> Unit)) {
        var pumlContent: String? = null
        applicationManager.invokeAndWait {
            pumlContent = Files.readString(pumlChooser.selectedFiles[0].toNioPath())
        }

        pumlContent?.let { puml ->
            CodeCreatingService.generateBomFromPuml(puml)?.let {
                showCallback(it)
            } ?: dumbService.smartInvokeLater {
                Messages.showInfoMessage(
                    "There were no DTOs generated.",
                    "${ElementType.BOM.value} Generating Error"
                )
            }
        } ?: dumbService.smartInvokeLater {
            Messages.showInfoMessage("No puml file was selected.", "${ElementType.BOM.value} Generating Error")
        }

    }

    private fun createDto(showCallback: ((CreateResponse) -> Unit)) {
        if (tree.hasBom()) {
            val editorTexts = tree.getTextsFromEditors(ElementType.BOM)

            if (editorTexts.isEmpty()) {
                dumbService.smartInvokeLater {
                    Messages.showInfoMessage(
                        "There were no BOMs generated.",
                        "${ElementType.DTO.value} Generating Error"
                    )
                }
                return
            }

            CodeCreatingService.generateDtoFromBom(editorTexts)?.let {
                showCallback(it)
            } ?: dumbService.smartInvokeLater {
                Messages.showInfoMessage(
                    "There were no DTOs generated.",
                    "${ElementType.DTO.value} Generating Error"
                )
            }
        } else {
            var pumlContent: String? = null
            applicationManager.invokeAndWait {
                pumlContent = Files.readString(pumlChooser.selectedFiles[0].toNioPath())
            }

            pumlContent?.let { puml ->
                CodeCreatingService.generateDtoFromPuml(puml)?.let {
                    showCallback(it)
                } ?: dumbService.smartInvokeLater {
                    Messages.showInfoMessage(
                        "There were no DTOs generated.",
                        "${ElementType.DTO.value} Generating Error"
                    )
                }
            }
        }
    }

    private fun createConverters(showCallback: ((CreateResponse) -> Unit)) {
        if (!(tree.hasBom() || tree.hasDto())) {
            dumbService.smartInvokeLater {
                Messages.showInfoMessage(
                    "There were no BOMs and DTOs generated.",
                    "${ElementType.CONVERTER.value} Generating Error"
                )
            }
            return
        }

        val bomTexts = tree.getTextsFromEditors(ElementType.BOM)
        val dtoTexts = tree.getTextsFromEditors(ElementType.DTO)

        if (bomTexts.isEmpty() && dtoTexts.isEmpty()) {
            dumbService.smartInvokeLater {
                Messages.showInfoMessage(
                    "There were no BOMs generated.",
                    "${ElementType.CONVERTER.value} Generating Error"
                )
            }
            return
        }

        CodeCreatingService.generateConverters(bomTexts, dtoTexts)?.let {
            showCallback(it)
        } ?: dumbService.smartInvokeLater {
            Messages.showInfoMessage(
                "There were no DTOs generated.",
                "${ElementType.CONVERTER.value} Generating Error"
            )
        }
    }

    private fun generate(elemToGenerate: ElementType) {
        val continuable = elemToGenerate != elemsToGenerate.last()

        createElements(continuable, elemToGenerate) { showCallback ->
            when (elemToGenerate) {
                ElementType.BOM -> createBom(showCallback)
                ElementType.DTO -> createDto(showCallback)
                ElementType.CONVERTER -> createConverters(showCallback)
            }
        }

        while (!isNeedContinue) {
            Thread.sleep(100)
        }

        isNeedContinue = false
    }


    private val generateBtn = JButtonWithListener("Generate Files") {
        if (!checkBeforeGenerating()) return@JButtonWithListener

        tree.editorsByType.clear()

        thread {
            elemsToGenerate.forEach { generate(it) }
        }
    }

    init {
        converterCheckBox.isEnabled = isConverterNeedsEnabling()

        /*
        * CONTROL PANEL
        * */

        val mainBorderLayout = BorderLayoutPanel()

        //  PUML CHOOSER
        mainPanel.add(pumlChooser, DefaultConstraints.topLeftColumn)

        //  CHECKBOXES
        val checkboxGroup = JPanel(GridBagLayout())
        checkboxGroup.add(converterCheckBox, DefaultConstraints.checkbox)
        checkboxGroup.add(bomCheckBox, DefaultConstraints.checkboxInsets)
        checkboxGroup.add(dtoCheckBox, DefaultConstraints.checkboxInsets)

        mainPanel.add(
            checkboxGroup, GridBagConstraints(
                0, GridBagConstraints.RELATIVE,
                1, 1,
                1.0, 0.0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.BOTH,
                JBInsets(0, 0, 0, 0),
                0, 0
            )
        )

        //  LEFT ALIGNMENT OF CONTROLS
        val mainPanelWrapper = JPanel(FlowLayout(FlowLayout.LEFT))
        mainPanelWrapper.add(mainPanel)

        //  BOTTOM BUTTONS
        val btnPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        btnPanel.add(generateBtn)

        mainBorderLayout.addToCenter(mainPanelWrapper)
        mainBorderLayout.addToBottom(btnPanel)

        //  SET CONTROL PANEL
        contentPanel.firstComponent = ScrollWithInsets { mainBorderLayout }

        /*
        * CONTENT CREATOR PANEL
        * */

        val treeViewer = OnePixelSplitter()

        //  TREE
        treeViewer.firstComponent = ScrollWithInsets { tree }
        tree.toggleRootVisibility()

        //  NODE VIEWER
        val nodeViewerPanel = BorderLayoutPanel()
        nodeViewerPanel.addToCenter(ScrollWithInsets { codePanel })

        loadingLabel.isVisible = false
        nodeViewerPanel.addToTop(loadingLabel)

        treeViewer.secondComponent = nodeViewerPanel

        //  SET TREE COMPONENT
        contentPanel.secondComponent = treeViewer
    }
}