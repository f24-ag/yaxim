package org.yaxim.androidclient;

import org.jboss.aerogear.android.unifiedpush.MessageHandler;
import org.yaxim.androidclient.data.YaximConfiguration;
import org.yaxim.androidclient.util.PreferenceConstants;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class NotifyingHandler extends BroadcastReceiver implements MessageHandler {
	
	private static final String PUSH_BROADCAST = "de.f24.rooms.messages.push.broadcast";

	public NotifyingHandler() {
		super();
	}
	
	@Override
	public void onMessage(final Context context, final Bundle message) {
		
		Log.d("PUSH", "Push received by service...");
		
		final Intent broadcastIntent = new Intent(PUSH_BROADCAST);
		broadcastIntent.putExtras(message);
		context.sendOrderedBroadcast(broadcastIntent, null);
	}

    @Override
    public void onDeleteMessage(final Context context, final Bundle arg0) {
        // handle GoogleCloudMessaging.MESSAGE_TYPE_DELETED
    }

    @Override
    public void onError() {
        // handle GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR
    }
    
	@Override
	public void onReceive(final Context context, final Intent intent) {
		
        // Check if the app is already running
        final ActivityManager activityManager = (ActivityManager) context.getSystemService( Context.ACTIVITY_SERVICE );
        for ( final RunningServiceInfo service : activityManager.getRunningServices( Integer.MAX_VALUE ) ) {
            if ( service.process.equals( context.getPackageName().toString() ) && service.started ) {
                Log.d( "PUSH", "Service running and connecting, aborting push notification" );
                return;
            }
        }

        Log.d( "PUSH", "Push received by service again, showing notification!" );
		
		final YaximConfiguration mConfig = YaximApplication.getConfig(context);
		final Bundle message = intent.getExtras();
		
		final Intent mainIntent = new Intent(context, MainWindow.class);
        final PendingIntent notify = PendingIntent.getActivity(context, 99, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
			.setSmallIcon(R.drawable.icon)
			.setContentTitle(context.getString(R.string.app_name))
			.setSound(mConfig.notifySound)
			.setAutoCancel(true)
			.setOngoing(false)
			.setContentText(message.getString("alert"))
			.setTicker(message.getString("alert"))
			.setLights(Color.MAGENTA, 200, 1000)
			.setVibrate(new long[] { 1000, 200, 200, 200, 400, 1000 });
		
		builder.setContentIntent(notify);
        notificationManager.notify(99, builder.getNotification());
        
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(PreferenceConstants.CONN_STARTUP, true).commit();	
    }
    
    public static BroadcastReceiver registerDynamicBroadcastReceiver(final Activity activity) {
    	
    	final BroadcastReceiver receiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(final Context context, final Intent intent) {
				Log.d("PUSH", "Push received by activity, broadcast aborted!");
				this.abortBroadcast();
			}
			
    	};
    	
    	final IntentFilter filter = new IntentFilter(PUSH_BROADCAST);
    	filter.setPriority(10);
		
		activity.registerReceiver(receiver, filter);
		
		return receiver;
    }
    
    public static void unregisterDynamicBroadcastReceiver(final Activity activity, final BroadcastReceiver receiver) {
    	if (receiver != null) {
    		activity.unregisterReceiver(receiver);
    	}
    }


}