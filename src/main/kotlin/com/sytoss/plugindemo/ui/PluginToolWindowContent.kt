package com.sytoss.plugindemo.ui

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.dsl.builder.panel
import com.sytoss.plugindemo.bom.pyramid.Pyramid
import com.sytoss.plugindemo.bom.warnings.ClassGroup
import com.sytoss.plugindemo.converters.FileConverter
import com.sytoss.plugindemo.services.CodeCheckingService
import com.sytoss.plugindemo.services.PackageFinderService
import com.sytoss.plugindemo.services.PyramidService
import com.sytoss.plugindemo.ui.components.RulesTable
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

    init {
        val splitter = OnePixelSplitter()

        splitter.firstComponent = panel {
            row {
                val modules = ModuleManager.getInstance(project).modules.asList()

                val combo = comboBox(modules)
                combo.component.addActionListener { event -> selectModule(event) }

                combo.component.selectedItem = modules[0]
                packageFinder.findPackages()
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

        splitter.secondComponent = panel {
            row {
                cell(errorsText)
            }
        }

        contentPanel.add(splitter)
    }

    private fun selectModule(event: ActionEvent) {
        val selected: Module = (event.source as JComboBox<*>).selectedItem as Module
        packageFinder.module = selected
        packageFinder.findPackages()
    }

    private fun analyseErrors() {
        val fileContent = FileConverter.filesToClassFiles(packageFinder.pyramidElems)

        if (fileContent.isEmpty()) {
            Messages.showErrorDialog("First, select the module", "Module Error")
            return
        }

        errorsText.icon = AnimatedIcon.Default()
        errorsText.text = "Loading..."
        errorsText.updateUI()

        thread {
            val report = CodeCheckingService.analyseErrors(
                fileContent,
                table.getCheckedRules()
            ).result

            errorsText.icon = null
            errorsText.text =
                if (report.isNotEmpty()) CodeCheckingService.buildReportLabelText(report)
                else "Code doesn't have errors."
        }
    }

    private fun getClassPath(warningClass: ClassGroup): String? {
        val (qualifiedName) = warningClass
        val psiClass = JavaPsiFacade.getInstance(project).findClass(
            qualifiedName,
            GlobalSearchScope.projectScope(project)
        )

        return if (psiClass != null) "file:///${psiClass.containingFile?.virtualFile?.path}:10:1" else null
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