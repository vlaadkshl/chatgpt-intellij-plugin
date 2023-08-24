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
import com.sytoss.aiHelper.bom.codeCreating.ElementType
import com.sytoss.aiHelper.services.CommonFields.applicationManager
import com.sytoss.aiHelper.services.CommonFields.dumbService
import com.sytoss.aiHelper.services.codeCreating.CodeCreatingService
import com.sytoss.aiHelper.services.codeCreating.CodeCreatingService.create
import com.sytoss.aiHelper.services.codeCreating.CodeCreatingService.createConverters
import com.sytoss.aiHelper.ui.components.*
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel
import kotlin.concurrent.thread

class CodeCreatingToolWindowContent {
    val contentPanel = OnePixelSplitter()

    private val mainPanel = JPanel(GridBagLayout())

    private val codePanel = BorderLayoutPanel()

    private var isLoading = false

    private var isDone = true

    private val tree = CreatedClassesTree(codePanel)

    private val pumlChooser = FileChooserCreateComponent("Choose PlantUML file", "puml")

    private val elemsToGenerate = mutableSetOf<ElementType>()

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

    private fun generate(elemToGenerate: ElementType) {
        isDone = elemToGenerate == elemsToGenerate.last()

        create(elemToGenerate, tree) { showCallback ->
            dumbService.smartInvokeLater { isLoading = true }
            tree.setElementLoadingState(elemToGenerate, CreatedClassesTree.LoadingState.LOADING)

            when (elemToGenerate) {
                ElementType.BOM -> {
                    CodeCreatingService.createBom(getPumlContentInEDT(), showCallback)
                }

                ElementType.DTO -> {
                    if (tree.hasBom()) {
                        val editorTexts = tree.getTextsFromEditors(ElementType.BOM)
                        CodeCreatingService.createDtoFromBom(editorTexts, showCallback)
                    } else {
                        CodeCreatingService.createDtoFromPuml(getPumlContentInEDT(), showCallback)
                    }
                }

                ElementType.CONVERTER -> {
                    if (!(tree.hasBom() || tree.hasDto())) {
                        dumbService.smartInvokeLater {
                            Messages.showInfoMessage(
                                "There were no BOMs and DTOs generated.",
                                "${ElementType.CONVERTER} Generating Error"
                            )
                        }
                        return@create
                    }
                    val bomTexts = tree.getTextsFromEditors(ElementType.BOM)
                    val dtoTexts = tree.getTextsFromEditors(ElementType.DTO)

                    createConverters(bomTexts, dtoTexts, showCallback)
                }
            }

            tree.setElementLoadingState(elemToGenerate, CreatedClassesTree.LoadingState.READY)
            dumbService.smartInvokeLater { isLoading = false }
        }

        while (!isNeedContinue) {
            Thread.sleep(100)
        }

        isNeedContinue = false
    }

    private val generateThread = thread(start = false) {
        elemsToGenerate.forEach {
            try {
                generate(it)
            } catch (_: InterruptedException) {
                tree.setElementLoadingState(it, CreatedClassesTree.LoadingState.READY)
                isDone = true
            }
        }
    }

    private val generateBtn = JButtonWithListener("Generate Files") {
        if (!checkBeforeGenerating()) return@JButtonWithListener

        tree.removeEditors()

        tree.clearRoot()
        tree.fillElementNodes(elemsToGenerate)
        tree.fillElementLoadingState(elemsToGenerate)

        generateThread.start()
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
                generateThread.interrupt()

                tree.removeEditors()
                tree.clearRoot()
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
//                cancelAction
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