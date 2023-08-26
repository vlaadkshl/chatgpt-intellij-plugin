package com.sytoss.aiHelper.exceptions.generationException

import com.sytoss.aiHelper.bom.codeCreating.ElementType

class ElementNotGeneratedException(vararg elements: ElementType) :
    IllegalStateException("${elements.joinToString()}}} ${if (elements.size == 1) "was" else "were"} not generated yet!")