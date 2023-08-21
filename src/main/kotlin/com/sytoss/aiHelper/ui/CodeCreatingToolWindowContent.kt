package com.sytoss.aiHelper.ui

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.Messages
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.components.BorderLayoutPanel
import com.sytoss.aiHelper.bom.codeCreating.CreateResponse
import com.sytoss.aiHelper.bom.codeCreating.ElementType
import com.sytoss.aiHelper.services.CommonFields.applicationManager
import com.sytoss.aiHelper.services.CommonFields.dumbService
import com.sytoss.aiHelper.services.codeCreating.Creators
import com.sytoss.aiHelper.services.codeCreating.Creators.isNeedContinue
import com.sytoss.aiHelper.ui.components.*
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.nio.file.Files
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.concurrent.thread

class CodeCreatingToolWindowContent {
    val contentPanel = OnePixelSplitter()

    private val mainPanel = JPanel(GridBagLayout())

    private val tabbedPane = JBTabbedPane()

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

    private val generateEditors = mutableMapOf<ElementType, MutableMap<String, Editor>>()

    private fun getTextsFromEditors(elementType: ElementType): List<String> =
        generateEditors[elementType]?.values?.map { it.document.text } ?: mutableListOf()

    private fun createElements(
        continuable: Boolean,
        tabComponent: BorderLayoutPanel,
        componentIndex: Int,
        generateFun: ((CreateResponse) -> Unit) -> Unit
    ): MutableMap<String, Editor> =
        Creators.create(
            continuable,
            tabbedPane,
            tabComponent,
            componentIndex
        ) { showCallback -> generateFun(showCallback) }

    private fun createBom(showCallback: ((CreateResponse) -> Unit)) {
        var pumlContent: String? = null
        applicationManager.invokeAndWait {
            pumlContent = Files.readString(pumlChooser.selectedFiles[0].toNioPath())
        }

        pumlContent?.let { puml ->
            Creators.createBom(puml)?.let {
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
        if (generateEditors.containsKey(ElementType.BOM)) {
            val editorTexts = getTextsFromEditors(ElementType.BOM)

            if (editorTexts.isEmpty()) {
                dumbService.smartInvokeLater {
                    Messages.showInfoMessage(
                        "There were no BOMs generated.",
                        "${ElementType.DTO.value} Generating Error"
                    )
                }
                return
            }

            Creators.createDto(editorTexts)?.let {
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
                Creators.createDto(puml)?.let {
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
        if (!(generateEditors.contains(ElementType.BOM) || generateEditors.contains(ElementType.DTO))) {
            dumbService.smartInvokeLater {
                Messages.showInfoMessage(
                    "There were no BOMs and DTOs generated.",
                    "${ElementType.CONVERTER.value} Generating Error"
                )
            }
            return
        }

        val bomTexts = getTextsFromEditors(ElementType.BOM)
        val dtoTexts = getTextsFromEditors(ElementType.DTO)

        if (bomTexts.isEmpty() && dtoTexts.isEmpty()) {
            dumbService.smartInvokeLater {
                Messages.showInfoMessage(
                    "There were no BOMs generated.",
                    "${ElementType.CONVERTER.value} Generating Error"
                )
            }
            return
        }

        Creators.createConverters(bomTexts, dtoTexts)?.let {
            showCallback(it)
        } ?: dumbService.smartInvokeLater {
            Messages.showInfoMessage(
                "There were no DTOs generated.",
                "${ElementType.CONVERTER.value} Generating Error"
            )
        }
    }

    private val tabComponents = mutableListOf<Pair<ElementType, BorderLayoutPanel>>()

    private fun generate(elemToGenerate: ElementType) {
        val continuable = elemToGenerate != elemsToGenerate.last()

        val component = tabComponents.find { it.first == elemToGenerate } ?: return
        val componentIndex = tabComponents.indexOf(component)

        createElements(
            continuable,
            tabComponent = component.second,
            componentIndex
        ) { showCallback ->
            when (elemToGenerate) {
                ElementType.BOM -> createBom(showCallback)
                ElementType.DTO -> createDto(showCallback)
                ElementType.CONVERTER -> createConverters(showCallback)
            }
        }.let {
            generateEditors[elemToGenerate] = it
        }

        while (!isNeedContinue) {
            Thread.sleep(100)
        }

        isNeedContinue = false
    }


    private val generateBtn = JButtonWithListener("Generate Files") {
        if (!checkBeforeGenerating()) return@JButtonWithListener

        generateEditors.clear()
        tabbedPane.removeAll()

        var i = 0
        elemsToGenerate.forEach {
            tabComponents.add(Pair(it, BorderLayoutPanel()))
            tabbedPane.addTab(it.value, tabComponents[i].second)
            tabbedPane.setEnabledAt(i, false)
            i++
        }

        thread {
            elemsToGenerate.forEach { generate(it) }
        }
    }

    init {
        converterCheckBox.isEnabled = isConverterNeedsEnabling()

        val mainBorderLayout = BorderLayoutPanel()

        addWithConstraints(pumlChooser)
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

        val mainPanelWrapper = JPanel(FlowLayout(FlowLayout.LEFT))
        mainPanelWrapper.add(mainPanel)

        val btnPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        btnPanel.add(generateBtn)

        mainBorderLayout.addToCenter(mainPanelWrapper)
        mainBorderLayout.addToBottom(btnPanel)

        contentPanel.firstComponent = ScrollWithInsets { mainBorderLayout }

        contentPanel.secondComponent = tabbedPane
    }

    private fun addWithConstraints(component: JComponent) {
        mainPanel.add(component, DefaultConstraints.topLeftColumn)
    }
}