package com.yy.realx

import android.support.multidex.MultiDexApplication
import cn.jiguang.share.android.api.JShareInterface

class RealXApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        JShareInterface.setDebugMode(true)
        JShareInterface.init(this)
    }
}
