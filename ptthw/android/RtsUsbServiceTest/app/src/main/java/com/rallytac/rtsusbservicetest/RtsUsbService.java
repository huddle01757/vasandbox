/*
    Copyright (c) 2022 Rally Tactical Systems, Inc.
*/

package com.rallytac.rtsusbservicetest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class RtsUsbService extends Service {
    private final static String TAG = RtsUsbService.class.toString();

    private static final int NOTIFICATION_ID = 1;
    private NotificationManager _notificationManager = null;
    private NotificationChannel _notificationChannel = null;

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

        initializeOsNotifications();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();

        if(_deviceHandler != null) {
            _deviceHandler.stop();
            _deviceHandler = null;
        }

        shutdownOsNotifications();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand, flags=" + flags + ", startId=" + startId);
        super.onStartCommand(intent, flags, startId);

        if(_deviceHandler == null) {
            _deviceHandler = new RtsUsbServiceDeviceHandler();
            _deviceHandler.start(this);
        }

        if(startId == 1) {
            showOsNotification(getString(R.string.notification_title), getString(R.string.notification_message), R.drawable.ic_app_icon);
        }

        return START_STICKY;
    }

    private void initializeOsNotifications()
    {
        try
        {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                if(_notificationManager == null)
                {
                    _notificationManager = getSystemService(NotificationManager.class);
                }

                if(_notificationChannel == null)
                {
                    _notificationChannel = new NotificationChannel(
                            BuildConfig.APPLICATION_ID + getString(R.string.android_notification_channel_id),
                            BuildConfig.APPLICATION_ID + getString(R.string.android_notification_channel_name),
                            NotificationManager.IMPORTANCE_HIGH);

                    _notificationChannel.setDescription(getString(R.string.android_notification_channel_description));
                    _notificationChannel.enableLights(true);
                    _notificationChannel.setLightColor(Color.RED);
                    _notificationChannel.setShowBadge(true);

                    _notificationManager.createNotificationChannel(_notificationChannel);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void shutdownOsNotifications()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            if(_notificationManager != null)
            {
                _notificationManager.cancelAll();
                _notificationManager.deleteNotificationChannel(BuildConfig.APPLICATION_ID + getString(R.string.android_notification_channel_id));
            }
        }
    }

    private void showOsNotification(String title, String msg, int iconId)
    {
        try
        {
            Intent notificationIntent = new Intent(this, RtsUsbServiceMainActivity.class);
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(this, 0, notificationIntent,
                            PendingIntent.FLAG_IMMUTABLE);

            Notification notification = new NotificationCompat.Builder(this, BuildConfig.APPLICATION_ID + getString(R.string.android_notification_channel_id))
                    .setContentTitle(title)
                    .setContentText(msg)
                    .setSmallIcon(iconId)
                    .setContentIntent(pendingIntent)
                    .build();

            notification.flags |= (Notification.FLAG_ONGOING_EVENT | Notification.FLAG_AUTO_CANCEL);

            if(_notificationManager != null)
            {
                _notificationManager.notify(NOTIFICATION_ID, notification);
            }

            startForeground(NOTIFICATION_ID, notification);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
