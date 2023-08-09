package com.sytoss.aiHelper.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.selected
import com.sytoss.aiHelper.bom.ModuleChooseType
import com.sytoss.aiHelper.services.PackageFinder
import com.sytoss.aiHelper.services.PyramidChooser
import com.sytoss.aiHelper.services.chat.CodeCheckingService
import com.sytoss.aiHelper.services.chat.PyramidCheckingService
import com.sytoss.aiHelper.ui.components.RulesTable
import java.net.SocketTimeoutException
import javax.swing.*
import kotlin.concurrent.thread

class CodeAnalysisToolWindowContent(private val project: Project) {

    val contentPanel = OnePixelSplitter()

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
        }.bind(PackageFinder::moduleChooseType) { PackageFinder.moduleChooseType = it }

        indent {
            row("Choose Modules") {
                val combo = comboBox(modules)
                combo.component.addActionListener {
                    PackageFinder.module = (it.source as JComboBox<*>).selectedItem as Module
                }
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
                    PyramidChooser.selectFile(it.source as JButton, project)
                    pyramidAnalysisButton.enabled(PyramidChooser.isFileSelected())
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

    private fun preparePanel(additionalAction: (() -> Unit)? = null) {
        controlPanel.apply()

        warningsPanel.removeAll()
        errorLabel.visible(false)

        if (additionalAction != null) additionalAction()
    }

    private fun findPackagesAndAsk(): Boolean {
        PackageFinder.findPackages()

        if (PackageFinder.isEmpty()) {
            MessageDialogBuilder.okCancel(
                "Code Checking Error",
                "This module is empty. I can't find any content, such as BOMs, DTOs, converters etc."
            ).ask(project)

            return false
        }

        return MessageDialogBuilder.yesNo(
            title = "Are you sure?",
            message = """
                Here is the elements I'll send for checking:
                
                ${PackageFinder.messageFileNames()}
            """.trimIndent()
        ).ask(project)
    }

    private fun analyseErrors() {
        preparePanel()

        val isContinue = findPackagesAndAsk()
        if (!isContinue) {
            return
        }

        thread {
            try {
                val fileContent = PackageFinder.toClassFiles()

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
                when (e) {
                    is SocketTimeoutException ->
                        errorLabel.component.text = "Oops... We have a timeout error.\nPlease, try again!"

                    else -> errorLabel.component.text = "Error: ${e.message}"
                }
                errorLabel.visible(true)
            } finally {
                loadingLabel.visible(false)
            }
        }
    }

    private fun analysePyramid() {
        preparePanel { PyramidChooser.clearPyramid() }

        val isContinue = findPackagesAndAsk()
        if (!isContinue) {
            return
        }

        thread {
            try {
                PyramidChooser.parsePyramidFromJson()

                val fileContent = PackageFinder.toClassFiles()

                loadingLabel.visible(true)

                val report = PyramidCheckingService.analyse(fileContent).result

                DumbService.getInstance(project).smartInvokeLater {
                    val reportPanel = PyramidCheckingService.buildReportUi(report, project)
                    warningsPanel.add(reportPanel)
                }
            } catch (e: Exception) {
                when (e) {
                    is NoSuchFileException -> DumbService.getInstance(project).smartInvokeLater {
                        MessageDialogBuilder.yesNo(
                            title = "Pyramid Processing Error",
                            message = """
                                Error is occured while processing the pyramid:
                                ${e.message}
                            """.trimIndent()
                        ).ask(project)
                    }

                    is SocketTimeoutException ->
                        errorLabel.component.text = "Oops... We have a timeout error.\nPlease, try again!"

                    else -> errorLabel.component.text = "Error: ${e.message}"
                }
                errorLabel.visible(true)
            } finally {
                loadingLabel.visible(false)
            }
        }
    }
}