package org.yaxim.androidclient;

import org.jboss.aerogear.android.unifiedpush.MessageHandler;
import org.yaxim.androidclient.data.YaximConfiguration;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class NotifyingHandler extends BroadcastReceiver implements MessageHandler {
	
	private static final String PUSH_BROADCAST = "de.f24.rooms.messages.push.broadcast";

	public NotifyingHandler() {
		super();
	}
	
	@Override
	public void onMessage(Context context, Bundle message) {
		
		Log.d("PUSH", "Push received by service...");
		
		Intent broadcastIntent = new Intent(PUSH_BROADCAST);
		broadcastIntent.putExtras(message);
		context.sendOrderedBroadcast(broadcastIntent, null);
	}

    @Override
    public void onDeleteMessage(Context context, Bundle arg0) {
        // handle GoogleCloudMessaging.MESSAGE_TYPE_DELETED
    }

    @Override
    public void onError() {
        // handle GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR
    }
    
	@Override
	public void onReceive(Context context, Intent intent) {
		
		Log.d("PUSH", "Push received by service again, showing notification!");
		
		YaximConfiguration mConfig = YaximApplication.getConfig(context);
		Bundle message = intent.getExtras();
		
		Intent mainIntent = new Intent(context, MainWindow.class);
        PendingIntent notify = PendingIntent.getActivity(context, 99, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
			.setSmallIcon(R.drawable.icon)
			.setContentTitle(context.getString(R.string.app_name))
			.setSound(mConfig.notifySound)
			.setAutoCancel(true)
			.setOngoing(false)
			.setContentText(message.getString("alert"));
		
		builder.setContentIntent(notify);
        notificationManager.notify(99, builder.getNotification());
	}
    
    public static BroadcastReceiver registerDynamicBroadcastReceiver(Activity activity) {
    	
    	BroadcastReceiver receiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.d("PUSH", "Push received by activity, broadcast aborted!");
				this.abortBroadcast();
			}
			
    	};
    	
    	IntentFilter filter = new IntentFilter(PUSH_BROADCAST);
    	filter.setPriority(10);
		
		activity.registerReceiver(receiver, filter);
		
		return receiver;
    }
    
    public static void unregisterDynamicBroadcastReceiver(Activity activity, BroadcastReceiver receiver) {
    	if (receiver != null) {
    		activity.unregisterReceiver(receiver);
    	}
    }


}