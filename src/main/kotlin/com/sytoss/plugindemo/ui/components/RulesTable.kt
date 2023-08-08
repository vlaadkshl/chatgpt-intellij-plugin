package com.sytoss.plugindemo.ui.components

import com.intellij.openapi.ui.Messages
import com.sytoss.plugindemo.bom.rules.Rule
import com.sytoss.plugindemo.bom.rules.Rules
import com.sytoss.plugindemo.services.JsonService
import java.io.FileNotFoundException
import javax.swing.JTable
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel

class RulesTable : JTable(DefaultTableModel(null, arrayOf("Name", ""))) {
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

        model.addTableModelListener { e ->
            if (e != null) {
                val row = e.firstRow
                val col = e.column

                val eventModel = e.source as TableModel
                val ruleName = eventModel.getValueAt(row, 0) as String
                val checked = eventModel.getValueAt(row, col) as Boolean

                val key = rulesMap.keys.find { key -> key.name == ruleName }!!
                rulesMap.replace(key, checked)
            }
        }
    }

    fun getCheckedRules(): List<Rule> = rulesMap.filter { (_, checked) -> checked }.keys.toList()

    override fun getColumnClass(column: Int): Class<*> = getValueAt(0, column).javaClass
}