package com.sytoss.plugindemo.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.selected
import com.sytoss.plugindemo.bom.ModuleChooseType
import com.sytoss.plugindemo.converters.FileConverter
import com.sytoss.plugindemo.services.PackageFinderService
import com.sytoss.plugindemo.services.PyramidService
import com.sytoss.plugindemo.services.chat.CodeCheckingService
import com.sytoss.plugindemo.services.chat.PyramidCheckingService
import com.sytoss.plugindemo.services.chat.PyramidCheckingService.pyramid
import com.sytoss.plugindemo.services.chat.PyramidCheckingService.pyramidFile
import com.sytoss.plugindemo.ui.components.RulesTable
import java.awt.event.ActionEvent
import java.net.SocketTimeoutException
import javax.swing.*
import kotlin.concurrent.thread

class PluginToolWindowContent(private val project: Project) {

    val contentPanel = OnePixelSplitter()

    private val packageFinder = PackageFinderService(project)

    private val table = RulesTable()

    private lateinit var loadingLabel: Cell<JLabel>

    private lateinit var errorLabel: Cell<JLabel>

    private var warningsPanel = JPanel()

    private lateinit var pyramidAnalysisButton: Cell<*>

    private val controlPanel = panel {
        val modules = ModuleManager.getInstance(project).modules.asList()

        lateinit var oneModuleRadio: Cell<JBRadioButton>
        buttonsGroup(title = "Module Mode", indent = false) {
            row {
                radioButton("All modules", value = ModuleChooseType.ALL_MODULES)
            }
            row {
                oneModuleRadio = radioButton("One module", value = ModuleChooseType.ONE_MODULE)
            }.enabled(modules.size > 1)
        }.bind(packageFinder::moduleChooseType) { packageFinder.moduleChooseType = it }

        indent {
            row("Choose Modules") {
                val combo = comboBox(modules)
                combo.component.addActionListener { selectModule(it) }

                combo.component.selectedItem = modules[0]
            }
        }.enabledIf(oneModuleRadio.component.selected)

        row("Code Analysis Feature") {
            cell(table)
            button("Errors Analysis") { analyseErrors() }
        }.bottomGap(BottomGap.MEDIUM)

        row("Pyramid Matching Feature") {
            button("Select Pyramid JSON") {
                DumbService.getInstance(project).smartInvokeLater {
                    pyramidFile = PyramidService.selectPyramid(it.source as JButton, project)
                    if (pyramidFile != null) {
                        pyramidAnalysisButton.enabled(true)
                    }
                }
            }
            pyramidAnalysisButton = button("Pyramid Matching Analysis") {
                analysePyramid()
            }.enabled(false)
        }
    }

    init {
        contentPanel.firstComponent = JBScrollPane(controlPanel)
        contentPanel.secondComponent = JBScrollPane(panel {
            row {
                loadingLabel = cell(
                    JLabel("Loading...", AnimatedIcon.Default(), SwingConstants.LEFT)
                ).visible(false)
            }
            row {
                errorLabel = cell(
                    JLabel("", AllIcons.General.BalloonError, SwingConstants.LEFT)
                ).visible(false)
            }
            row {
                cell(warningsPanel)
            }
        })
    }

    private fun selectModule(event: ActionEvent) {
        val selected: Module = (event.source as JComboBox<*>).selectedItem as Module
        packageFinder.module = selected
    }

    private fun analyseErrors() {
        controlPanel.apply()
        errorLabel.visible(false)
        warningsPanel.removeAll()

        packageFinder.findPackages()

        if (packageFinder.isPyramidEmpty()) {
            Messages.showErrorDialog(
                "This module is empty. I can't find any content, such as BOMs, DTOs, converters etc.",
                "Module Error"
            )
            return
        }

        thread {
            try {
                val fileContent = FileConverter.filesToClassFiles(packageFinder.pyramidElems)

                loadingLabel.visible(true)

                val report = CodeCheckingService.analyse(
                    fileContent,
                    table.getCheckedRules()
                ).result

                DumbService.getInstance(project).smartInvokeLater {
                    val reportPanel = CodeCheckingService.buildReportUi(report, project)
                    warningsPanel.add(reportPanel)
                }
            } catch (e: Exception) {
                if (e is SocketTimeoutException) {
                    errorLabel.component.text = "Oops... We have a timeout error.\nPlease, try again!"
                } else {
                    errorLabel.component.text = "Error: ${e.message}"
                }
                errorLabel.visible(true)
            } finally {
                loadingLabel.visible(false)
            }
        }
    }

    private fun analysePyramid() {
        controlPanel.apply()
        errorLabel.visible(false)
        warningsPanel.removeAll()

        if (pyramidFile == null) {
            Messages.showErrorDialog("First select the \"pyramid.json\" file!", "Error: Analysing Pyramid")
        }

        packageFinder.findPackages()

        if (packageFinder.isPyramidEmpty()) {
            Messages.showErrorDialog(
                "This module is empty. I can't find any content, such as BOMs, DTOs, converters etc.",
                "Module Error"
            )
            return
        }

        thread {
            try {
                pyramid = PyramidService.parseJson(pyramidFile!!)

                val fileContent = FileConverter.filesToClassFiles(packageFinder.pyramidElems)

                loadingLabel.visible(true)

                val report = PyramidCheckingService.analyse(fileContent).result

                DumbService.getInstance(project).smartInvokeLater {
                    loadingLabel.visible(false)

                    val reportPanel = PyramidCheckingService.buildReportUi(report, project)
                    warningsPanel.add(reportPanel)
                }
            } catch (e: Exception) {
                if (e is SocketTimeoutException) {
                    errorLabel.component.text = "Oops... We have a timeout error.\nPlease, try again!"
                } else {
                    errorLabel.component.text = "Error: ${e.message}"
                }
                errorLabel.visible(true)
            }
        }
    }
}