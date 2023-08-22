package com.sytoss.aiHelper.ui.components

import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.panel
import com.sytoss.aiHelper.services.CommonFields.project
import java.awt.FlowLayout
import java.nio.file.Files
import javax.swing.JComponent
import javax.swing.JPanel

class FileChooserCreateComponent(text: String) : JPanel(FlowLayout(FlowLayout.LEFT)) {
    val selectedFiles = mutableListOf<VirtualFile>()

    private var descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()

    constructor(
        text: String,
        extension: String,
    ) : this(text) {
        this.descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor(extension)
    }

    private val showButton = JButtonWithListener("Show Selected", AllIcons.Actions.ToggleVisibility) {
        val dialog = object : DialogWrapper(true) {
            init {
                title = "Showing Selected Files"
                init()
            }

            override fun createCenterPanel(): JComponent {
                return ScrollWithInsets {
                    panel {
                        for (file in selectedFiles) {
                            row {
                                label(file.name)
                            }
                            row {
                                val fileContent = Files.readString(file.toNioPath()).replace("\r\n", "\n")

                                val document = EditorFactory.getInstance().createDocument(fileContent)
                                val editorPane = EditorFactory.getInstance().createEditor(document, project)
                                cell(editorPane.component)
                            }
                        }
                    }
                }
            }
        }
        dialog.showAndGet()
    }

    private val removeButton = JButtonWithListener("Clear Selection", AllIcons.Diff.Remove) {
        selectedFiles.clear()
        changeButtonsStateToAppropriate()
    }

    private val chooser = JButtonWithListener("Select Files") {
        FileChooser.chooseFiles(
            FileChooserDescriptorFactory.createMultipleFilesNoJarsDescriptor(),
            project,
            null
        ) {
            selectedFiles.clear()
            selectedFiles.addAll(it)

            changeButtonsStateToAppropriate()
        }
    }

    fun getFirstFileContent(): String = Files.readString(selectedFiles[0].toNioPath())

    private fun changeButtonsStateToAppropriate() {
        showButton.isEnabled = selectedFiles.isNotEmpty()
        removeButton.isEnabled = selectedFiles.isNotEmpty()
    }

    init {
        changeButtonsStateToAppropriate()

        add(JBLabel(text))
        add(chooser)
        add(showButton)
        add(removeButton)
    }
}