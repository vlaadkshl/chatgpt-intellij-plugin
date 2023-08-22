package com.sytoss.aiHelper.bom.codeCreating

enum class ElementType(private val value: String) {
    BOM("BOM"),
    DTO("DTO"),
    CONVERTER("Converter");

    override fun toString() = value
}