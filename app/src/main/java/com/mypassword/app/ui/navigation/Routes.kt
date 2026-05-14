package com.mypassword.app.ui.navigation

object Routes {
    const val UNLOCK = "unlock"
    const val LIST = "list"
    const val DETAIL = "detail/{entryId}"
    const val EDIT = "edit/{entryId}"
    const val BACKUP = "backup"
    const val SETTINGS = "settings"

    fun detail(entryId: Long) = "detail/$entryId"
    fun edit(entryId: Long) = "edit/$entryId"
}
