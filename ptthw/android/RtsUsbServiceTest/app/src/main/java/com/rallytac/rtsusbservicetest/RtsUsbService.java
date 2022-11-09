/*
    Copyright (c) 2022 Rally Tactical Systems, Inc.
*/

package com.rallytac.rtsusbservicetest;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class RtsUsbService extends Service {
    private final static String TAG = RtsUsbService.class.toString();

    private RtsUsbServiceDeviceHandler _deviceHandler = null;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();

        if(_deviceHandler != null) {
            _deviceHandler.stop();
            _deviceHandler = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand, flags=" + flags + ", startId=" + startId);
        super.onStartCommand(intent, flags, startId);

        if(_deviceHandler == null) {
            _deviceHandler = new RtsUsbServiceDeviceHandler();
            _deviceHandler.start(this);
        }

        return START_STICKY;
    }
}
