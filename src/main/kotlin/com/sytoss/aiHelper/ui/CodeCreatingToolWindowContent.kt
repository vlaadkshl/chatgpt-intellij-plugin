package com.sytoss.aiHelper.ui

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.components.BorderLayoutPanel
import com.sytoss.aiHelper.bom.codeCreating.ElementType
import com.sytoss.aiHelper.services.UiBuilder
import com.sytoss.aiHelper.services.codeCreating.Creators
import com.sytoss.aiHelper.ui.components.*
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.concurrent.thread

class CodeCreatingToolWindowContent(private val project: Project) {

    val contentPanel = OnePixelSplitter()

    private val mainPanel = JPanel(GridBagLayout())

    private val elementsPanel = JPanel(GridBagLayout())

    private val loadingLabel = JBLabel("Loading...", AnimatedIcon.Default(), SwingConstants.LEFT)

    private val pumlChooser = FileChooserCreateComponent("Choose PlantUML file", "puml", project)

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
            DumbService.getInstance(project).smartInvokeLater {
                Messages.showInfoMessage("There is no types of files to create.", "Create Error")
                loadingLabel.isVisible = false
            }
            return false
        }
        if (pumlChooser.selectedFiles.isEmpty()) {
            DumbService.getInstance(project).smartInvokeLater {
                Messages.showInfoMessage("There is no .puml file.", "Create Error")
                loadingLabel.isVisible = false
            }
            return false
        }
        return true
    }

    private val generateBtn = JButtonWithListener("Generate Files") {
        if (!checkBeforeGenerating()) return@JButtonWithListener

        loadingLabel.isVisible = true
        elementsPanel.removeAll()

        thread {
            try {
                val generateResult = Creators.create(elemsToGenerate, pumlChooser.selectedFiles[0])
                DumbService.getInstance(project).smartInvokeLater {
                    if (generateResult.isNotEmpty()) {
                        for (elems in generateResult) {
                            UiBuilder.buildCreateClassesPanel(elems.value, elementsPanel, project)
                        }
                    }
                }
            } catch (e: Throwable) {
                DumbService.getInstance(project).smartInvokeLater { Messages.showErrorDialog(e.message, "Error") }
            } finally {
                DumbService.getInstance(project).smartInvokeLater {
                    loadingLabel.isVisible = false
                    loadingLabel.text = "Loading..."
                }
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

        loadingLabel.isVisible = false

        val mainElementsWrapper = JPanel(GridBagLayout())
        mainElementsWrapper.add(loadingLabel, DefaultConstraints.topLeftColumn)
        mainElementsWrapper.add(elementsPanel, DefaultConstraints.topLeftColumn)

        val mainElementsPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        mainElementsPanel.add(mainElementsWrapper)

        contentPanel.secondComponent = ScrollWithInsets { mainElementsPanel }
    }

    private fun addWithConstraints(component: JComponent) {
        mainPanel.add(component, DefaultConstraints.topLeftColumn)
    }
}