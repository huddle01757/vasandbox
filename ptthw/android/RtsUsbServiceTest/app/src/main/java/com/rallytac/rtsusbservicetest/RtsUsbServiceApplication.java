/*
    Copyright (c) 2022 Rally Tactical Systems, Inc.
*/

package com.rallytac.rtsusbservicetest;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

public class RtsUsbServiceApplication extends Application {
    private final static String TAG = RtsUsbServiceApplication.class.toString();

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        startService(new Intent(this, RtsUsbService.class));
    }

    @Override
    public void onTerminate() {
        Log.d(TAG, "onTerminate");
        super.onTerminate();
    }
}
