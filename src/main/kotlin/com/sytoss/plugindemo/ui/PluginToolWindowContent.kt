package com.sytoss.plugindemo.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.selected
import com.sytoss.plugindemo.bom.ModuleChooseType
import com.sytoss.plugindemo.bom.pyramid.Pyramid
import com.sytoss.plugindemo.converters.FileConverter
import com.sytoss.plugindemo.services.CodeCheckingService
import com.sytoss.plugindemo.services.PackageFinderService
import com.sytoss.plugindemo.services.PyramidService
import com.sytoss.plugindemo.ui.components.RulesTable
import io.ktor.network.sockets.*
import java.awt.GridLayout
import java.awt.event.ActionEvent
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.concurrent.thread

class PluginToolWindowContent(private val project: Project) {

    val contentPanel = JPanel(GridLayout())

    private val packageFinder = PackageFinderService(project)

    private val table = RulesTable()

    private val errorsText = JLabel("Here will be displayed the warnings")

    private var pyramid: Pyramid? = null

    private val panel: DialogPanel = panel {
        group("Choose Module/Modules") {
            lateinit var oneModuleRadio: Cell<JBRadioButton>
            buttonsGroup(title = "Module Mode", indent = false) {
                row { radioButton("All modules", value = ModuleChooseType.ALL_MODULES) }
                row { oneModuleRadio = radioButton("One module", value = ModuleChooseType.ONE_MODULE) }
            }.bind(packageFinder::moduleChooseType) { packageFinder.moduleChooseType = it }

            indent {
                row("Choose Modules") {
                    val modules = ModuleManager.getInstance(project).modules.asList()

                    val combo = comboBox(modules)
                    combo.component.addActionListener { event -> selectModule(event) }

                    combo.component.selectedItem = modules[0]
                }
            }.enabledIf(oneModuleRadio.component.selected)
        }
        row("Code Analysis Feature") {
            cell(table)
            button("Errors Analysis") { analyseErrors() }
        }
        row("Pyramid Matching Feature") {
            button("Select Pyramid JSON") { event -> pyramid = PyramidService.selectPyramid(event, project) }
            button("Pyramid Matching Analysis") { analysePyramid() }
        }
    }

    init {
        val splitter = OnePixelSplitter()

        splitter.firstComponent = JBScrollPane(panel)
        splitter.secondComponent = JBScrollPane(panel {
            row {
                cell(errorsText)
            }
        })

        contentPanel.add(splitter)
    }

    private fun selectModule(event: ActionEvent) {
        val selected: Module = (event.source as JComboBox<*>).selectedItem as Module
        packageFinder.module = selected
    }

    private fun analyseErrors() {
        panel.apply()

        packageFinder.findPackages()

        if (packageFinder.isPyramidEmpty()) {
            Messages.showErrorDialog("First, select the module", "Module Error")
            return
        }

        thread {
            try {
                val fileContent = FileConverter.filesToClassFiles(packageFinder.pyramidElems)

                errorsText.icon = AnimatedIcon.Default()
                errorsText.text = "Loading..."
                errorsText.updateUI()

                val report = CodeCheckingService.analyseErrors(
                    fileContent,
                    table.getCheckedRules()
                ).result

                errorsText.icon = null
                errorsText.text =
                    if (report.isNotEmpty()) CodeCheckingService.buildReportLabelText(report, project)
                    else "Code doesn't have errors."
            } catch (e: Exception) {
                errorsText.icon = AllIcons.General.BalloonError
                if (e is SocketTimeoutException) {
                    errorsText.text = "Oops... We have a timeout error.\nPlease, try again!"
                } else {
                    errorsText.text = """Error: ${e.message}""".trimMargin()
                }
            }
        }
    }

    private fun analysePyramid() {
        if (pyramid != null) {
            val fileContent = FileConverter.filesToClassFiles(packageFinder.pyramidElems)
            val report = CodeCheckingService.analysePyramid(fileContent)

            Messages.showMessageDialog(null, report, "Pyramid Review Results", Messages.getInformationIcon())
        } else {
            Messages.showErrorDialog("First select the \"pyramid.json\" file!", "Error: Analysing Pyramid")
        }
    }
}