package com.sytoss.plugindemo.services

import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile

class ProblemChecker : AbstractBaseJavaLocalInspectionTool() {

    override fun getDefaultLevel(): HighlightDisplayLevel {
        return HighlightDisplayLevel.ERROR
    }

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file.name.contains("author", ignoreCase = true)) {
            val problemDescriptor = manager.createProblemDescriptor(
                file.firstChild,
                "This is a warning message for the first element of the file.",
                true,
                ProblemHighlightType.ERROR,
                true
            )
            return arrayOf(problemDescriptor)
        }

        return ProblemDescriptor.EMPTY_ARRAY
    }
}