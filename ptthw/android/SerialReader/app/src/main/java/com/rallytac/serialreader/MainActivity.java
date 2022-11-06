/*
    Copyright (c) 2022 Rally Tactical Systems, Inc.
*/

package com.rallytac.serialreader;

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
import android.widget.TextView;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private MyUsbStuff _myUsbStuffObject = null;
    private ListView _lvLog = null;
    private List<String> _logMessages = null;
    private ArrayAdapter<String> _logAdapter = null;

    private final BroadcastReceiver _myBroadcastReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(Context context, Intent i) {
            if (i.getAction().equals(MyUsbStuff.ACTION_DEVICE_MESSAGE_RECEIVED)) {
                if(i.hasExtra(MyUsbStuff.EXTRA_DEVICE_MESSAGE_DATA)) {
                    String message = i.getStringExtra(MyUsbStuff.EXTRA_DEVICE_MESSAGE_DATA);
                    logIt("[" + message + "]");
                }
            }
            else if (i.getAction().equals(MyUsbStuff.ACTION_USB_DETACHED)) {
                logIt("device detached");
            }
            else if (i.getAction().equals(MyUsbStuff.ACTION_USB_ATTACHED)) {
                logIt("device attached");
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void logIt(String logMsg) {
        Log.i(TAG, logMsg);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                LocalDateTime now = LocalDateTime.now();

                _logMessages.add(dtf.format(now) + " : " + logMsg);
                _logAdapter.notifyDataSetChanged();
                _lvLog.setSelection(_logAdapter.getCount() - 1);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _lvLog = findViewById(R.id.lvLog);
        _logMessages = new LinkedList<>();
        _logAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, _logMessages);
        _lvLog.setAdapter(_logAdapter);

        IntentFilter filter = new IntentFilter();
        filter.addAction(MyUsbStuff.ACTION_DEVICE_MESSAGE_RECEIVED);
        filter.addAction(MyUsbStuff.ACTION_USB_DETACHED);
        filter.addAction(MyUsbStuff.ACTION_USB_ATTACHED);
        registerReceiver(_myBroadcastReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(_myUsbStuffObject == null) {
            _myUsbStuffObject = new MyUsbStuff();
            _myUsbStuffObject.start(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //_myUsbStuffObject.stop();
        //_myUsbStuffObject = null;
    }

    private void clearLog() {
        _logMessages.clear();
        _logAdapter.notifyDataSetChanged();
    }

    public void onClickBtnClear(View view) {
        clearLog();
    }
}
