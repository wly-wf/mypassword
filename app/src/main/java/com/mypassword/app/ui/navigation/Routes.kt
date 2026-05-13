package com.mypassword.app.ui.navigation

object Routes {
    const val UNLOCK = "unlock"
    const val LIST = "list"
    const val EDIT = "edit/{entryId}"
    const val BACKUP = "backup"
    const val SETTINGS = "settings"

    fun edit(entryId: Long = -1) = "edit/$entryId"
}
