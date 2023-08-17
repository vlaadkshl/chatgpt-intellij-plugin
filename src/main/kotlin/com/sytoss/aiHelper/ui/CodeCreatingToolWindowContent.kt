package com.sytoss.aiHelper.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.ui.Messages
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.components.BorderLayoutPanel
import com.sytoss.aiHelper.bom.codeCreating.ElementType
import com.sytoss.aiHelper.services.CommonFields.project
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

    private val converterCheckBox = JBCheckBoxWithListener("Converter") {
        val source = it.source as JBCheckBox
        if (source.isSelected && source.isEnabled) {
            elemsToGenerate.add(ElementType.CONVERTER)
        } else {
            elemsToGenerate.remove(ElementType.CONVERTER)
        }
    }

    private val bomCheckBox = JBCheckBoxWithListener("BOM") {
        isBomCheckboxSelected = (it.source as JBCheckBox).isSelected

        converterCheckBox.isEnabled = isConverterNeedsEnabling()

        if (isBomCheckboxSelected) {
            elemsToGenerate.add(ElementType.BOM)
        } else {
            elemsToGenerate.remove(ElementType.BOM)
        }
    }

    private val dtoCheckBox = JBCheckBoxWithListener("DTO") {
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

    private fun createBom(continuable: Boolean): MutableMap<String, Editor> =
        Creators.create(continuable, "BOM", tabbedPane) { showCallback ->
            var pumlContent: String? = null
            ApplicationManager.getApplication().invokeAndWait {
                pumlContent = Files.readString(pumlChooser.selectedFiles[0].toNioPath())
            }

            pumlContent?.let { puml ->
                Creators.createBom(puml)?.let {
                    showCallback(it)
                } ?: DumbService.getInstance(project).smartInvokeLater {
                    Messages.showInfoMessage("There were no DTOs generated.", "BOM Generating Error")
                }
            }?: DumbService.getInstance(project).smartInvokeLater {
                Messages.showInfoMessage("No puml file was selected.", "BOM Generating Error")
            }
        }

    private fun createDto(continuable: Boolean): MutableMap<String, Editor> =
        Creators.create(continuable, "DTO", tabbedPane) { showCallback ->
            if (generateEditors.containsKey(ElementType.BOM)) {
                val editorTexts = generateEditors[ElementType.BOM]?.values?.map { it.document.text }
                if (editorTexts != null) {
                    Creators.createDto(editorTexts)?.let {
                        showCallback(it)
                    } ?: DumbService.getInstance(project).smartInvokeLater {
                        Messages.showInfoMessage("There were no DTOs generated.", "DTO Generating Error")
                    }
                } else {
                    DumbService.getInstance(project).smartInvokeLater {
                        Messages.showInfoMessage("There were no BOMs generated.", "DTO Generating Error")
                    }
                }
            } else {
                var pumlContent: String? = null
                ApplicationManager.getApplication().invokeAndWait {
                    pumlContent = Files.readString(pumlChooser.selectedFiles[0].toNioPath())
                }

                pumlContent?.let { puml ->
                    Creators.createDto(puml)?.let {
                        showCallback(it)
                    } ?: DumbService.getInstance(project).smartInvokeLater {
                        Messages.showInfoMessage("There were no DTOs generated.", "DTO Generating Error")
                    }
                }
            }
        }

    private val generateBtn = JButtonWithListener("Generate Files") {
        if (!checkBeforeGenerating()) return@JButtonWithListener

        generateEditors.clear()
        tabbedPane.removeAll()
        thread {
            for (elemToGenerate in elemsToGenerate) {
                val continuable = elemToGenerate != elemsToGenerate.last()

                when (elemToGenerate) {
                    ElementType.BOM -> {
                        createBom(continuable).let { generateEditors[elemToGenerate] = it }
                    }

                    ElementType.DTO -> {
                        createDto(continuable).let { generateEditors[elemToGenerate] = it }
                    }

                    ElementType.CONVERTER -> {
                        TODO()
                    }
                }

                while (!isNeedContinue) {
                    Thread.sleep(100)
                }

                isNeedContinue = false
            }
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