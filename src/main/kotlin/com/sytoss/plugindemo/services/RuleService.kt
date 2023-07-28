package com.sytoss.plugindemo.services

import com.sytoss.plugindemo.bom.rules.Rule
import com.sytoss.plugindemo.bom.rules.Rules

object RuleService {
    fun getRules(): List<Rule> = JsonService.fromJsonResourceFile<Rules>("/rules.json").rules

    fun formatRules(rules: List<Rule>): String {
        val rulesStringBuilder = StringBuilder()

        var i = 1
        for (rule in rules) {
            rulesStringBuilder.append("$i. ")
            i++

            rulesStringBuilder.append(
                """
${rule.rule}
Applicable for these class types: ${rule.fileTypes}

            """.trimIndent()
            )
        }

        return rulesStringBuilder.toString()
    }
}