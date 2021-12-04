package com.ams.hook

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.rlz.ams.hook.Hook

/**
 * Created by RLZ
 * on 2021/12/3
 *
 */
class App : Application() {

    companion object {
        @JvmStatic
        lateinit var mContext: Application;
    }

    override fun onCreate() {
        super.onCreate()
        mContext = this
        Handler(Looper.getMainLooper()).postDelayed({
            Hook.hookAms(this)
        }, 500)
    }
}