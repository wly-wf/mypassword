package com.mypassword.app.ui.navigation

/**
 * 应用导航路由定义
 */
object Routes {
    const val UNLOCK = "unlock"
    const val LIST = "list"
    const val EDIT = "edit/{entryId}"
    const val BACKUP = "backup"
    const val SETTINGS = "settings"

    fun edit(entryId: Long = -1) = "edit/$entryId"
}
