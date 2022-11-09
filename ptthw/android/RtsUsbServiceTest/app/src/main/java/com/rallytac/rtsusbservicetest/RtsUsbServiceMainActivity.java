/*
    Copyright (c) 2022 Rally Tactical Systems, Inc.
*/

package com.rallytac.rtsusbservicetest;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;

public class RtsUsbServiceMainActivity extends AppCompatActivity {
    private final static String TAG = RtsUsbServiceMainActivity.class.toString();
    private final static int MAX_LOG_MESSAGES = 512;

    private boolean _inUiMode = false;
    private ListView _lvLog = null;
    private List<String> _logMessages = null;
    private ArrayAdapter<String> _logAdapter = null;

    private final BroadcastReceiver _myBroadcastReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(Context context, Intent i) {
            if (i.getAction().equals(RtsUsbServiceDeviceHandler.ACTION_DEVICE_MESSAGE_RECEIVED)) {
                if(i.hasExtra(RtsUsbServiceDeviceHandler.EXTRA_DEVICE_MESSAGE_DATA)) {
                    String message = i.getStringExtra(RtsUsbServiceDeviceHandler.EXTRA_DEVICE_MESSAGE_DATA);
                    logIt("[" + message + "]");
                }
            }
            else if (i.getAction().equals(RtsUsbServiceDeviceHandler.ACTION_USB_DETACHED)) {
                logIt("device detached");
            }
            else if (i.getAction().equals(RtsUsbServiceDeviceHandler.ACTION_USB_ATTACHED)) {
                logIt("device attached");
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String makeLogLine(String msg) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now) + " : " + msg;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void logIt(String logMsg) {
        Log.i(TAG, logMsg);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                while (_logMessages.size() >= MAX_LOG_MESSAGES) {
                    _logMessages.remove(0);
                }

                _logMessages.add(makeLogLine(logMsg));
                _logAdapter.notifyDataSetChanged();
                _lvLog.setSelection(_logAdapter.getCount() - 1);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        if(savedInstanceState != null) {
            Log.d(TAG, "savedInstanceState=[" + savedInstanceState.toString() + "]");
        }
        else {
            Log.d(TAG, "savedInstanceState=[null]");
        }

        super.onCreate(savedInstanceState);
        new RtsUsbServiceApplication();

        Intent intent = getIntent();
        if(intent != null) {
            Log.d(TAG, "intent=[" + intent.toString() + "]");
            if(intent.getAction().equals(Intent.ACTION_MAIN)) {
                _inUiMode = true;
            }
        }
        else {
            Log.d(TAG, "intent=[null]");
        }

        if(_inUiMode) {
            setContentView(R.layout.activity_main);

            _lvLog = findViewById(R.id.lvLog);
            _logMessages = new LinkedList<>();
            _logAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, _logMessages);
            _lvLog.setAdapter(_logAdapter);

            IntentFilter filter = new IntentFilter();
            filter.addAction(RtsUsbServiceDeviceHandler.ACTION_DEVICE_MESSAGE_RECEIVED);
            filter.addAction(RtsUsbServiceDeviceHandler.ACTION_USB_DETACHED);
            filter.addAction(RtsUsbServiceDeviceHandler.ACTION_USB_ATTACHED);
            registerReceiver(_myBroadcastReceiver, filter);

        }
        else {
            finish();
        }
    }
    private void clearLog() {
        _logMessages.clear();
        _logAdapter.notifyDataSetChanged();
    }

    public void onClickBtnClear(View view) {
        clearLog();
    }
}
