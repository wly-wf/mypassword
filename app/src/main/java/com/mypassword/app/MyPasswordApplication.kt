package com.mypassword.app

import android.app.Application

class MyPasswordApplication : Application() {

    // 数据库实例，全局单例
    lateinit var database: com.mypassword.app.data.db.AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: MyPasswordApplication
            private set
    }
}
