package com.sytoss.aiHelper.ui.components

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.StatusText
import com.intellij.util.ui.components.BorderLayoutPanel
import com.sytoss.aiHelper.bom.codeCreating.CreateResponse
import com.sytoss.aiHelper.bom.codeCreating.CreateResponse.CreateContent
import com.sytoss.aiHelper.bom.codeCreating.ElementType
import com.sytoss.aiHelper.services.CommonFields
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode

class CreatedClassesTree(contentPanel: BorderLayoutPanel) : Tree(DefaultMutableTreeNode("Result")) {
    private val editorsByType = mutableMapOf<ElementType, MutableMap<String, Editor>>()

    private val elementNodes = mutableMapOf<ElementType, DefaultMutableTreeNode>()

    init {
        addTreeSelectionListener {
            (lastSelectedPathComponent as DefaultMutableTreeNode?)?.let { node ->
                contentPanel.removeAll()

                val nodeInfo = node.userObject
                if (nodeInfo is CreateContent) {
                    val editors = findEditorsWithSomeClass(nodeInfo.fileName) ?: return@let

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

    fun removeEditors() = editorsByType.clear()

    fun fillEditorsByType(type: ElementType, data: CreateResponse) {
        val editors = data.result
            .associateBy(
                { it.fileName },
                {
                    EditorFactory.getInstance().createEditor(
                        EditorFactory.getInstance().createDocument(it.body),
                        CommonFields.project,
                        JavaClassFileType.INSTANCE,
                        false
                    )
                })
            .toMutableMap()
        editorsByType.putIfAbsent(type, editors)
    }

    private fun getRoot() = (model as DefaultTreeModel).root as DefaultMutableTreeNode

    private fun findEditorsWithSomeClass(someClass: String) = editorsByType.values.find { it.containsKey(someClass) }

    private fun insertToRoot(child: MutableTreeNode) {
        val root = getRoot()
        (model as DefaultTreeModel).insertNodeInto(child, root, root.childCount)
    }

    private fun insertToNode(root: MutableTreeNode, child: MutableTreeNode) {
        (model as DefaultTreeModel).insertNodeInto(child, root, root.childCount)
    }

    fun insertToTypeRoot(type: ElementType, child: DefaultMutableTreeNode) {
        elementNodes[type]?.let { rootNode -> insertToNode(rootNode, child) }
    }

    fun fillElementNodes(elemsToGenerate: Collection<ElementType>) {
        for (elementType in elemsToGenerate) {
            val child = DefaultMutableTreeNode(elementType)

            insertToRoot(child)
            elementNodes[elementType] = child
        }
    }

    fun clearRoot() = getRoot().removeAllChildren()

    fun toggleRootVisibility() {
        val root = getRoot()
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