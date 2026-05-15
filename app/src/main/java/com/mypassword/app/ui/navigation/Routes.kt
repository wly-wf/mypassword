package com.mypassword.app.ui.navigation

object Routes {
    const val UNLOCK = "unlock"
    const val HOME = "home"
    const val DETAIL = "detail/{entryId}"
    const val EDIT = "edit/{entryId}"
    const val ADDR_DETAIL = "addr_detail/{addressId}"
    const val ADDR_EDIT = "addr_edit/{addressId}"
    const val BACKUP = "backup"

    fun detail(entryId: Long) = "detail/$entryId"
    fun edit(entryId: Long) = "edit/$entryId"
    fun addrDetail(id: Long) = "addr_detail/$id"
    fun addrEdit(id: Long) = "addr_edit/$id"
}
