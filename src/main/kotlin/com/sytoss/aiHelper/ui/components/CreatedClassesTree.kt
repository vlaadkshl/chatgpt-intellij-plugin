package com.sytoss.aiHelper.ui.components

import com.intellij.openapi.editor.Editor
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.StatusText
import com.intellij.util.ui.components.BorderLayoutPanel
import com.sytoss.aiHelper.bom.codeCreating.CreateResponse.CreateContent
import com.sytoss.aiHelper.bom.codeCreating.ElementType
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class CreatedClassesTree(contentPanel: BorderLayoutPanel) : Tree(DefaultMutableTreeNode("Result")) {
    val editorsByType = mutableMapOf<ElementType, MutableMap<String, Editor>>()

    init {
        addTreeSelectionListener {
            (lastSelectedPathComponent as DefaultMutableTreeNode?)?.let { node ->
                contentPanel.removeAll()

                val nodeInfo = node.userObject
                if (nodeInfo is CreateContent) {
                    val (_, editors) = editorsByType.entries.find { (_, editors) ->
                        editors.containsKey(nodeInfo.fileName)
                    } ?: return@let

                    val editor: Editor = editors[nodeInfo.fileName] ?: return@let

                    contentPanel.addToCenter(editor.component)
                } else {
                    contentPanel.addToCenter(
                        object : StatusText(contentPanel) {
                            override fun isStatusVisible() = true
                        }.component
                    )
                }
            }
        }
    }

    fun toggleRootVisibility() {
        val model = model as DefaultTreeModel
        val root = model.root as DefaultMutableTreeNode
        isRootVisible = (root.childCount != 0)
    }

    fun hasBom() = editorsByType.contains(ElementType.BOM)

    fun hasDto() = editorsByType.contains(ElementType.BOM)

    fun getTextsFromEditors(elementType: ElementType): List<String> {
        val editors = editorsByType[elementType]?.values
        return editors?.map { it.document.text }
            ?: listOf()
    }
}