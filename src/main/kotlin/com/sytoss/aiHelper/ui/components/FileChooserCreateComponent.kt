package com.sytoss.aiHelper.ui.components

import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.dsl.builder.panel
import java.awt.FlowLayout
import java.awt.Label
import java.nio.file.Files
import javax.swing.JComponent
import javax.swing.JPanel

class FileChooserCreateComponent(text: String, project: Project) : JPanel(FlowLayout(FlowLayout.LEFT)) {
    val selectedFiles = mutableListOf<VirtualFile>()

    private var descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()

    constructor(
        text: String,
        extension: String,
        project: Project
    ) : this(text, project) {
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

    private fun changeButtonsStateToAppropriate() {
        showButton.isEnabled = selectedFiles.isNotEmpty()
        removeButton.isEnabled = selectedFiles.isNotEmpty()
    }

    init {
        changeButtonsStateToAppropriate()

        add(Label(text))
        add(chooser)
        add(showButton)
        add(removeButton)
    }
}