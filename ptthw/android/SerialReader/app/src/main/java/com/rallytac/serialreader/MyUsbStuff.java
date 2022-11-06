/*
    Copyright (c) 2022 Rally Tactical Systems, Inc.
*/

package com.rallytac.serialreader;

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
import java.util.HashMap;
import java.util.Map;

public class MyUsbStuff {
    public static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    
    public static final String ACTION_USB_PERMISSION_GRANTED = "com.rallytac.serialreader.USB_PERMISSION_GRANTED";
    public static final String ACTION_USB_PERMISSION_NOT_GRANTED = "com.rallytac.serialreader.USB_PERMISSION_NOT_GRANTED";
    public static final String ACTION_USB_DISCONNECTED = "com.rallytac.serialreader.USB_DISCONNECTED";
    public static final String ACTION_NO_USB = "com.rallytac.serialreader.NO_USB";
    public static final String ACTION_DEVICE_MESSAGE_RECEIVED = "com.rallytac.serialreader.MESSAGE_RECEIVED";
    public static final String EXTRA_DEVICE_MESSAGE_DATA = "com.rallytac.serialreader.EXTRA_MESSAGE_DATA";

    private UsbManager _usbManager = null;
    private UsbDevice _device = null;
    private UsbDeviceConnection _connection = null;
    private boolean _serialPortConnected = false;
    private Context _ctx = null;
    private boolean _connectionThreadRunning = false;
    private ConnectionThread _connThread = null;
    private UsbInterface _readInterface = null;
    private UsbEndpoint _readEndpoint = null;

    private void startConnectionThread() {
        stopConnectionThread();
        _connectionThreadRunning = true;
        _connThread = new ConnectionThread();
        _connThread.start();
    }

    private void stopConnectionThread() {
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
        @Override
        public void onReceive(Context context, Intent i) {
            if (i.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = i.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted)
                {
                    Intent intent = new Intent(ACTION_USB_PERMISSION_GRANTED);
                    _ctx.sendBroadcast(intent);
                    _serialPortConnected = true;
                    startConnectionThread();
                } 
                else {
                    Intent intent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
                    _ctx.sendBroadcast(intent);
                }
            } 
            else if (i.getAction().equals(ACTION_USB_ATTACHED)) {
                if (!_serialPortConnected) {
                    findSerialPortDevice();
                }
            } 
            else if (i.getAction().equals(ACTION_USB_DETACHED)) {
                Intent intent = new Intent(ACTION_USB_DISCONNECTED);
                _ctx.sendBroadcast(intent);
                _serialPortConnected = false;
                stopConnectionThread();
            }
        }
    };

    public void start(Context ctx) {
        _ctx = ctx;
        _serialPortConnected = false;
        setFilter();
        _usbManager = (UsbManager) _ctx.getSystemService(Context.USB_SERVICE);
        findSerialPortDevice();
    }

    public void stop() {
    }

    private void setFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_ATTACHED);
        _ctx.registerReceiver(_usbReceiver, filter);
    }

    private void findSerialPortDevice() {
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

    private class ConnectionThread extends Thread {
        private final String TAG = ConnectionThread.class.getSimpleName();
        private final int PAUSE_MS = 500;

        private byte[] inboundBuffer = new byte[1024];
        private StringBuilder sb = new StringBuilder();

        @Override
        public void run() {
            while( _connectionThreadRunning ) {
                _connection = _usbManager.openDevice(_device);
                if(_connection == null) {
                    mySleep(PAUSE_MS);
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
                                _readInterface = intf;
                                _readEndpoint = ep;
                                break;
                            }
                        }
                    }
                }

                if(_readInterface == null) {
                    _connection.close();
                    _connection = null;
                    mySleep(PAUSE_MS);
                    continue;
                }

                _connection.controlTransfer(0x21, 0x22, 0x1, 0, null, 0, 0);
                _connection.claimInterface(_readInterface, true);

                sb.setLength(0);

                while (_connectionThreadRunning) {
                    int rc = _connection.bulkTransfer(_readEndpoint, inboundBuffer, inboundBuffer.length, 1000);

                    if (rc > 0) {
                        try {
                            String s = new String(inboundBuffer, 0, rc, "UTF-8");
                            s = s.replace("\r", "");
                            s = s.replace("\n", "");

                            sb.append(s);
                            String capturedSoFar = sb.toString();
                            String[] messages = capturedSoFar.split("\\^");
                            if(messages != null) {
                                for(int x = 0; x < messages.length; x++) {
                                    Log.d(TAG, "msg: '" + messages[x] + "'");

                                    Intent intent = new Intent(ACTION_DEVICE_MESSAGE_RECEIVED);
                                    intent.putExtra(EXTRA_DEVICE_MESSAGE_DATA, messages[x]);
                                    _ctx.sendBroadcast(intent);
                                }

                                //TODO: Don't just whack the builder, rather, just remove the messages we processed
                                sb.setLength(0);
                            }
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        mySleep(100);
                    }
                }

                _connection.releaseInterface(_readInterface);
                _connection.close();
                mySleep(PAUSE_MS);
            }
        }
    }
}
