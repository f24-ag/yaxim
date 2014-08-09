package org.yaxim.androidclient.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.yaxim.androidclient.IXMPPRosterCallback;
import org.yaxim.androidclient.MainWindow;
import org.yaxim.androidclient.R;
import org.yaxim.androidclient.YaximApplication;
import org.yaxim.androidclient.crypto.Crypto;
import org.yaxim.androidclient.data.RosterProvider;
import org.yaxim.androidclient.data.RosterProvider.RoomsConstants;
import org.yaxim.androidclient.exceptions.YaximXMPPException;
import org.yaxim.androidclient.util.ConnectionState;
import org.yaxim.androidclient.util.PreferenceConstants;
import org.yaxim.androidclient.util.StatusMode;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;
import de.f24.rooms.crypto.EncryptionException;
import de.f24.rooms.messages.CompanyAffiliationResponse;
import de.f24.rooms.messages.ContactSearch;
import de.f24.rooms.messages.ContactSync;
import de.f24.rooms.messages.FileMessage;
import de.f24.rooms.messages.OpenRoomRequest;
import de.f24.rooms.messages.Registration;
import de.f24.rooms.messages.RegistrationRequest;
import de.f24.rooms.messages.RoomsMessageFactory;
import de.f24.rooms.messages.RoomsMessageType;
import de.f24.rooms.messages.TaskMessage;
import de.f24.rooms.messages.TaskResponse;
import de.f24.rooms.messages.WebLoginToken;

public class XMPPService extends GenericService {

	private final AtomicBoolean mConnectionDemanded = new AtomicBoolean(false); // should we try to reconnect?
	private static final int RECONNECT_AFTER = 5;
	private static final int RECONNECT_MAXIMUM = 10*60;
	private static final String RECONNECT_ALARM = "org.yaxim.androidclient.RECONNECT_ALARM";
	private int mReconnectTimeout = RECONNECT_AFTER;
	private String mReconnectInfo = "";
	private final Intent mAlarmIntent = new Intent(RECONNECT_ALARM);
	private PendingIntent mPAlarmIntent;
	private final BroadcastReceiver mAlarmReceiver = new ReconnectAlarmReceiver();

	private ServiceNotification mServiceNotification = null;

	private Smackable mSmackable;
	private boolean create_account = false;
	private IXMPPRosterService.Stub mService2RosterConnection;
	private IXMPPChatService.Stub mServiceChatConnection;

	private final RemoteCallbackList<IXMPPRosterCallback> mRosterCallbacks = new RemoteCallbackList<IXMPPRosterCallback>();
	private final HashSet<String> mIsBoundTo = new HashSet<String>();
	private final Handler mMainHandler = new Handler();

	@Override
	public IBinder onBind(final Intent intent) {
		super.onBind(intent);
		final String chatPartner = intent.getDataString();
		if ((chatPartner != null)) {
			resetNotificationCounter(chatPartner);
			mIsBoundTo.add(chatPartner);
			return mServiceChatConnection;
		}

		return mService2RosterConnection;
	}

	@Override
	public void onRebind(final Intent intent) {
		super.onRebind(intent);
		final String chatPartner = intent.getDataString();
		if ((chatPartner != null)) {
			mIsBoundTo.add(chatPartner);
			resetNotificationCounter(chatPartner);
		}
	}

	@Override
	public boolean onUnbind(final Intent intent) {
		final String chatPartner = intent.getDataString();
		if ((chatPartner != null)) {
			mIsBoundTo.remove(chatPartner);
		}
		return true;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		createServiceRosterStub();
		createServiceChatStub();

		mPAlarmIntent = PendingIntent.getBroadcast(this, 0, mAlarmIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
		registerReceiver(mAlarmReceiver, new IntentFilter(RECONNECT_ALARM));

		YaximBroadcastReceiver.initNetworkStatus(getApplicationContext());

		if (mConfig.autoConnect && mConfig.jid_configured) {
			/*
			 * start our own service so it remains in background even when
			 * unbound
			 */
			final Intent xmppServiceIntent = new Intent(this, XMPPService.class);
			startService(xmppServiceIntent);
		}

		mServiceNotification = ServiceNotification.getInstance();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		((AlarmManager)getSystemService(Context.ALARM_SERVICE)).cancel(mPAlarmIntent);
		mRosterCallbacks.kill();
		if (mSmackable != null) {
		    manualDisconnect();
		    mSmackable.unRegisterCallback();
		}
		unregisterReceiver(mAlarmReceiver);
	}

	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		logInfo("onStartCommand(), mConnectionDemanded=" + mConnectionDemanded.get());
		if (intent != null) {
			create_account = intent.getBooleanExtra("create_account", false);
			
			if ("disconnect".equals(intent.getAction())) {
				failConnection(getString(R.string.conn_no_network));
				return START_STICKY;
			} else
			if ("reconnect".equals(intent.getAction())) {
				// TODO: integrate the following steps into one "RECONNECT"
				failConnection(getString(R.string.conn_no_network));
				// reset reconnection timeout
				mReconnectTimeout = RECONNECT_AFTER;
				doConnect();
				return START_STICKY;
			} else
			if ("ping".equals(intent.getAction())) {
				if (mSmackable != null) {
					mSmackable.sendServerPing();
					return START_STICKY;
				}
				// if not yet connected, fall through to doConnect()
			}
		}
		
		mConnectionDemanded.set(mConfig.autoConnect);
		doConnect();
		return START_STICKY;
	}
	
