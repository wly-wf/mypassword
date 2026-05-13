package com.mypassword.app

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.mypassword.app.data.crypto.SessionManager
import com.mypassword.app.data.db.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MyPasswordApplication : Application() {

    lateinit var database: AppDatabase
    lateinit var sessionManager: SessionManager
        private set

    fun isDatabaseInitialized(): Boolean = ::database.isInitialized

    private val _screenLocked = MutableStateFlow(false)
    val screenLocked: StateFlow<Boolean> = _screenLocked.asStateFlow()

    fun clearScreenLocked() {
        _screenLocked.value = false
    }

    private var activityCount = 0
    private lateinit var screenOffReceiver: BroadcastReceiver

    override fun onCreate() {
        super.onCreate()

        try {
            System.loadLibrary("sqlcipher")
        } catch (_: UnsatisfiedLinkError) {
        }

        sessionManager = SessionManager(this)

        screenOffReceiver = ScreenOffReceiver()
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(a: android.app.Activity, b: android.os.Bundle?) {}
            override fun onActivityStarted(a: android.app.Activity) {
                activityCount++
            }
            override fun onActivityResumed(a: android.app.Activity) {}
            override fun onActivityPaused(a: android.app.Activity) {}
            override fun onActivityStopped(a: android.app.Activity) {
                activityCount--
                if (activityCount <= 0) {
                    sessionManager.lock()
                }
            }
            override fun onActivitySaveInstanceState(a: android.app.Activity, b: android.os.Bundle) {}
            override fun onActivityDestroyed(a: android.app.Activity) {}
        })
    }

    override fun onTerminate() {
        super.onTerminate()
        unregisterReceiver(screenOffReceiver)
    }

    private inner class ScreenOffReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Intent.ACTION_SCREEN_OFF == intent.action) {
                sessionManager.lock()
                _screenLocked.value = true
            }
        }
    }
}
