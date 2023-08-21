package com.sytoss.aiHelper.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project

object CommonFields {

    lateinit var project: Project

    lateinit var dumbService: DumbService

    val applicationManager = ApplicationManager.getApplication()
}