	public void syncContacts() {
		final Crypto crypto = YaximApplication.getApp(getApplicationContext()).mCrypto;
		final Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
		final String _ID = ContactsContract.Contacts._ID;
		final String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
		final String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;

		final Uri PhoneCONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
		final String Phone_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
		final String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;
		
		final List<String> phones = new ArrayList<String>();
		final ContentResolver contentResolver = getContentResolver();
		final Cursor cursor = contentResolver.query(CONTENT_URI, null, null, null, null);	
		// Loop for every contact in the phone
		if (cursor.getCount() > 0) {
			while (cursor.moveToNext()) {
				final String contact_id = cursor.getString(cursor.getColumnIndex(_ID));
				final String name = cursor.getString(cursor.getColumnIndex(DISPLAY_NAME));
				final int hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndex(HAS_PHONE_NUMBER)));
				if (hasPhoneNumber > 0) {
					// Query and loop for every phone number of the contact
					final Cursor phoneCursor = contentResolver.query(PhoneCONTENT_URI, null, Phone_CONTACT_ID + " = ?", new String[] { contact_id }, null);
					while (phoneCursor.moveToNext()) {
						String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(NUMBER)).replaceAll("\\s+","");
						if (!phoneNumber.startsWith("+")) {
							if (phoneNumber.startsWith("0")) {
								phoneNumber = "+49" + phoneNumber.substring(1);
								phones.add(crypto.hash(phoneNumber));
							}
						}
						else {
							phones.add(crypto.hash(phoneNumber));
						}
					}
					phoneCursor.close();
				}
			}
		}
		final ContactSync sync = (ContactSync)RoomsMessageFactory.getRoomsMessage(RoomsMessageType.ContactSync);
		sync.setHashes(phones);
		mSmackable.sendControlMessage(sync);
	}

	private void createServiceChatStub() {
		mServiceChatConnection = new IXMPPChatService.Stub() {

			@Override
            public void sendMessage(final String user, final String message)
					throws RemoteException {
				if (mSmackable != null) {
                    mSmackable.sendMessage(user, message);
                } else {
                    SmackableImp.sendOfflineMessage(getContentResolver(),
							user, message, mConfig.jabberID);
                }
			}

			@Override
            public boolean isAuthenticated() throws RemoteException {
				if (mSmackable != null) {
					return mSmackable.isAuthenticated();
				}

				return false;
			}
			
			@Override
            public void clearNotifications(final String Jid) throws RemoteException {
				clearNotification(Jid);
			}

			@Override
			public String sendFile(final String jabberID, final String fileName, final long size, final String key, final String url, final String mimeType)
					throws RemoteException {
				if (mSmackable != null) {
					final FileMessage message = (FileMessage)RoomsMessageFactory.getRoomsMessage(RoomsMessageType.File);
					try {
						message.setFilename(fileName);
						message.setSize(size);
						message.setKey(key);
						message.setDownloadLink(url);
						message.setDescription(fileName);
						message.setMimeType(mimeType);
						return mSmackable.sendRoomMessage(jabberID, message);
					}
					catch (final Exception ex) {
						Log.e("JSON", ex.getMessage());
					}
				}
				return null;
			}
			
			@Override
			public String openRoom(final String parentRoomID, final String topic, final String[] participants)
					throws RemoteException {
				if (mSmackable != null) {
					return mSmackable.sendControlMessage(createOpenRoomRequest(parentRoomID, topic, participants));
				}
				return null;
			}

			@Override
			public String sendTaskResponse(final String selectedOption)
					throws RemoteException {
				if (mSmackable != null) {
					final TaskResponse message = (TaskResponse)RoomsMessageFactory.getRoomsMessage(RoomsMessageType.TaskResponse);
					message.setAnswer(selectedOption);
					return mSmackable.sendControlMessage(message);
				}
				return null;
			}

			@Override
			public String sendTask(final String roomID, final String text, final String recipient)
					throws RemoteException {
				if (mSmackable != null) {
					final TaskMessage message = (TaskMessage)RoomsMessageFactory.getRoomsMessage(RoomsMessageType.Task);
					try {
						message.setRecipients(Arrays.asList(recipient));
						message.setText(text);
						message.setOptions(Arrays.asList("Accept", "Reject", "Done"));
						return mSmackable.sendRoomMessage(roomID, message);
					}
					catch (final Exception ex) {
						Log.e("JSON", ex.getMessage());
					}
				}
				return null;
			}
		};
	}
	
	private OpenRoomRequest createOpenRoomRequest(final String parentRoomID, final String topic, final String[] participants) {
		String roomName = topic;
		if (parentRoomID != null) {
			final Cursor c = getContentResolver().query(RosterProvider.ROOMS_URI, new String[] { RoomsConstants.NAME }, 
					RoomsConstants.ID + " = ?", new String[] {parentRoomID}, null);
			if (c.moveToNext()) {
				roomName = c.getString(0) + "/" + roomName;
			}
		}
		final OpenRoomRequest request = new OpenRoomRequest();
		request.setRoomName(roomName);
		final List<String> lstParticipants = new ArrayList<String>();
		lstParticipants.addAll(Arrays.asList(participants));
		lstParticipants.add(mConfig.jabberID);
		request.setParticipants(lstParticipants);
		return request;
	}

	private void createServiceRosterStub() {
		mService2RosterConnection = new IXMPPRosterService.Stub() {

			@Override
            public void registerRosterCallback(final IXMPPRosterCallback callback)
					throws RemoteException {
				if (callback != null) {
                    mRosterCallbacks.register(callback);
                }
			}

			@Override
            public void unregisterRosterCallback(final IXMPPRosterCallback callback)
					throws RemoteException {
				if (callback != null) {
                    mRosterCallbacks.unregister(callback);
                }
			}

			@Override
            public int getConnectionState() throws RemoteException {
				if (mSmackable != null) {
					return mSmackable.getConnectionState().ordinal();
				} else {
					return ConnectionState.OFFLINE.ordinal();
				}
			}

			@Override
            public String getConnectionStateString() throws RemoteException {
				return XMPPService.this.getConnectionStateString();
			}


			@Override
            public void setStatusFromConfig()
					throws RemoteException {
				if (mSmackable != null) { // this should always be true, but stil...
					mSmackable.setStatusFromConfig();
					updateServiceNotification();
				}
			}

			@Override
            public void addRosterItem(final String user, final String alias, final String group)
					throws RemoteException {
				try {
					mSmackable.addRosterItem(user, alias, group);
				} catch (final YaximXMPPException e) {
					shortToastNotify(e);
				}
			}

			@Override
            public void addRosterGroup(final String group) throws RemoteException {
				mSmackable.addRosterGroup(group);
			}

			@Override
            public void removeRosterItem(final String user) throws RemoteException {
				try {
					mSmackable.removeRosterItem(user);
				} catch (final YaximXMPPException e) {
					shortToastNotify(e);
				}
			}

			@Override
            public void moveRosterItemToGroup(final String user, final String group)
					throws RemoteException {
				try {
					mSmackable.moveRosterItemToGroup(user, group);
				} catch (final YaximXMPPException e) {
					shortToastNotify(e);
				}
			}

			@Override
            public void renameRosterItem(final String user, final String newName)
					throws RemoteException {
				try {
					mSmackable.renameRosterItem(user, newName);
				} catch (final YaximXMPPException e) {
					shortToastNotify(e);
				}
			}

			@Override
            public void renameRosterGroup(final String group, final String newGroup)
					throws RemoteException {
				mSmackable.renameRosterGroup(group, newGroup);
			}

			@Override
            public void disconnect() throws RemoteException {
				manualDisconnect();
			}

			@Override
            public void connect() throws RemoteException {
				mConnectionDemanded.set(true);
				mReconnectTimeout = RECONNECT_AFTER;
				doConnect();
			}

			@Override
            public void sendPresenceRequest(final String jid, final String type)
					throws RemoteException {
				mSmackable.sendPresenceRequest(jid, type);
			}

			@Override
			public void openRoom(final String parentRoomID, final String topic,
					final String[] participants) throws RemoteException {
				mSmackable.sendControlMessage(createOpenRoomRequest(parentRoomID, topic, participants));
			}

			@Override
			public void sendRegistrationMessage1(final String phoneNumber)
					throws RemoteException {
				final RegistrationRequest request = new RegistrationRequest();
				request.setPhoneNumber(phoneNumber);
				mSmackable.sendControlMessage(request);
			}

			@Override
			public void sendRegistrationMessage2(final String code, final String publicKey)
					throws RemoteException {
				final Registration registration = new Registration();
				registration.setConfirmationCode(code);
				registration.setPublicKey(publicKey);
				mSmackable.sendControlMessage(registration);
			}

			@Override
			public void syncContacts() throws RemoteException {
				XMPPService.this.syncContacts();
			}

			@Override
			public void searchContact(final String name) throws RemoteException {
				final ContactSearch search = (ContactSearch)RoomsMessageFactory.getRoomsMessage(RoomsMessageType.ContactSearch);
				search.setQuery(name);
				mSmackable.sendControlMessage(search);
			}

			@Override
			public void sendWebToken(final String token) throws RemoteException {
				final Crypto crypto = YaximApplication.getApp(getApplicationContext()).mCrypto;
				final WebLoginToken tokenMessage = (WebLoginToken)RoomsMessageFactory.getRoomsMessage(RoomsMessageType.WebLoginToken);
				try {
					tokenMessage.setTokenHash(crypto.hash(token));
					tokenMessage.setPassword(crypto.encryptSymmetrically(mConfig.password, token));
					mSmackable.sendControlMessage(tokenMessage);
				}
				catch (final EncryptionException ex) {
					logError("Encryption error: " + ex.getMessage());
				}
			}

            @Override
            public void setCompanies( final String[] companyKeys ) throws RemoteException {

                final CompanyAffiliationResponse response =
                        (CompanyAffiliationResponse) RoomsMessageFactory
                                .getRoomsMessage( RoomsMessageType.CompanyAffiliationResponse );
                response.setCompanyKeys( Arrays.asList( companyKeys ) );
                mSmackable.sendControlMessage( response );
            }
		};
	}

	private String getConnectionStateString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(mReconnectInfo);
		if (mSmackable != null && mSmackable.getLastError() != null) {
			sb.append("\n");
			sb.append(mSmackable.getLastError());
		}
		return sb.toString();
	}

	public String getStatusTitle(final ConnectionState cs) {
		if (cs != ConnectionState.ONLINE) {
            return mReconnectInfo;
        }
		String status = getString(StatusMode.fromString(mConfig.statusMode).getTextId());

		if (mConfig.statusMessage.length() > 0) {
			status = status + " (" + mConfig.statusMessage + ")";
		}

		return status;
	}

	private void updateServiceNotification() {
		ConnectionState cs = ConnectionState.OFFLINE;
		if (mSmackable != null) {
			cs = mSmackable.getConnectionState();
		}

		// HACK to trigger show-offline when XEP-0198 reconnect is going on
		getContentResolver().notifyChange(RosterProvider.CONTENT_URI, null);
		getContentResolver().notifyChange(RosterProvider.GROUPS_URI, null);
		// end-of-HACK

		broadcastConnectionState(cs);

		// do not show notification if not a foreground service
		if (!mConfig.foregroundService) {
            return;
        }

		if (cs == ConnectionState.OFFLINE) {
			mServiceNotification.hideNotification(this, SERVICE_NOTIFICATION);
			return;
		}
		final Notification n = new Notification(R.drawable.ic_offline, null,
				System.currentTimeMillis());
		n.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR | Notification.FLAG_ONLY_ALERT_ONCE;

		final Intent notificationIntent = new Intent(this, MainWindow.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		n.contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		if (cs == ConnectionState.ONLINE) {
            n.icon = R.drawable.ic_online;
        }

		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);		
		final String title = getString(R.string.conn_title, sharedPreferences.getString(PreferenceConstants.NAME, ""));
		final String message = getStatusTitle(cs);
		n.setLatestEventInfo(this, title, message, n.contentIntent);

		mServiceNotification.showNotification(this, SERVICE_NOTIFICATION,
				n);
	}

	private void doConnect() {
		mReconnectInfo = getString(R.string.conn_connecting);
		updateServiceNotification();
		if (mSmackable == null) {
			createAdapter();
		}

		mSmackable.requestConnectionState(ConnectionState.ONLINE, create_account);
	}

	private void broadcastConnectionState(final ConnectionState cs) {
		final int broadCastItems = mRosterCallbacks.beginBroadcast();

		for (int i = 0; i < broadCastItems; i++) {
			try {
				mRosterCallbacks.getBroadcastItem(i).connectionStateChanged(cs.ordinal());
			} catch (final RemoteException e) {
				logError("caught RemoteException: " + e.getMessage());
			}
		}
		mRosterCallbacks.finishBroadcast();
	}

	private NetworkInfo getNetworkInfo() {
		final Context ctx = getApplicationContext();
		final ConnectivityManager connMgr =
				(ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		return connMgr.getActiveNetworkInfo();
	}
	private boolean networkConnected() {
		final NetworkInfo info = getNetworkInfo();

		return info != null && info.isConnected();
	}
	private boolean networkConnectedOrConnecting() {
		final NetworkInfo info = getNetworkInfo();

		return info != null && info.isConnectedOrConnecting();
	}

	// call this when Android tells us to shut down
	private void failConnection(final String reason) {
		logInfo("failConnection: " + reason);
		mReconnectInfo = reason;
		updateServiceNotification();
		if (mSmackable != null) {
            mSmackable.requestConnectionState(ConnectionState.DISCONNECTED);
        }
	}

	// called from Smackable when connection broke down
	private void connectionFailed(final String reason) {
		logInfo("connectionFailed: " + reason);
		//TODO: error message from downstream?
		//mLastConnectionError = reason;
		if (!networkConnected()) {
			mReconnectInfo = getString(R.string.conn_no_network);
			mSmackable.requestConnectionState(ConnectionState.RECONNECT_NETWORK);

		} else if (mConnectionDemanded.get()) {
			mReconnectInfo = getString(R.string.conn_reconnect, mReconnectTimeout);
			mSmackable.requestConnectionState(ConnectionState.RECONNECT_DELAYED);
			logInfo("connectionFailed(): registering reconnect in " + mReconnectTimeout + "s");
			((AlarmManager)getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.RTC_WAKEUP,
					System.currentTimeMillis() + mReconnectTimeout * 1000, mPAlarmIntent);
			mReconnectTimeout = mReconnectTimeout * 2;
			if (mReconnectTimeout > RECONNECT_MAXIMUM) {
                mReconnectTimeout = RECONNECT_MAXIMUM;
            }
		} else {
			connectionClosed();
		}

	}

	private void connectionClosed() {
		logInfo("connectionClosed.");
		mReconnectInfo = "";
		mServiceNotification.hideNotification(this, SERVICE_NOTIFICATION);
	}

	public void manualDisconnect() {
		mConnectionDemanded.set(false);
		mReconnectInfo = getString(R.string.conn_disconnecting);
		performDisconnect();
	}

	public void performDisconnect() {
		if (mSmackable != null) {
			// this is non-blocking
			mSmackable.requestConnectionState(ConnectionState.OFFLINE);
		}
	}

	private void createAdapter() {
		System.setProperty("smack.debugEnabled", "" + mConfig.smackdebug);
		try {
			mSmackable = new SmackableImp(mConfig, getContentResolver(), this);
		} catch (final NullPointerException e) {
			e.printStackTrace();
		}

		mSmackable.registerCallback(new XMPPServiceCallback() {
			@Override
            public void newMessage(final String from, final String roomID, final String message, final boolean silent_notification) {
				logInfo("notification: " + from);
				notifyClient(from, mSmackable.getNameForJID(from, roomID), roomID, message, !mIsBoundTo.contains(from) && !mIsBoundTo.contains(roomID), silent_notification, false);
			}

			@Override
            public void messageError(final String from, final String error, final boolean silent_notification) {
				logInfo("error notification: " + from);
				mMainHandler.post(new Runnable() {
					@Override
                    public void run() {
						// work around Toast fallback for errors
						notifyClient(from, mSmackable.getNameForJID(from, null), null, error,
							!mIsBoundTo.contains(from), silent_notification, true);
					}});
				}

			@Override
            public void rosterChanged() {
			}

			@Override
            public void connectionStateChanged() {
				// TODO: OFFLINE is sometimes caused by XMPPConnection calling
				// connectionClosed() callback on an error, need to catch that?
				switch (mSmackable.getConnectionState()) {
				//case OFFLINE:
				case DISCONNECTED:
					connectionFailed(getString(R.string.conn_disconnected));
					break;
                case ONLINE:
                    mReconnectTimeout = RECONNECT_AFTER;//$FALL-THROUGH$
				default:
					updateServiceNotification();
				}
			}
		});
	}

	private class ReconnectAlarmReceiver extends BroadcastReceiver {
		@Override
        public void onReceive(final Context ctx, final Intent i) {
			logInfo("Alarm received.");
			if (!mConnectionDemanded.get()) {
				return;
			}
			if (mSmackable != null && mSmackable.getConnectionState() == ConnectionState.ONLINE) {
				logError("Reconnect attempt aborted: we are connected again!");
				return;
			}
			doConnect();
		}
	}
}
