package com.sytoss.aiHelper.bom.codeCreating

enum class ElementType(val text: String) {
    BOM("BOM"),
    DTO("DTO"),
    CONVERTER("Converter");

    override fun toString() = text
}