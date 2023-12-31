package com.sytoss.aiHelper.ui.components

import com.intellij.openapi.ui.Messages
import com.intellij.ui.scale.JBUIScale
import com.sytoss.aiHelper.bom.chat.checkingCode.rules.Rule
import com.sytoss.aiHelper.bom.chat.checkingCode.rules.Rules
import com.sytoss.aiHelper.services.JsonService
import java.io.FileNotFoundException
import javax.swing.JTable
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel

class RulesTable : JTable(object : DefaultTableModel(null, arrayOf("Name", "")) {
    override fun isCellEditable(row: Int, column: Int) = (column == 1)
}) {
    private val rulesMap: MutableMap<Rule, Boolean> = mutableMapOf()

    init {
        try {
            val rules = JsonService.fromJsonResourceFile<Rules>("/rules.json").rules
            for (rule in rules) {
                rulesMap[rule] = true
            }
        } catch (e: FileNotFoundException) {
            Messages.showErrorDialog("Can't read \"rules.json\" file.", "Error")
        }
    }

    init {
        val model = this.model as DefaultTableModel

        for ((rule, checked) in rulesMap) {
            model.addRow(arrayOf(rule.name, checked))
        }

        getColumnModel().getColumn(0).preferredWidth = JBUIScale.scale(300)
        getColumnModel().getColumn(0).resizable = true

        model.addTableModelListener {
            if (it != null) {
                val row = it.firstRow
                val col = it.column

                val eventModel = it.source as TableModel
                val ruleName = eventModel.getValueAt(row, 0) as String
                val checked = eventModel.getValueAt(row, col) as Boolean

                val key = rulesMap.keys.find { key -> key.name == ruleName }!!
                rulesMap.replace(key, checked)
            }
        }
    }

    fun getCheckedRules(): List<Rule> = rulesMap.filter { (_, checked) -> checked }.keys.toList()

    fun isNothingSelected(): Boolean = rulesMap.values.stream().allMatch { it == false }

    override fun getColumnClass(column: Int): Class<*> = getValueAt(0, column).javaClass
}