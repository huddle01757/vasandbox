/*
    Copyright (c) 2022 Rally Tactical Systems, Inc.
*/

package com.rallytac.rtsusbservicetest;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RtsUsbServiceDeviceHandler {
    private final String TAG = RtsUsbServiceDeviceHandler.class.getSimpleName();

    public static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    
    public static final String ACTION_USB_PERMISSION_GRANTED = "com.rallytac.rtsusbservice.USB_PERMISSION_GRANTED";
    public static final String ACTION_USB_PERMISSION_NOT_GRANTED = "com.rallytac.rtsusbservice.USB_PERMISSION_NOT_GRANTED";
    public static final String ACTION_USB_DISCONNECTED = "com.rallytac.rtsusbservice.USB_DISCONNECTED";
    public static final String ACTION_NO_USB = "com.rallytac.rtsusbservice.NO_USB";
    public static final String ACTION_DEVICE_MESSAGE_RECEIVED = "com.rallytac.rtsusbservice.MESSAGE_RECEIVED";
    public static final String EXTRA_DEVICE_MESSAGE_DATA = "com.rallytac.rtsusbservice.EXTRA_MESSAGE_DATA";
    public static final String ACTION_LOG_TO_UI_REQUEST = "com.rallytac.rtsusbservice.LOG_TO_UI_REQUEST";
    public static final String EXTRA_LOG_TO_UI_REQUEST_DATA = "com.rallytac.rtsusbservice.LOG_TO_UI_REQUEST_DATA";

    private UsbManager _usbManager = null;
    private UsbDevice _device = null;
    private UsbDeviceConnection _connection = null;
    private boolean _serialPortConnected = false;
    private Context _ctx = null;
    private boolean _connectionThreadRunning = false;
    private RtsUsbConnectionThread _connThread = null;
    private UsbInterface _readInterface = null;
    private UsbEndpoint _readEndpoint = null;
    private String _messageDelimiter = null;
    private HashMap<String, ArrayList<String>>   _messageMap = null;

    private void loadMessageMapping() {
        _messageDelimiter = _ctx.getResources().getString(R.string.message_delimiter);
        _messageMap = null;
        String[] loadedMapping = _ctx.getResources().getStringArray(R.array.message_mapping);

        String usbMessage = null;
        ArrayList<String> mapped = null;

        for(String s: loadedMapping) {
            if(s.endsWith(_messageDelimiter)) {
                if(usbMessage != null && mapped != null) {
                    if(_messageMap == null) {
                        _messageMap = new HashMap<>();
                    }
                    _messageMap.put(usbMessage, mapped);
                }

                usbMessage = s.substring(0, s.length()-1);
                mapped = null;
                continue;
            }

            if(mapped == null) {
                mapped = new ArrayList<>();
            }

            mapped.add(s);
        }

        if(usbMessage != null && mapped != null) {
            if(_messageMap == null) {
                _messageMap = new HashMap<>();
            }

            _messageMap.put(usbMessage, mapped);
        }

        if(_messageMap != null) {
            Log.d(TAG, "message mapping:");
            Iterator itr = _messageMap.entrySet().iterator();
            while(itr.hasNext()) {
                Map.Entry element = (Map.Entry)itr.next();
                String key = (String)element.getKey();
                ArrayList<String> intents = (ArrayList<String>)element.getValue();
                for(String intent: intents) {
                    Log.d(TAG, "   [" + key + "]->[" + intent + "]");
                }
            }
        }
    }

    private ArrayList<String> getMappedIntentsForUsbMessage(String usbMsg) {
        if(_messageMap == null) {
            return null;
        }

        ArrayList<String> rc = _messageMap.get(usbMsg);

        return rc;
    }

    private void startConnectionThread() {
        Log.d(TAG, "startConnectionThread");
        stopConnectionThread();
        _connectionThreadRunning = true;
        _connThread = new RtsUsbConnectionThread();
        _connThread.start();
    }

    private void stopConnectionThread() {
        Log.d(TAG, "stopConnectionThread");
        _connectionThreadRunning = false;
        if(_connThread != null) {
            try {
                _connThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            _connThread = null;
        }
    }

    private final BroadcastReceiver _usbReceiver = new BroadcastReceiver() {
        private final String TAG = BroadcastReceiver.class.getSimpleName();

        @Override
        public void onReceive(Context context, Intent i) {
            Log.d(TAG, "onReceive");

            if (i.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = i.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted)
                {
                    Log.d(TAG, "ACTION_USB_PERMISSION_GRANTED");
                    Intent intent = new Intent(ACTION_USB_PERMISSION_GRANTED);
                    _ctx.sendBroadcast(intent);
                    _serialPortConnected = true;
                    startConnectionThread();
                } 
                else {
                    Log.d(TAG, "ACTION_USB_PERMISSION_NOT_GRANTED");
                    Intent intent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
                    _ctx.sendBroadcast(intent);
                }
            } 
            else if (i.getAction().equals(ACTION_USB_ATTACHED)) {
                Log.d(TAG, "ACTION_USB_ATTACHED");
                if (!_serialPortConnected) {
                    findSerialPortDevice();
                }
            } 
            else if (i.getAction().equals(ACTION_USB_DETACHED)) {
                Log.d(TAG, "ACTION_USB_DETACHED");
                Intent intent = new Intent(ACTION_USB_DISCONNECTED);
                _ctx.sendBroadcast(intent);
                _serialPortConnected = false;
                stopConnectionThread();
            }
        }
    };

    public void start(Context ctx) {
        Log.d(TAG, "start");
        _ctx = ctx;
        _serialPortConnected = false;
        loadMessageMapping();
        setFilter();
        _usbManager = (UsbManager) _ctx.getSystemService(Context.USB_SERVICE);
        findSerialPortDevice();
    }

    public void stop() {
        Log.d(TAG, "stop");
        stopConnectionThread();
    }

    private void setFilter() {
        Log.d(TAG, "setFilter");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_ATTACHED);
        _ctx.registerReceiver(_usbReceiver, filter);
    }

    private void findSerialPortDevice() {
        Log.d(TAG, "findSerialPortDevice");
        HashMap<String, UsbDevice> usbDevices = _usbManager.getDeviceList();
        
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                _device = entry.getValue();
                int deviceVID = _device.getVendorId();
                int devicePID = _device.getProductId();

                if (deviceVID != 0x1d6b && (devicePID != 0x0001 || devicePID != 0x0002 || devicePID != 0x0003)) {
                    requestUserPermission();
                    keep = false;
                } 
                else {
                    _connection = null;
                    _device = null;
                }

                if (!keep) {
                    break;
                }
            }
            if (!keep) {
                Intent intent = new Intent(ACTION_NO_USB);
                _ctx.sendBroadcast(intent);
            }
        } else {
            Intent intent = new Intent(ACTION_NO_USB);
            _ctx.sendBroadcast(intent);
        }
    }

    private void requestUserPermission() {
        Log.d(TAG, "requestUserPermission");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(_ctx, 0, new Intent(ACTION_USB_PERMISSION), 0);
        _usbManager.requestPermission(_device, pendingIntent);
    }
    
    private void mySleep(int ms) {
        try {
            Thread.sleep(ms);
        } 
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void logToUi(String msg) {
        Log.d(TAG, msg);

        Intent logIntent = new Intent(ACTION_LOG_TO_UI_REQUEST);
        logIntent.putExtra(EXTRA_LOG_TO_UI_REQUEST_DATA, msg);
        _ctx.sendBroadcast(logIntent);
    }

    private class RtsUsbConnectionThread extends Thread {
        private final String TAG = RtsUsbConnectionThread.class.getSimpleName();
        private final int PAUSE_MS = 500;

        private byte[] inboundBuffer = new byte[1024];
        private StringBuilder sb = new StringBuilder();

        @Override
        public void run() {
            while( _connectionThreadRunning ) {
                _connection = _usbManager.openDevice(_device);
                if(_connection == null) {
                    Log.d(TAG, "openDevice failed");
                    mySleep(PAUSE_MS);
                    continue;
                }

                for (int ix = 0; ix < _device.getInterfaceCount(); ix++) {
                    if (_readInterface != null) {
                        break;
                    }

                    UsbInterface intf = _device.getInterface(ix);
                    int ifc = intf.getInterfaceClass();

                    if (ifc == UsbConstants.USB_CLASS_CDC_DATA) {
                        for (int ex = 0; ex < intf.getEndpointCount(); ex++) {
                            UsbEndpoint ep = intf.getEndpoint(ex);
                            if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                                Log.d(TAG, "found cdc data usb class");
                                _readInterface = intf;
                                _readEndpoint = ep;
                                break;
                            }
                        }
                    }
                }

                if(_readInterface == null) {
                    Log.d(TAG, "no read interface found");
                    _connection.close();
                    _connection = null;
                    mySleep(PAUSE_MS);
                    continue;
                }

                _connection.controlTransfer(0x21, 0x22, 0x1, 0, null, 0, 0);
                _connection.claimInterface(_readInterface, true);

                sb.setLength(0);

                Log.d(TAG, "entering read loop");
                while (_connectionThreadRunning) {
                    int rc = _connection.bulkTransfer(_readEndpoint, inboundBuffer, inboundBuffer.length, 1000);

                    if (rc > 0) {
                        Log.d(TAG, "bulkTransfer returned " + rc + " bytes");

                        try {
                            String s = new String(inboundBuffer, 0, rc, "UTF-8");
                            s = s.replace("\r", "");
                            s = s.replace("\n", "");

                            sb.append(s);
                            String capturedSoFar = sb.toString();
                            String[] messages = capturedSoFar.split("\\" + _messageDelimiter);
                            if(messages != null) {
                                for(int x = 0; x < messages.length; x++) {
                                    Log.d(TAG, "msg: [" + messages[x] + "]");

                                    Intent intent = new Intent(ACTION_DEVICE_MESSAGE_RECEIVED);
                                    intent.putExtra(EXTRA_DEVICE_MESSAGE_DATA, messages[x]);
                                    _ctx.sendBroadcast(intent);

                                    ArrayList<String> intentTextList = getMappedIntentsForUsbMessage(messages[x]);
                                    if(intentTextList != null && !intentTextList.isEmpty()) {
                                        for(String intentText: intentTextList) {
                                            logToUi("[" + messages[x] + "] sends [" + intentText + "]");
                                            _ctx.sendBroadcast(new Intent(intentText));
                                        }
                                    }
                                    else {
                                        logToUi("no or empty mapping for [" + messages[x] + "]");
                                    }
                                }

                                //TODO: Don't just whack the builder, rather, just remove the messages we processed
                                sb.setLength(0);
                            }
                        } catch (UnsupportedEncodingException e) {
                            Log.d(TAG, "read exception " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    else {
                        //Log.d(TAG, "bulkTransfer returned " + rc + " bytes");
                        mySleep(100);
                    }
                }
                Log.d(TAG, "left read loop");

                _connection.releaseInterface(_readInterface);
                _connection.close();
                mySleep(PAUSE_MS);
            }
        }
    }
}
