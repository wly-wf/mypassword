package com.mypassword.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.mypassword.app.data.crypto.SessionManager
import com.mypassword.app.data.db.AppDatabase

class MyPasswordApplication : Application() {

    lateinit var database: AppDatabase
        internal set
    lateinit var sessionManager: SessionManager
        private set

    private var activityCount = 0

    override fun onCreate() {
        super.onCreate()
        instance = this
        sessionManager = SessionManager(this)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                activityCount++
            }
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                activityCount--
                if (activityCount <= 0) {
                    // 所有 Activity 都不可见，锁定
                    sessionManager.lock()
                }
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    companion object {
        lateinit var instance: MyPasswordApplication
            private set
    }
}
