package com.sytoss.aiHelper.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.components.BorderLayoutPanel
import com.sytoss.aiHelper.bom.codeCreating.ElementType
import com.sytoss.aiHelper.services.UiBuilder
import com.sytoss.aiHelper.services.codeCreating.Creators
import com.sytoss.aiHelper.ui.components.*
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.nio.file.Files
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.concurrent.thread

class CodeCreatingToolWindowContent(private val project: Project) {

    private val dumbService = DumbService.getInstance(project)

    val contentPanel = OnePixelSplitter()

    private val mainPanel = JPanel(GridBagLayout())

    private val tabbedPane = JBTabbedPane()

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
            Messages.showInfoMessage("There is no types of files to create.", "Create Error")
            return false
        }
        if (pumlChooser.selectedFiles.isEmpty()) {
            Messages.showInfoMessage("There is no .puml file.", "Create Error")
            return false
        }
        return true
    }

    private var isNeedContinue = false
    private fun needsContinue() {
        isNeedContinue = true
    }

    private fun createBom(continuable: Boolean): MutableMap<String, Editor> {
        val editors: MutableMap<String, Editor> = mutableMapOf()

        val loadingLabel = JBLabel("Loading BOMs...", AnimatedIcon.Default(), SwingConstants.LEFT)
        try {
            val innerPanel = JPanel(GridBagLayout())
            innerPanel.add(loadingLabel, DefaultConstraints.topLeftColumn)

            val innerPanelWrapper = JPanel(FlowLayout(FlowLayout.LEFT))
            innerPanelWrapper.add(innerPanel)

            val innerPanelBorder = BorderLayoutPanel()
            val continueButton = JButtonWithListener("Continue") { needsContinue() }
            continueButton.isEnabled = false
            if (continuable) {
                innerPanelBorder.addToTop(continueButton)
            }
            innerPanelBorder.addToCenter(JBScrollPane(innerPanelWrapper))

            tabbedPane.addTab("BOM", innerPanelBorder)

            var pumlContent: String? = null
            ApplicationManager.getApplication().invokeAndWait {
                pumlContent = Files.readString(pumlChooser.selectedFiles[0].toNioPath())
            }

            pumlContent?.let { puml ->
                Creators.createBom(puml)?.let {
                    ApplicationManager.getApplication().invokeAndWait {
                        UiBuilder.buildCreateClassesPanel(it, innerPanel, project, editors)
                        continueButton.isEnabled = true
                    }
                }
            }
        } catch (e: Throwable) {
            dumbService.smartInvokeLater { Messages.showErrorDialog(e.message, "Error") }
        } finally {
            dumbService.smartInvokeLater { loadingLabel.isVisible = false }
        }

        return editors
    }


    private val generateBtn = JButtonWithListener("Generate Files") {
        if (!checkBeforeGenerating()) return@JButtonWithListener

        tabbedPane.removeAll()

        thread {
            val result = mutableMapOf<ElementType, MutableMap<String, Editor>>()

            for (elemToGenerate in elemsToGenerate) {

                when (elemToGenerate) {
                    ElementType.BOM -> {
                        createBom(true)
                            .let { result[ElementType.BOM] = it }

                        while (!isNeedContinue) {
                            Thread.sleep(100)
                        }

                        isNeedContinue = false
                    }

                    ElementType.DTO -> {
                        val isBomExists = result.containsKey(ElementType.BOM)
                    }

                    ElementType.CONVERTER -> {
                        TODO()
                    }
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

        contentPanel.secondComponent = tabbedPane
    }

    private fun addWithConstraints(component: JComponent) {
        mainPanel.add(component, DefaultConstraints.topLeftColumn)
    }
}