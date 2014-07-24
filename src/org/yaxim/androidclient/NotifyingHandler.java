package org.yaxim.androidclient;

import org.jboss.aerogear.android.unifiedpush.MessageHandler;
import org.yaxim.androidclient.data.YaximConfiguration;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class NotifyingHandler implements MessageHandler {

	public NotifyingHandler() {
		super();
	}
	
	@Override
	public void onMessage(Context context, Bundle message) {
		// Check if the app is already running
	    ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
	        if (service.process.equals(context.getPackageName().toString()) && service.started) {
	            return;
	        }
	    }
		
		YaximConfiguration mConfig = YaximApplication.getConfig(context);

		NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		WakeLock mWakeLock = ((PowerManager) context
				.getSystemService(Context.POWER_SERVICE)).newWakeLock(
				PowerManager.PARTIAL_WAKE_LOCK, "Push");
		Notification mNotification = new Notification();
		Intent mNotificationIntent = new Intent(context, MainWindow.class);

		String msg = message.getString("alert");
		Log.i("PUSH Notification", msg);
		mWakeLock.acquire();

		try {
			try {
				RingtoneManager.getRingtone(context, mConfig.notifySound)
						.play();
			} catch (NullPointerException e) {
				// ignore NPE when ringtone was not found
			}
			// mNotificationIntent.setData();
			// mNotificationIntent.putExtra(ChatWindow.INTENT_EXTRA_USERNAME,
			// fromUserId);
			mNotificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

			mNotification.sound = mConfig.notifySound;
			mNotification.audioStreamType = AudioManager.STREAM_NOTIFICATION;
			mNotification.defaults |= Notification.DEFAULT_VIBRATE;
			mNotification.flags |= Notification.FLAG_AUTO_CANCEL;
			mNotification.icon = R.drawable.icon;
			mNotification.sound = mConfig.notifySound;
			mNotification.tickerText = msg;
			mNotification.ledARGB = Color.MAGENTA;
			mNotification.ledOnMS = 300;
			mNotification.ledOffMS = 1000;
			mNotification.flags |= Notification.FLAG_SHOW_LIGHTS;

			PendingIntent appIntent = PendingIntent.getActivity(context, 0,	mNotificationIntent, 0);
			mNotification.setLatestEventInfo(context, context.getText(R.string.rooms_new_message), msg,	appIntent);

			notificationManager.notify(0, mNotification);
		} 
		finally {
			mWakeLock.release();
		}
	}

    @Override
    public void onDeleteMessage(Context context, Bundle arg0) {
        // handle GoogleCloudMessaging.MESSAGE_TYPE_DELETED
    }

    @Override
    public void onError() {
        // handle GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR
    }
}