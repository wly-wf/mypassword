package com.mypassword.app

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
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
    /** 熄屏后亮屏时为 true，导航到解锁界面后重置 */
    val screenLocked: StateFlow<Boolean> = _screenLocked.asStateFlow()

    fun clearScreenLocked() {
        _screenLocked.value = false
    }

    private var activityCount = 0

    override fun onCreate() {
        super.onCreate()
        instance = this

        try {
            System.loadLibrary("sqlcipher")
        } catch (_: UnsatisfiedLinkError) {
        }

        sessionManager = SessionManager(this)

        // 熄屏即锁定
        registerReceiver(
            ScreenOffReceiver(),
            IntentFilter(Intent.ACTION_SCREEN_OFF)
        )

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                activityCount++
                if (activityCount == 1 && _screenLocked.value) {
                    // 从熄屏恢复，清掉旧的 FLAG_RECEIVER（如果还有）
                }
            }
            override fun onActivityResumed(activity: Activity) {
                // Activity 可见时，如果之前熄屏锁定了，标记需要在 UI 呈现解锁
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                activityCount--
                if (activityCount <= 0) {
                    sessionManager.lock()
                }
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private inner class ScreenOffReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Intent.ACTION_SCREEN_OFF == intent.action) {
                sessionManager.lock()
                _screenLocked.value = true
            }
        }
    }

    companion object {
        lateinit var instance: MyPasswordApplication
            private set
    }
}
