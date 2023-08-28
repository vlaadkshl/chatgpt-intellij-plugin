package com.sytoss.aiHelper.services

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing

object CommonFields {

    lateinit var project: Project

    lateinit var dumbService: DumbService

    val applicationManager: Application = ApplicationManager.getApplication()

    fun coroutineSwingLaunch(callback: suspend () -> Unit) = MainScope().launch(Dispatchers.Swing) { callback() }
}