package com.sytoss.aiHelper.ui

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.components.BorderLayoutPanel
import com.sytoss.aiHelper.bom.codeCreating.CreateResponse
import com.sytoss.aiHelper.services.UiBuilder
import com.sytoss.aiHelper.services.codeCreating.BomFromPumlCreator
import com.sytoss.aiHelper.ui.components.*
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.concurrent.thread

class CodeCreatingToolWindowContent(private val project: Project) {

    val contentPanel = OnePixelSplitter()

    private val mainPanel = JPanel(GridBagLayout())

    private val elementsPanel = JPanel(GridBagLayout())

    private val loadingLabel = JLabel("Loading...", AnimatedIcon.Default(), SwingConstants.LEFT)

    private val pumlChooser = FileChooserCreateComponent("Choose PlantUML file", "puml", project)

    private val generateBtn = JButtonWithListener("Generate Files") {
        loadingLabel.isVisible = true
        thread {
            var bomClasses: CreateResponse? = null
            try {
                bomClasses = BomFromPumlCreator.createBom()
            } finally {
                DumbService.getInstance(project).smartInvokeLater {
                    loadingLabel.isVisible = false
                    if (bomClasses != null) {
                        UiBuilder.buildCreateClassesPanel(bomClasses, elementsPanel, project)
                    }
                }
            }
        }
    }

    private var isBomCheckBoxSelected = false
    private var isDtoCheckBoxSelected = false

    private val converterCheckBox = JBCheckBoxWithListener("Converter") {}

    private val bomCheckBox = JBCheckBoxWithListener("BOM") {
        val source = it.source as JBCheckBox
        isBomCheckBoxSelected = source.isSelected
        converterCheckBox.isSelected = isBomCheckBoxSelected && isDtoCheckBoxSelected
        converterCheckBox.isEnabled = isBomCheckBoxSelected && isDtoCheckBoxSelected
    }

    private val dtoCheckBox = JBCheckBoxWithListener("DTO") {
        val source = it.source as JBCheckBox
        isDtoCheckBoxSelected = source.isSelected
        converterCheckBox.isSelected = isBomCheckBoxSelected && isDtoCheckBoxSelected
        converterCheckBox.isEnabled = isBomCheckBoxSelected && isDtoCheckBoxSelected
    }

    init {
        converterCheckBox.isEnabled = false

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

        elementsPanel.add(loadingLabel)
        loadingLabel.isVisible = false
        contentPanel.secondComponent = ScrollWithInsets { elementsPanel }
    }

    private fun addWithConstraints(component: JComponent) {
        mainPanel.add(component, DefaultConstraints.topLeftColumn)
    }
}