package com.sytoss.aiHelper.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.ui.Messages
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.components.BorderLayoutPanel
import com.sytoss.aiHelper.bom.codeCreating.CreateResponse
import com.sytoss.aiHelper.bom.codeCreating.ElementType
import com.sytoss.aiHelper.exceptions.generationException.ElementNotGeneratedException
import com.sytoss.aiHelper.services.CommonFields.applicationManager
import com.sytoss.aiHelper.services.codeCreating.CodeCreatingService
import com.sytoss.aiHelper.services.codeCreating.CodeCreatingService.createConverters
import com.sytoss.aiHelper.ui.components.*
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode

class CodeCreatingToolWindowContent {
    val contentPanel = OnePixelSplitter()

    private val mainPanel = JPanel(GridBagLayout())

    private val codePanel = BorderLayoutPanel()

    private var isLoading = false

    private var isDone = true

    private val tree = CreatedClassesTree(codePanel)

    private val pumlChooser = FileChooserCreateComponent("Choose PlantUML file", "puml")

    private val elemsToGenerate = sortedSetOf<ElementType>()

    private var isBomCheckboxSelected = false

    private var isDtoCheckboxSelected = false

    private fun isConverterNeedsEnabling() = isBomCheckboxSelected && isDtoCheckboxSelected

    private val converterCheckBox = JBCheckBoxWithListener(ElementType.CONVERTER) {
        val source = it.source as JBCheckBox
        if (source.isSelected && source.isEnabled) {
            elemsToGenerate.add(ElementType.CONVERTER)
        } else {
            elemsToGenerate.remove(ElementType.CONVERTER)
        }
    }

    private val bomCheckBox = JBCheckBoxWithListener(ElementType.BOM) {
        isBomCheckboxSelected = (it.source as JBCheckBox).isSelected

        converterCheckBox.isEnabled = isConverterNeedsEnabling()

        if (isBomCheckboxSelected) {
            elemsToGenerate.add(ElementType.BOM)
        } else {
            elemsToGenerate.remove(ElementType.BOM)
        }
    }

    private val dtoCheckBox = JBCheckBoxWithListener(ElementType.DTO) {
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

    private fun getPumlContentInEDT(): String? {
        var pumlContent: String? = null
        applicationManager.invokeAndWait { pumlContent = pumlChooser.getFirstFileContent() }
        return pumlContent
    }

    private var isNeedContinue = false

    private fun needsContinue() {
        isNeedContinue = true
    }

    private suspend fun createElem(elemToGenerate: ElementType): CreateResponse? =
        when (elemToGenerate) {
            ElementType.BOM -> CodeCreatingService.createBom(getPumlContentInEDT())

            ElementType.DTO -> {
                if (tree.hasBom()) {
                    val editorTexts = tree.getTextsFromEditors(ElementType.BOM)
                    CodeCreatingService.createDtoFromBom(editorTexts)
                } else {
                    CodeCreatingService.createDtoFromPuml(getPumlContentInEDT())
                }
            }

            ElementType.CONVERTER -> {
                if (!(tree.hasBom() || tree.hasDto())) {
                    throw ElementNotGeneratedException(ElementType.BOM, ElementType.DTO)
                }
                val bomTexts = tree.getTextsFromEditors(ElementType.BOM)
                val dtoTexts = tree.getTextsFromEditors(ElementType.DTO)

                createConverters(bomTexts, dtoTexts)
            }
        }

    private suspend fun generate() {
        for (type in elemsToGenerate) {
            isLoading = true
            isDone = type == elemsToGenerate.last()

            tree.setElementLoadingState(type, CreatedClassesTree.LoadingState.LOADING)
            tree.selectTypeRoot(type)

            createElem(type)?.let { response ->
                isLoading = false

                for (generatedClass in response.result) {
                    tree.insertToTypeRoot(type, DefaultMutableTreeNode(generatedClass))
                }

                tree.fillEditorsByType(type, response)

                tree.expandTypeRoot(type)

                tree.setElementLoadingState(type, CreatedClassesTree.LoadingState.READY)

                while (!isNeedContinue) {
                    delay(100)
                }
                isNeedContinue = false
            }

            tree.toggleRootVisibility()
        }
    }

    private lateinit var coroutineJob: Job

    private val generateBtn = JButtonWithListener("Generate Files") {
        if (!checkBeforeGenerating()) return@JButtonWithListener

        tree.removeEditors()

        tree.clearRoot()
        tree.fillElementNodes(elemsToGenerate)
        tree.fillElementLoadingState(elemsToGenerate)

        coroutineJob = MainScope().launch(Dispatchers.Swing) {
            try {
                generate()
            } catch (e: Throwable) {
                if (e !is CancellationException) {
                    Messages.showErrorDialog(e.message, "Error Happened")
                }
            }
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

        val treeViewerWithActions = BorderLayoutPanel()

        //  ACTION TOOLBAR

        class ContinueAction : AnAction("Co&ntinue Generating", null, AllIcons.Actions.Resume) {
            override fun actionPerformed(e: AnActionEvent) {
                needsContinue()
            }

            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = !(isLoading || isDone)
            }

            override fun getActionUpdateThread() = ActionUpdateThread.EDT
        }

        class CancelAction : AnAction("Cance&l Generating", null, AllIcons.Actions.Cancel) {
            override fun actionPerformed(e: AnActionEvent) {
                coroutineJob.cancel()

                isDone = true
                isLoading = false

                for (elementType in elemsToGenerate) {
                    tree.setElementLoadingState(elementType, CreatedClassesTree.LoadingState.READY)
                }
            }

            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = isLoading
            }

            override fun getActionUpdateThread() = ActionUpdateThread.EDT
        }

        val continueAction = ContinueAction()
        val cancelAction = CancelAction()

        val actionToolbar = ActionManager.getInstance().createActionToolbar(
            ActionPlaces.TOOLBAR,
            DefaultActionGroup(
                continueAction,
                cancelAction
            ),
            false
        ) as ActionToolbarImpl

        actionToolbar.setForceMinimumSize(true)
        actionToolbar.layoutPolicy = ActionToolbar.NOWRAP_LAYOUT_POLICY

        treeViewerWithActions.addToLeft(actionToolbar)

        //  TREE VIEWER

        val treeViewer = OnePixelSplitter()
        treeViewerWithActions.addToCenter(treeViewer)

        //  TREE
        treeViewer.firstComponent = ScrollWithInsets { tree }
        tree.toggleRootVisibility()

        //  NODE VIEWER
        val nodeViewerPane = JBScrollPane(
            object : BorderLayoutPanel() {
                init {
                    addToCenter(codePanel)
                    border = null
                }
            }
        )
        treeViewer.secondComponent = nodeViewerPane
        actionToolbar.targetComponent = codePanel

        //  SET TREE COMPONENT
        contentPanel.secondComponent = treeViewerWithActions
    }
}