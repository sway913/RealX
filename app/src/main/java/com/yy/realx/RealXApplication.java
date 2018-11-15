package com.yy.realx;

import android.support.multidex.MultiDexApplication;
import cn.jiguang.share.android.api.JShareInterface;

public class RealXApplication extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        JShareInterface.setDebugMode(true);
        JShareInterface.init(this);
    }
}
